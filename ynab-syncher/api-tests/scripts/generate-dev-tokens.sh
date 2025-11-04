#!/bin/bash

# YNAB Syncher - Development Token Generation Script
# 
# Generates JWT tokens for development and testing purposes.
# Supports both Keycloak OAuth2 tokens and fallback development tokens.
#
# Usage:
#   ./generate-dev-tokens.sh                    # Interactive mode
#   ./generate-dev-tokens.sh --user admin       # Generate admin token
#   ./generate-dev-tokens.sh --user testuser    # Generate test user token
#   ./generate-dev-tokens.sh --user readonly    # Generate readonly token
#   ./generate-dev-tokens.sh --fallback         # Use fallback tokens (no Keycloak)
#   ./generate-dev-tokens.sh --json             # Output as JSON

set -euo pipefail

# Default configuration
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
REALM="${KEYCLOAK_REALM:-ynab-syncher}"
CLIENT_ID="${KEYCLOAK_CLIENT_ID:-ynab-syncher-api}"
CLIENT_SECRET="${KEYCLOAK_CLIENT_SECRET:-ynab-syncher-secret-123}"

# Default users and passwords
declare -A USERS=(
    ["admin"]="admin123"
    ["testuser"]="user123"
    ["readonly"]="readonly123"
)

declare -A USER_ROLES=(
    ["admin"]="ynab-syncher-admin"
    ["testuser"]="ynab-syncher-user"
    ["readonly"]="default-roles-ynab-syncher"
)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1" >&2
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1" >&2
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

log_debug() {
    if [[ "${DEBUG:-false}" == "true" ]]; then
        echo -e "${BLUE}[DEBUG]${NC} $1" >&2
    fi
}

# Function to check if Keycloak is available
check_keycloak_health() {
    local health_url="${KEYCLOAK_URL}/health"
    log_debug "Checking Keycloak health at: $health_url"
    
    if curl -s -f "$health_url" >/dev/null 2>&1; then
        log_debug "Keycloak health check passed"
        return 0
    else
        log_debug "Keycloak health check failed"
        return 1
    fi
}

# Function to check if realm exists
check_realm_exists() {
    local token_url="${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token"
    log_debug "Checking realm at: $token_url"
    
    # Try to get a token with dummy credentials to test if realm exists
    local response_code
    response_code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$token_url" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=${CLIENT_ID}" \
        -d "username=dummy" \
        -d "password=dummy" 2>/dev/null)
    
    # 401 means realm exists but credentials are invalid (expected)
    # 404 means realm doesn't exist
    if [[ "$response_code" == "401" || "$response_code" == "400" ]]; then
        log_debug "Realm '$REALM' exists (got $response_code)"
        return 0
    else
        log_debug "Realm '$REALM' does not exist (got $response_code)"
        return 1
    fi
}

