#!/bin/bash

# YNAB Syncher - Bruno API Test Runner
# 
# Executes complete API test suite for Phase 7 validation
# Requires infrastructure to be running and tokens to be available
#
# Usage:
#   ./run-api-tests.sh                    # Run all tests
#   ./run-api-tests.sh --auth-only        # Run only authentication tests
#   ./run-api-tests.sh --system-only      # Run only system/actuator tests
#   ./run-api-tests.sh --reconcile-only   # Run only reconciliation tests
#   ./run-api-tests.sh --env docker       # Use docker environment
#   ./run-api-tests.sh --generate-tokens  # Generate tokens before running tests

set -euo pipefail

# Default configuration
ENVIRONMENT="${BRUNO_ENV:-local}"
TEST_SCOPE="all"
GENERATE_TOKENS=false
VERBOSE=false
DRY_RUN=false

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
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
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${BLUE}[DEBUG]${NC} $1"
    fi
}

# Help function
show_help() {
    cat << EOF
YNAB Syncher - Bruno API Test Runner

Usage: $0 [OPTIONS]

OPTIONS:
    --auth-only         Run only authentication tests
    --system-only       Run only system/actuator tests  
    --reconcile-only    Run only reconciliation API tests
    --env ENV           Use environment (local|docker) [default: local]
    --generate-tokens   Generate tokens before running tests
    --verbose           Enable verbose output
    --dry-run           Show what would be executed without running
    --help              Show this help message

EXAMPLES:
    $0                                  # Run all tests with local environment
    $0 --env docker --generate-tokens  # Run all tests with docker environment and token generation
    $0 --auth-only --verbose           # Run only auth tests with verbose output
    $0 --reconcile-only --env docker   # Run only reconciliation tests with docker environment

PREREQUISITES:
    - Infrastructure running (./scripts/docker-dev.sh start for docker environment)
    - Bruno CLI installed (npm install -g @usebruno/cli)
    - Tokens available in environment (use --generate-tokens or run generate-dev-tokens.sh manually)

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --auth-only)
            TEST_SCOPE="auth"
            shift
            ;;
        --system-only)
            TEST_SCOPE="system"
            shift
            ;;
        --reconcile-only)
            TEST_SCOPE="reconciliation"
            shift
            ;;
        --env)
            ENVIRONMENT="$2"
            shift 2
            ;;
        --generate-tokens)
            GENERATE_TOKENS=true
            shift
            ;;
        --verbose)
            VERBOSE=true
            shift
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --help)
            show_help
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            show_help
            exit 1
            ;;
    esac
done

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(local|docker)$ ]]; then
    log_error "Invalid environment: $ENVIRONMENT. Must be 'local' or 'docker'"
    exit 1
fi

# Validate test scope
if [[ ! "$TEST_SCOPE" =~ ^(all|auth|system|reconciliation)$ ]]; then
    log_error "Invalid test scope: $TEST_SCOPE. Must be 'all', 'auth', 'system', or 'reconciliation'"
    exit 1
fi

log_info "Starting Bruno API Test Suite"
log_info "Environment: $ENVIRONMENT"
log_info "Test Scope: $TEST_SCOPE"
log_info "Generate Tokens: $GENERATE_TOKENS"

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if Bruno CLI is installed
    if ! command -v bruno &> /dev/null; then
        log_error "Bruno CLI not found. Install with: npm install -g @usebruno/cli"
        exit 1
    fi
    
    # Check if collections directory exists
    if [[ ! -d "api-tests/bruno/collections" ]]; then
        log_error "Bruno collections directory not found: api-tests/bruno/collections"
        exit 1
    fi
    
    # Check if environment file exists
    if [[ ! -f "api-tests/bruno/environments/${ENVIRONMENT}.bru" ]]; then
        log_error "Environment file not found: api-tests/bruno/environments/${ENVIRONMENT}.bru"
        exit 1
    fi
    
    log_debug "Prerequisites check completed successfully"
}

# Generate tokens if requested
generate_tokens() {
    if [[ "$GENERATE_TOKENS" == "true" ]]; then
        log_info "Generating authentication tokens..."
        
        if [[ ! -f "api-tests/scripts/generate-dev-tokens.sh" ]]; then
            log_error "Token generation script not found: api-tests/scripts/generate-dev-tokens.sh"
            exit 1
        fi
        
        if [[ "$DRY_RUN" == "true" ]]; then
            log_info "[DRY RUN] Would execute: ./api-tests/scripts/generate-dev-tokens.sh --json"
        else
            # Generate tokens and extract them
            log_debug "Executing token generation script..."
            TOKEN_JSON=$(./api-tests/scripts/generate-dev-tokens.sh --json 2>/dev/null || {
                log_warn "Keycloak token generation failed, trying fallback tokens..."
                ./api-tests/scripts/generate-dev-tokens.sh --fallback --json 2>/dev/null
            })
            
            if [[ -n "$TOKEN_JSON" ]]; then
                log_info "Tokens generated successfully"
                log_debug "Token JSON: $TOKEN_JSON"
            else
                log_error "Failed to generate tokens"
                exit 1
            fi
        fi
    fi
}

# Run specific test collection
run_test_collection() {
    local collection_path="$1"
    local collection_name="$2"
    
    log_info "Running $collection_name tests..."
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would execute: bruno run $collection_path --env $ENVIRONMENT"
        return 0
    fi
    
    # Run Bruno tests
    if bruno run "$collection_path" --env "$ENVIRONMENT" 2>&1; then
        log_info "✅ $collection_name tests completed successfully"
        return 0
    else
        log_error "❌ $collection_name tests failed"
        return 1
    fi
}

# Run tests based on scope
run_tests() {
    local base_path="api-tests/bruno/collections"
    local failed_collections=()
    
    case "$TEST_SCOPE" in
        "auth")
            run_test_collection "$base_path/auth" "Authentication" || failed_collections+=("Authentication")
            ;;
        "system")
            run_test_collection "$base_path/system" "System/Actuator" || failed_collections+=("System/Actuator")
            ;;
        "reconciliation")
            run_test_collection "$base_path/reconciliation" "Reconciliation API" || failed_collections+=("Reconciliation API")
            ;;
        "all")
            log_info "Running complete API test suite..."
            
            # Run authentication tests first
            run_test_collection "$base_path/auth" "Authentication" || failed_collections+=("Authentication")
            
            # Run system tests
            run_test_collection "$base_path/system" "System/Actuator" || failed_collections+=("System/Actuator")
            
            # Run reconciliation tests
            run_test_collection "$base_path/reconciliation" "Reconciliation API" || failed_collections+=("Reconciliation API")
            ;;
    esac
    
    # Report results
    if [[ ${#failed_collections[@]} -eq 0 ]]; then
        log_info "🎉 All test collections completed successfully!"
        return 0
    else
        log_error "❌ Failed test collections: ${failed_collections[*]}"
        return 1
    fi
}

# Health check for infrastructure
check_infrastructure() {
    log_info "Checking infrastructure health..."
    
    local base_url
    if [[ "$ENVIRONMENT" == "docker" ]]; then
        base_url="http://localhost:8080"
    else
        base_url="http://localhost:8080"
    fi
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "[DRY RUN] Would check: $base_url/actuator/health"
        return 0
    fi
    
    # Check application health
    if curl -s -f "$base_url/actuator/health" > /dev/null; then
        log_info "✅ Application is healthy"
    else
        log_error "❌ Application health check failed. Is the application running?"
        log_info "For docker environment, run: ./scripts/docker-dev.sh start"
        log_info "For local environment, run: mvn -pl infrastructure spring-boot:run"
        exit 1
    fi
    
    # Check Keycloak if using docker environment
    if [[ "$ENVIRONMENT" == "docker" ]]; then
        if curl -s -f "http://localhost:8081/realms/ynab-syncher/.well-known/openid_configuration" > /dev/null; then
            log_info "✅ Keycloak is healthy"
        else
            log_warn "⚠️  Keycloak health check failed. Some authentication tests may fail."
        fi
    fi
}

# Main execution
main() {
    log_info "================================"
    log_info "YNAB Syncher - API Test Suite"
    log_info "Phase 7: Complete Integration Testing"
    log_info "================================"
    
    check_prerequisites
    check_infrastructure
    generate_tokens
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "🔍 Dry run completed. All commands would execute successfully."
        exit 0
    fi
    
    if run_tests; then
        log_info "================================"
        log_info "🎉 API Test Suite Completed Successfully!"
        log_info "Phase 7 validation: ✅ PASSED"
        log_info "================================"
        exit 0
    else
        log_error "================================"
        log_error "❌ API Test Suite Failed!"
        log_error "Phase 7 validation: ❌ FAILED"
        log_error "================================"
        exit 1
    fi
}

# Execute main function
main "$@"