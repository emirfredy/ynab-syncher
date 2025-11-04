#!/bin/bash

# Docker Development Infrastructure Management Script
# Provides easy management of PostgreSQL + Keycloak infrastructure
# Application still uses H2 by default (no behavior change)

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
COMPOSE_FILE="docker-compose.yml"
NETWORK_NAME="ynab-network"
POSTGRES_CONTAINER="ynab-postgres"
KEYCLOAK_CONTAINER="keycloak"

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_docker() {
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed or not in PATH"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose is not installed"
        exit 1
    fi
}

# Get docker-compose command (legacy vs plugin)
get_compose_cmd() {
    if docker compose version &> /dev/null; then
        echo "docker compose"
    else
        echo "docker-compose"
    fi
}

wait_for_service() {
    local service=$1
    local timeout=${2:-60}
    local interval=5
    local elapsed=0
    
    log_info "Waiting for $service to be healthy..."
    
    while [ $elapsed -lt $timeout ]; do
        if docker inspect --format='{{.State.Health.Status}}' "$service" 2>/dev/null | grep -q "healthy"; then
            log_success "$service is healthy"
            return 0
        fi
        
        sleep $interval
        elapsed=$((elapsed + interval))
        echo -n "."
    done
    
    echo
    log_error "$service failed to become healthy within ${timeout}s"
    return 1
}

show_status() {
    local compose_cmd=$(get_compose_cmd)
    
    echo
    log_info "=== Docker Infrastructure Status ==="
    
    if $compose_cmd ps --format table; then
        echo
        
        # Check PostgreSQL
        if docker inspect --format='{{.State.Health.Status}}' "$POSTGRES_CONTAINER" &>/dev/null; then
            local pg_status=$(docker inspect --format='{{.State.Health.Status}}' "$POSTGRES_CONTAINER")
            log_info "PostgreSQL: $pg_status"
            
            if [ "$pg_status" = "healthy" ]; then
                log_info "PostgreSQL accessible at: localhost:5432"
                log_info "Database: ynabsyncher"
                log_info "Username: ynabsyncher"
            fi
        else
            log_warning "PostgreSQL: not running"
        fi
        
        # Check Keycloak
        if docker inspect --format='{{.State.Health.Status}}' "$KEYCLOAK_CONTAINER" &>/dev/null; then
            local kc_status=$(docker inspect --format='{{.State.Health.Status}}' "$KEYCLOAK_CONTAINER")
            log_info "Keycloak: $kc_status"
            
            if [ "$kc_status" = "healthy" ]; then
                log_info "Keycloak accessible at: http://localhost:8081"
                log_info "Admin Console: http://localhost:8081/admin"
                log_info "Admin Credentials: admin/admin123"
            fi
        else
            log_warning "Keycloak: not running"
        fi
        
        echo
        log_info "=== Application Connection Info ==="
        log_info "Default Profile (H2): mvn -pl infrastructure spring-boot:run"
        log_info "Docker Profile (PostgreSQL): SPRING_PROFILES_ACTIVE=docker mvn -pl infrastructure spring-boot:run"
    else
        log_warning "No services are running"
    fi
}

start_infrastructure() {
    local compose_cmd=$(get_compose_cmd)
    
    log_info "Starting Docker infrastructure..."
    
    # Create necessary directories
    mkdir -p infrastructure/docker/postgres/init
    mkdir -p infrastructure/docker/keycloak
    
    # Start services
    $compose_cmd up -d
    
    # Wait for PostgreSQL first
    if wait_for_service "$POSTGRES_CONTAINER" 120; then
        log_success "PostgreSQL is ready"
    else
        log_error "PostgreSQL failed to start"
        return 1
    fi
    
    # Wait for Keycloak
    if wait_for_service "$KEYCLOAK_CONTAINER" 180; then
        log_success "Keycloak is ready"
    else
        log_error "Keycloak failed to start"
        return 1
    fi
    
    log_success "All services are running and healthy!"
    show_status
}

stop_infrastructure() {
    local compose_cmd=$(get_compose_cmd)
    
    log_info "Stopping Docker infrastructure..."
    $compose_cmd down
    log_success "Infrastructure stopped"
}

restart_infrastructure() {
    stop_infrastructure
    start_infrastructure
}

clean_infrastructure() {
    local compose_cmd=$(get_compose_cmd)
    
    log_warning "This will remove all containers and volumes (data will be lost)"
    read -p "Are you sure? [y/N]: " -n 1 -r
    echo
    
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        log_info "Cleaning Docker infrastructure..."
        $compose_cmd down -v --remove-orphans
        docker system prune -f
        log_success "Infrastructure cleaned"
    else
        log_info "Clean cancelled"
    fi
}

logs_service() {
    local service=${1:-}
    local compose_cmd=$(get_compose_cmd)
    
    if [ -z "$service" ]; then
        log_info "Available services: postgres, keycloak, pgadmin"
        read -p "Enter service name: " service
    fi
    
    case $service in
        postgres|postgresql)
            $compose_cmd logs -f "$POSTGRES_CONTAINER"
            ;;
        keycloak|kc)
            $compose_cmd logs -f "$KEYCLOAK_CONTAINER"
            ;;
        pgadmin)
            $compose_cmd logs -f pgadmin
            ;;
        all)
            $compose_cmd logs -f
            ;;
        *)
            log_error "Unknown service: $service"
            log_info "Available services: postgres, keycloak, pgadmin, all"
            exit 1
            ;;
    esac
}

test_connectivity() {
    log_info "Testing infrastructure connectivity..."
    
    # Test PostgreSQL
    if docker exec "$POSTGRES_CONTAINER" pg_isready -U ynabsyncher -d ynabsyncher &>/dev/null; then
        log_success "PostgreSQL: connection OK"
    else
        log_error "PostgreSQL: connection failed"
    fi
    
    # Test Keycloak (use health endpoint instead of well-known for Phase 2)
    if curl -sf http://localhost:8081/health/ready &>/dev/null; then
        log_success "Keycloak: connection OK"
    else
        log_error "Keycloak: connection failed"
    fi
}

show_help() {
    echo "Docker Development Infrastructure Management"
    echo
    echo "Usage: $0 <command>"
    echo
    echo "Commands:"
    echo "  start      Start PostgreSQL + Keycloak infrastructure"
    echo "  stop       Stop all infrastructure services"
    echo "  restart    Restart all infrastructure services"
    echo "  status     Show current status of all services"
    echo "  logs       Show logs for a specific service"
    echo "  test       Test connectivity to all services"
    echo "  clean      Remove all containers and volumes (destructive)"
    echo "  help       Show this help message"
    echo
    echo "Examples:"
    echo "  $0 start                    # Start infrastructure"
    echo "  $0 logs postgres           # Show PostgreSQL logs"
    echo "  $0 logs keycloak          # Show Keycloak logs"
    echo "  $0 status                 # Show service status"
    echo
    echo "Application Usage:"
    echo "  mvn -pl infrastructure spring-boot:run                           # H2 (default)"
    echo "  SPRING_PROFILES_ACTIVE=docker mvn -pl infrastructure spring-boot:run  # PostgreSQL"
}

# Main script logic
main() {
    check_docker
    
    case "${1:-help}" in
        start)
            start_infrastructure
            ;;
        stop)
            stop_infrastructure
            ;;
        restart)
            restart_infrastructure
            ;;
        status)
            show_status
            ;;
        logs)
            logs_service "${2:-}"
            ;;
        test)
            test_connectivity
            ;;
        clean)
            clean_infrastructure
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "Unknown command: $1"
            echo
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@"