# Function to get token from Keycloak
get_keycloak_token() {
    local username="$1"
    local password="$2"
    local token_url="${KEYCLOAK_URL}/realms/${REALM}/protocol/openid-connect/token"
    
    log_debug "Getting token for user '$username' from: $token_url"
    
    local response
    response=$(curl -s -X POST "$token_url" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=${CLIENT_ID}" \
        -d "client_secret=${CLIENT_SECRET}" \
        -d "username=${username}" \
        -d "password=${password}" \
        2>/dev/null)
    
    if [[ $? -ne 0 ]]; then
        log_debug "Curl failed for user '$username'"
        return 1
    fi
    
    local access_token
    access_token=$(echo "$response" | jq -r '.access_token // empty' 2>/dev/null)
    
    if [[ -n "$access_token" && "$access_token" != "null" ]]; then
        log_debug "Successfully retrieved token for user '$username'"
        echo "$access_token"
        return 0
    else
        log_debug "Failed to extract access_token from response for user '$username'"
        log_debug "Response: $response"
        return 1
    fi
}

# Function to generate fallback development tokens (JWT-like format)
generate_fallback_token() {
    local username="$1"
    local role="${USER_ROLES[$username]:-ynab-syncher-user}"
    local current_time=$(date +%s)
    local expiry_time=$((current_time + 3600)) # 1 hour from now
    
    # Base64 encoded header ({"alg":"none","typ":"JWT"})
    local header="eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0"
    
    # Create payload JSON
    local payload_json=$(cat <<EOF
{
  "iss": "ynab-syncher-dev",
  "sub": "$username",
  "aud": "ynab-syncher-api",
  "exp": $expiry_time,
  "iat": $current_time,
  "auth_time": $current_time,
  "jti": "dev-${username}-$(date +%s)",
  "preferred_username": "$username",
  "email": "${username}@ynabsyncher.local",
  "realm_access": {
    "roles": ["$role"]
  },
  "scope": "openid profile email",
  "typ": "Bearer"
}
EOF
)
    
    # Base64 encode payload (URL-safe, no padding)
    local payload
    payload=$(echo -n "$payload_json" | base64 -w 0 | tr -d '=' | tr '+/' '-_')
    
    # Development token (no signature for simplicity)
    echo "${header}.${payload}."
}

# Function to show token info
show_token_info() {
    local token="$1"
    local username="$2"
    
    # Try to decode JWT payload
    local payload_part
    payload_part=$(echo "$token" | cut -d'.' -f2)
    
    # Add padding if needed for base64 decoding
    local padding=$(( (4 - ${#payload_part} % 4) % 4 ))
    for ((i=0; i<padding; i++)); do
        payload_part+="="
    done
    
    # Decode and pretty-print
    local decoded_payload
    if decoded_payload=$(echo "$payload_part" | tr '_-' '/+' | base64 -d 2>/dev/null | jq . 2>/dev/null); then
        log_info "Token for user '$username':"
        echo "$decoded_payload" | jq -C . 2>/dev/null || echo "$decoded_payload"
        echo
    else
        log_info "Token for user '$username' (raw):"
        echo "$token"
        echo
    fi
}

# Function to output JSON format
output_json() {
    local tokens="$1"
    echo "$tokens" | jq .
}

# Function to output environment format
output_env() {
    local tokens="$1"
    
    echo "# YNAB Syncher Development Tokens"
    echo "# Generated on: $(date)"
    echo "# Valid for: 1 hour"
    echo ""
    
    echo "$tokens" | jq -r 'to_entries[] | "export " + (.key | ascii_upcase) + "_TOKEN=\"" + .value + "\""'
}

# Function to generate all tokens
generate_all_tokens() {
    local use_fallback="$1"
    local tokens="{}"
    
    for username in "${!USERS[@]}"; do
        local token=""
        
        if [[ "$use_fallback" == "true" ]]; then
            log_info "Generating fallback token for user: $username"
            token=$(generate_fallback_token "$username")
        else
            log_info "Getting Keycloak token for user: $username"
            local password="${USERS[$username]}"
            token=$(get_keycloak_token "$username" "$password")
            
            if [[ -z "$token" ]]; then
                log_warn "Failed to get Keycloak token for '$username', using fallback"
                token=$(generate_fallback_token "$username")
            fi
        fi
        
        if [[ -n "$token" ]]; then
            tokens=$(echo "$tokens" | jq --arg key "$username" --arg value "$token" '. + {($key): $value}')
        else
            log_error "Failed to generate token for user: $username"
        fi
    done
    
    echo "$tokens"
}

# Function to show usage
show_usage() {
    cat <<EOF
YNAB Syncher - Development Token Generation

Usage: $0 [OPTIONS]

OPTIONS:
    --user USER         Generate token for specific user (admin, testuser, readonly)
    --fallback          Use fallback tokens instead of Keycloak
    --json              Output tokens in JSON format
    --env               Output tokens as environment variables
    --info              Show token payload information
    --help              Show this help message

EXAMPLES:
    $0                           # Interactive mode - generate all tokens
    $0 --user admin              # Generate admin token only
    $0 --fallback --json         # Generate fallback tokens in JSON format
    $0 --user testuser --info    # Generate testuser token and show info

ENVIRONMENT VARIABLES:
    KEYCLOAK_URL                 Keycloak base URL (default: http://localhost:8081)
    KEYCLOAK_REALM               Keycloak realm name (default: ynab-syncher)
    KEYCLOAK_CLIENT_ID           OAuth2 client ID (default: ynab-syncher-api)
    KEYCLOAK_CLIENT_SECRET       OAuth2 client secret (default: ynab-syncher-secret-123)
    DEBUG                        Enable debug logging (default: false)

USERS:
    admin                        Full administrative access (password: admin123)
    testuser                     Standard user access (password: user123)
    readonly                     Read-only access (password: readonly123)

EOF
}

# Main function
main() {
    local specific_user=""
    local use_fallback="false"
    local output_format="default"
    local show_info="false"
    
    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            --user)
                specific_user="$2"
                shift 2
                ;;
            --fallback)
                use_fallback="true"
                shift
                ;;
            --json)
                output_format="json"
                shift
                ;;
            --env)
                output_format="env"
                shift
                ;;
            --info)
                show_info="true"
                shift
                ;;
            --help)
                show_usage
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_usage
                exit 1
                ;;
        esac
    done
    
    # Check if jq is available
    if ! command -v jq >/dev/null 2>&1; then
        log_error "jq is required but not installed. Please install jq first."
        exit 1
    fi
    
    # Check Keycloak availability if not using fallback
    if [[ "$use_fallback" != "true" ]]; then
        if ! check_keycloak_health; then
            log_warn "Keycloak is not available at $KEYCLOAK_URL"
            log_warn "Use --fallback flag to generate development tokens without Keycloak"
            
            # Ask user if they want to continue with fallback
            if [[ "$output_format" == "default" && -t 0 ]]; then
                read -p "Continue with fallback tokens? (y/N): " -n 1 -r
                echo
                if [[ $REPLY =~ ^[Yy]$ ]]; then
                    use_fallback="true"
                else
                    log_error "Exiting. Use --fallback flag to skip this check."
                    exit 1
                fi
            else
                log_error "Use --fallback flag to generate development tokens"
                exit 1
            fi
        elif ! check_realm_exists; then
            log_warn "Realm '$REALM' does not exist in Keycloak"
            log_warn "You may need to import the realm configuration first"
            
            if [[ "$output_format" == "default" && -t 0 ]]; then
                read -p "Continue with fallback tokens? (y/N): " -n 1 -r
                echo
                if [[ $REPLY =~ ^[Yy]$ ]]; then
                    use_fallback="true"
                else
                    log_error "Exiting. Import realm or use --fallback flag."
                    exit 1
                fi
            else
                log_error "Import realm configuration or use --fallback flag"
                exit 1
            fi
        fi
    fi
    
    # Generate tokens
    if [[ -n "$specific_user" ]]; then
        if [[ ! "${USERS[$specific_user]+_}" ]]; then
            log_error "Unknown user: $specific_user"
            log_error "Available users: ${!USERS[*]}"
            exit 1
        fi
        
        local token=""
        if [[ "$use_fallback" == "true" ]]; then
            token=$(generate_fallback_token "$specific_user")
        else
            local password="${USERS[$specific_user]}"
            token=$(get_keycloak_token "$specific_user" "$password")
            
            if [[ -z "$token" ]]; then
                log_warn "Failed to get Keycloak token, using fallback"
                token=$(generate_fallback_token "$specific_user")
            fi
        fi
        
        if [[ -n "$token" ]]; then
            if [[ "$show_info" == "true" ]]; then
                show_token_info "$token" "$specific_user"
            fi
            
            case "$output_format" in
                json)
                    echo "{\"$specific_user\": \"$token\"}" | jq .
                    ;;
                env)
                    echo "export $(echo "$specific_user" | tr '[:lower:]' '[:upper:]')_TOKEN=\"$token\""
                    ;;
                *)
                    echo "$token"
                    ;;
            esac
        else
            log_error "Failed to generate token for user: $specific_user"
            exit 1
        fi
    else
        # Generate all tokens
        local tokens
        tokens=$(generate_all_tokens "$use_fallback")
        
        if [[ "$show_info" == "true" ]]; then
            for username in "${!USERS[@]}"; do
                local token
                token=$(echo "$tokens" | jq -r ".${username}")
                if [[ -n "$token" && "$token" != "null" ]]; then
                    show_token_info "$token" "$username"
                fi
            done
        fi
        
        case "$output_format" in
            json)
                output_json "$tokens"
                ;;
            env)
                output_env "$tokens"
                ;;
            *)
                log_info "Generated tokens for YNAB Syncher development:"
                echo "$tokens" | jq -r 'to_entries[] | "\(.key): \(.value)"'
                echo
                log_info "Use --json or --env for different output formats"
                log_info "Use --info to see token payload details"
                ;;
        esac
    fi
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi