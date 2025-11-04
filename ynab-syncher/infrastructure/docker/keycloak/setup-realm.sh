#!/bin/bash

# YNAB Syncher - Keycloak Realm Setup Script
# 
# Sets up the ynab-syncher realm with users and roles using Keycloak Admin API
# This script runs after Keycloak starts to configure the development environment

set -euo pipefail

# Configuration
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin123}"
REALM_NAME="ynab-syncher"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_debug() {
    if [[ "${DEBUG:-false}" == "true" ]]; then
        echo -e "${BLUE}[DEBUG]${NC} $1"
    fi
}

# Function to wait for Keycloak to be ready
wait_for_keycloak() {
    log_info "Waiting for Keycloak to be ready..."
    local retries=30
    local count=0
    
    while [[ $count -lt $retries ]]; do
        if curl -s -f "$KEYCLOAK_URL/health/ready" >/dev/null 2>&1; then
            log_info "Keycloak is ready!"
            return 0
        fi
        
        count=$((count + 1))
        log_debug "Attempt $count/$retries - Keycloak not ready yet, waiting..."
        sleep 2
    done
    
    log_error "Keycloak did not become ready within $((retries * 2)) seconds"
    return 1
}

# Function to get admin access token
get_admin_token() {
    local response
    response=$(curl -s -X POST "$KEYCLOAK_URL/realms/master/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=admin-cli" \
        -d "username=$ADMIN_USER" \
        -d "password=$ADMIN_PASSWORD")
    
    echo "$response" | jq -r '.access_token'
}

# Function to check if realm exists
realm_exists() {
    local token="$1"
    local response_code
    response_code=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $token" \
        "$KEYCLOAK_URL/admin/realms/$REALM_NAME")
    
    [[ "$response_code" == "200" ]]
}

# Function to create realm
create_realm() {
    local token="$1"
    log_info "Creating realm: $REALM_NAME"
    
    local realm_config
    realm_config=$(cat <<EOF
{
  "realm": "$REALM_NAME",
  "displayName": "YNAB Syncher",
  "enabled": true,
  "sslRequired": "external",
  "registrationAllowed": false,
  "loginWithEmailAllowed": true,
  "duplicateEmailsAllowed": false,
  "resetPasswordAllowed": false,
  "editUsernameAllowed": false,
  "bruteForceProtected": false,
  "accessTokenLifespan": 300,
  "accessTokenLifespanForImplicitFlow": 900,
  "ssoSessionIdleTimeout": 1800,
  "ssoSessionMaxLifespan": 36000,
  "defaultSignatureAlgorithm": "RS256"
}
EOF
)
    
    curl -s -X POST "$KEYCLOAK_URL/admin/realms" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "$realm_config"
}

# Function to create roles
create_roles() {
    local token="$1"
    log_info "Creating realm roles..."
    
    # Admin role
    curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "ynab-syncher-admin",
            "description": "Full administrative access to YNAB Syncher"
        }'
    
    # User role
    curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d '{
            "name": "ynab-syncher-user",
            "description": "Standard user access to YNAB Syncher"
        }'
}

# Function to create client
create_client() {
    local token="$1"
    log_info "Creating OAuth2 client..."
    
    local client_config
    client_config=$(cat <<EOF
{
  "clientId": "ynab-syncher-api",
  "name": "YNAB Syncher API Client",
  "description": "Client for YNAB Syncher API access",
  "enabled": true,
  "clientAuthenticatorType": "client-secret",
  "secret": "ynab-syncher-secret-123",
  "redirectUris": [
    "http://localhost:8080/*",
    "http://localhost:3000/*"
  ],
  "webOrigins": [
    "http://localhost:8080",
    "http://localhost:3000"
  ],
  "standardFlowEnabled": true,
  "implicitFlowEnabled": false,
  "directAccessGrantsEnabled": true,
  "serviceAccountsEnabled": true,
  "publicClient": false,
  "protocol": "openid-connect",
  "fullScopeAllowed": true,
  "defaultClientScopes": [
    "web-origins",
    "acr",
    "profile",
    "roles",
    "email"
  ],
  "protocolMappers": [
    {
      "name": "realm roles",
      "protocol": "openid-connect",
      "protocolMapper": "oidc-usermodel-realm-role-mapper",
      "consentRequired": false,
      "config": {
        "multivalued": "true",
        "userinfo.token.claim": "true",
        "id.token.claim": "true",
        "access.token.claim": "true",
        "claim.name": "realm_access.roles",
        "jsonType.label": "String"
      }
    }
  ]
}
EOF
)
    
    curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/clients" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "$client_config"
}

# Function to create user
create_user() {
    local token="$1"
    local username="$2"
    local password="$3"
    local email="$4"
    local first_name="$5"
    local last_name="$6"
    local roles="$7"
    
    log_info "Creating user: $username"
    
    # Create user
    local user_config
    user_config=$(cat <<EOF
{
  "username": "$username",
  "enabled": true,
  "emailVerified": true,
  "firstName": "$first_name",
  "lastName": "$last_name",
  "email": "$email",
  "credentials": [
    {
      "type": "password",
      "value": "$password",
      "temporary": false
    }
  ]
}
EOF
)
    
    local user_response
    user_response=$(curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "$user_config")
    
    # Get user ID from Location header or search for user
    local user_id
    user_id=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users?username=$username" \
        -H "Authorization: Bearer $token" | jq -r '.[0].id')
    
    # Assign roles
    if [[ -n "$roles" && "$roles" != "null" ]]; then
        IFS=',' read -ra ROLE_ARRAY <<< "$roles"
        for role in "${ROLE_ARRAY[@]}"; do
            role=$(echo "$role" | xargs) # trim whitespace
            log_info "Assigning role '$role' to user '$username'"
            
            # Get role representation
            local role_rep
            role_rep=$(curl -s -X GET "$KEYCLOAK_URL/admin/realms/$REALM_NAME/roles/$role" \
                -H "Authorization: Bearer $token")
            
            # Assign role to user
            curl -s -X POST "$KEYCLOAK_URL/admin/realms/$REALM_NAME/users/$user_id/role-mappings/realm" \
                -H "Authorization: Bearer $token" \
                -H "Content-Type: application/json" \
                -d "[$role_rep]"
        done
    fi
}

# Main setup function
main() {
    log_info "Starting Keycloak realm setup for YNAB Syncher..."
    
    # Wait for Keycloak to be ready
    if ! wait_for_keycloak; then
        log_error "Keycloak is not available"
        exit 1
    fi
    
    # Get admin token
    log_info "Getting admin access token..."
    local admin_token
    admin_token=$(get_admin_token)
    
    if [[ -z "$admin_token" || "$admin_token" == "null" ]]; then
        log_error "Failed to get admin access token"
        exit 1
    fi
    
    # Check if realm already exists
    if realm_exists "$admin_token"; then
        log_warn "Realm '$REALM_NAME' already exists, skipping creation"
    else
        # Create realm
        create_realm "$admin_token"
        log_info "Realm '$REALM_NAME' created successfully"
    fi
    
    # Create roles
    create_roles "$admin_token"
    
    # Create client
    create_client "$admin_token"
    
    # Create users
    create_user "$admin_token" "admin" "admin123" "admin@ynabsyncher.local" "Admin" "User" "ynab-syncher-admin"
    create_user "$admin_token" "testuser" "user123" "testuser@ynabsyncher.local" "Test" "User" "ynab-syncher-user"
    create_user "$admin_token" "readonly" "readonly123" "readonly@ynabsyncher.local" "ReadOnly" "User" ""
    
    log_info "Keycloak realm setup completed successfully!"
    log_info ""
    log_info "Realm: $REALM_NAME"
    log_info "Admin Console: $KEYCLOAK_URL/admin/"
    log_info "Realm URL: $KEYCLOAK_URL/realms/$REALM_NAME"
    log_info ""
    log_info "Test Users:"
    log_info "  admin (password: admin123) - Administrator"
    log_info "  testuser (password: user123) - Standard User"
    log_info "  readonly (password: readonly123) - Read Only"
    log_info ""
    log_info "OAuth2 Client:"
    log_info "  Client ID: ynab-syncher-api"
    log_info "  Client Secret: ynab-syncher-secret-123"
    log_info ""
    log_info "Test token generation:"
    log_info "  ./api-tests/scripts/generate-dev-tokens.sh"
}

# Show usage
show_usage() {
    cat <<EOF
YNAB Syncher - Keycloak Realm Setup

Usage: $0 [OPTIONS]

OPTIONS:
    --help              Show this help message

ENVIRONMENT VARIABLES:
    KEYCLOAK_URL                 Keycloak base URL (default: http://localhost:8081)
    KEYCLOAK_ADMIN               Keycloak admin username (default: admin)
    KEYCLOAK_ADMIN_PASSWORD      Keycloak admin password (default: admin123)
    DEBUG                        Enable debug logging (default: false)

This script sets up the ynab-syncher realm with:
- OAuth2 client configuration
- Test users with different roles
- Proper JWT token claims mapping

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
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

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi