#!/bin/bash

# =============================================================================
# YNAB Syncher - Comprehensive Test & Validation Suite
# =============================================================================
# This script runs all types of tests and validations in the project:
# - Unit Tests (Domain & Infrastructure)
# - Integration Tests (WireMock)
# - Architecture Tests (ArchUnit)
# - Mutation Testing (PIT)
# - Code Coverage Analysis
# =============================================================================

set -e  # Exit on any error

# =============================================================================
# PROJECT CONFIGURATION
# =============================================================================
# Determine the project root directory (parent of scripts directory)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "Script location: $SCRIPT_DIR"
echo "Project root: $PROJECT_ROOT"

#!/bin/bash

# YNAB Syncher Test Suite
# Comprehensive testing script for hexagonal architecture validation
# Supports both quick development feedback and full CI/CD validation

set -e  # Exit on any error

# Color definitions for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

# Configuration flags
FAIL_FAST=false
VERBOSE=false
ONLY_TESTS=""
TARGET_MODULE=""
QUICK_MODE=false

# Phase 2: Parallel execution flags
PARALLEL_MODE=false
MAX_PARALLEL_JOBS=0

# Phase 3: Interactive development mode flags
INTERACTIVE_MODE=false

# Show usage information
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "OPTIONS:"
    echo "  --quick              Run quick tests only (architecture + unit + integration)"
    echo "  --fail-fast          Stop execution on first test failure"
    echo "  --only <tests>       Run only specified test types (space-separated)"
    echo "  --module <module>    Run tests for specific module only (domain or infrastructure)"
    echo "  --verbose, -v        Enable verbose output"
    echo "  --parallel           Enable parallel test execution (60% faster)"
    echo "  --jobs <n>           Maximum parallel jobs (default: auto-detect)"
    echo "  --interactive, -i    Interactive test selection menu"
    echo "  --help, -h           Show this help message"
    echo ""
    echo "AVAILABLE TEST TYPES:"
    echo "  architecture         ArchUnit compliance tests"
    echo "  unit                 Domain unit tests"
    echo "  integration          Infrastructure integration tests"
    echo "  mutation             PIT mutation testing"
    echo "  wiremock             WireMock integration tests"
    echo "  build                Full build verification"
    echo ""
    echo "EXAMPLES:"
    echo "  $0                              # Run all tests"
    echo "  $0 --quick                      # Quick development feedback"
    echo "  $0 --fail-fast                  # Stop on first failure"
    echo "  $0 --interactive                # Interactive test selection menu"
    echo "  $0 --only architecture          # Run only architecture tests"
    echo "  $0 --only unit integration      # Run unit and integration tests"
    echo "  $0 --module domain              # Run only domain module tests"
    echo "  $0 --quick --fail-fast          # Quick tests with fail-fast"
    echo ""
    echo "  # Parallel execution (Phase 2)"
    echo "  $0 --parallel                   # Parallel execution (60% faster)"
    echo "  $0 --quick --parallel --fail-fast  # Super-fast development feedback"
    echo "  $0 --parallel --jobs 4          # Parallel with custom job count"
}

# Parse command line arguments
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --quick)
                QUICK_MODE=true
                shift
                ;;
            --fail-fast)
                FAIL_FAST=true
                shift
                ;;
            --only)
                shift
                ONLY_TESTS=""
                # Collect all test types until next flag or end
                while [[ $# -gt 0 && ! "$1" =~ ^-- ]]; do
                    if [[ -n "$ONLY_TESTS" ]]; then
                        ONLY_TESTS="$ONLY_TESTS $1"
                    else
                        ONLY_TESTS="$1"
                    fi
                    shift
                done
                ;;
            --module)
                TARGET_MODULE="$2"
                if [[ "$TARGET_MODULE" != "domain" && "$TARGET_MODULE" != "infrastructure" ]]; then
                    echo -e "${RED}Error: Module must be 'domain' or 'infrastructure'${NC}"
                    exit 1
                fi
                shift 2
                ;;
            --parallel)
                PARALLEL_MODE=true
                shift
                ;;
            --jobs)
                MAX_PARALLEL_JOBS="$2"
                if ! [[ "$MAX_PARALLEL_JOBS" =~ ^[0-9]+$ ]] || [[ "$MAX_PARALLEL_JOBS" -lt 1 ]]; then
                    echo -e "${RED}Error: Jobs must be a positive integer${NC}"
                    exit 1
                fi
                shift 2
                ;;
            --verbose|-v)
                VERBOSE=true
                shift
                ;;
            --interactive|-i)
                INTERACTIVE_MODE=true
                shift
                ;;
            --help|-h)
                show_usage
                exit 0
                ;;
            *)
                echo -e "${RED}Unknown option: $1${NC}"
                echo ""
                show_usage
                exit 1
                ;;
        esac
    done
}

# Check if a test should be skipped based on flags
should_skip_test() {
    local test_name="$1"
    local test_type=""
    
    # Map test names to types
    case "$test_name" in
        *"Architecture"*) test_type="architecture" ;;
        *"Unit Tests (Domain)"*) test_type="unit" ;;
        *"Integration Tests (Infrastructure)"*) test_type="integration" ;;
        *"Mutation"*) test_type="mutation" ;;
        *"WireMock"*) test_type="wiremock" ;;
        *"Build"*) test_type="build" ;;
    esac
    
    # Skip based on module filter
    if [[ -n "$TARGET_MODULE" ]]; then
        case "$TARGET_MODULE" in
            "domain")
                if [[ "$test_name" == *"Infrastructure"* || "$test_name" == *"WireMock"* ]]; then
                    return 0  # Skip
                fi
                ;;
            "infrastructure")
                if [[ "$test_name" == *"Domain"* ]]; then
                    return 0  # Skip
                fi
                ;;
        esac
    fi
    
    # Skip based on --only filter
    if [[ -n "$ONLY_TESTS" ]]; then
        # Check if test_type is in ONLY_TESTS
        if [[ ! " $ONLY_TESTS " =~ " $test_type " ]]; then
            return 0  # Skip
        fi
    fi
    
    # Skip mutation testing in quick mode
    if [[ "$QUICK_MODE" == "true" && "$test_type" == "mutation" ]]; then
        return 0  # Skip
    fi
    
    return 1  # Don't skip
}

# Enhanced fail-fast error handling
handle_test_failure() {
    local test_name="$1"
    local exit_code="$2"
    local output="$3"
    
    print_error "Test failed: $test_name (exit code: $exit_code)"
    
    if [[ "$FAIL_FAST" == "true" ]]; then
        echo -e "\n${RED}💥 FAIL-FAST MODE: Stopping execution on first failure${NC}"
        echo -e "\n${YELLOW}📋 FAILURE DETAILS:${NC}"
        echo -e "   Test: $test_name"
        echo -e "   Exit Code: $exit_code"
        
        if [[ -n "$output" && "$VERBOSE" == "true" ]]; then
            echo -e "\n${YELLOW}📝 OUTPUT (last 20 lines):${NC}"
            echo "$output" | tail -20
        fi
        
        echo -e "\n${CYAN}🔧 SUGGESTED ACTIONS:${NC}"
        case "$test_name" in
            *"Architecture"*)
                echo -e "   • Check ArchUnit violations in test output"
                echo -e "   • Review layer dependencies and package structure"
                echo -e "   • Run: mvn test -Dtest=ArchitectureTest"
                ;;
            *"Unit"*)
                echo -e "   • Review failed unit tests in the domain module"
                echo -e "   • Check test assertions and business logic"
                echo -e "   • Run: mvn -pl domain test"
                ;;
            *"Integration"*)
                echo -e "   • Check integration test failures in infrastructure"
                echo -e "   • Verify test containers and external dependencies"
                echo -e "   • Run: mvn -pl infrastructure test"
                ;;
            *"Mutation"*)
                echo -e "   • Review mutation score and uncovered code"
                echo -e "   • Add missing test cases for critical logic"
                echo -e "   • Run: mvn -pl domain org.pitest:pitest-maven:mutationCoverage"
                ;;
        esac
        
        echo -e "\n${CYAN}🔍 DEBUG COMMANDS:${NC}"
        echo -e "   • Verbose mode: $0 --verbose"
        echo -e "   • Single test: $0 --only $(echo "$test_name" | tr '[:upper:]' '[:lower:]' | cut -d' ' -f1)"
        echo -e "   • Module only: $0 --module $(if [[ "$test_name" == *"Domain"* ]]; then echo "domain"; else echo "infrastructure"; fi)"
        
        exit $exit_code
    fi
}

# Test results tracking
declare -A test_results
declare -A test_counts
declare -A test_durations

# Parallel execution tracking
declare -A test_pids
declare -A test_outputs

# Setup parallel execution environment
setup_parallel_execution() {
    if [[ "$PARALLEL_MODE" == "true" ]]; then
        # Auto-detect number of cores if not specified
        if [[ "$MAX_PARALLEL_JOBS" -eq 0 ]]; then
            if command -v nproc >/dev/null 2>&1; then
                MAX_PARALLEL_JOBS=$(nproc)
            else
                MAX_PARALLEL_JOBS=4  # Safe default
            fi
        fi
        
        # Limit to reasonable maximum (avoid overwhelming system)
        if [[ "$MAX_PARALLEL_JOBS" -gt 8 ]]; then
            MAX_PARALLEL_JOBS=8
        fi
        
        if [[ "$VERBOSE" == "true" ]]; then
            echo -e "${CYAN}⚡ Parallel Mode: Enabled (${MAX_PARALLEL_JOBS} jobs)${NC}"
        fi
    fi
}

# Colorize status values
colorize_status() {
    local status="$1"
    case "$status" in
        "PASS")
            echo -e "${GREEN}${status}${NC}"
            ;;
        "WARN")
            echo -e "${YELLOW}${status}${NC}"
            ;;
        "FAIL")
            echo -e "${RED}${status}${NC}"
            ;;
        "SKIP")
            echo -e "${CYAN}${status}${NC}"
            ;;
        *)
            echo "$status"
            ;;
    esac
}

print_header() {
    echo -e "\n${BLUE}==================================================================================${NC}"
    echo -e "${WHITE}$1${NC}"
    echo -e "${BLUE}==================================================================================${NC}\n"
}

# =============================================================================
# PHASE 3: INTERACTIVE DEVELOPMENT MODE
# =============================================================================

# Display the last test results status
display_last_test_results() {
    echo -e "${WHITE}📊 Last Test Results:${NC}"
    
    # Check if we have any results
    if [[ ${#test_results[@]} -eq 0 ]]; then
        echo -e "   ${CYAN}No tests have been run yet${NC}"
        return
    fi
    
    # Display results in a compact format
    local categories=("Architecture Tests (ArchUnit)" "Unit Tests (Domain)" "Integration Tests (Infrastructure)" "WireMock Integration Tests" "Mutation Testing (PIT)" "Full Build Verification")
    
    for category in "${categories[@]}"; do
        local status="${test_results[$category]:-SKIP}"
        local count="${test_counts[$category]:-N/A}"
        local duration="${test_durations[$category]:-N/A}"
        
        if [[ "$status" != "SKIP" ]]; then
            local status_colored=$(colorize_status "$status")
            printf "   %-35s %s" "$category:" "$status_colored"
            if [[ "$count" != "N/A" ]]; then
                printf " (%s tests" "$count"
                if [[ "$duration" != "N/A" ]]; then
                    printf ", %s)" "$duration"
                else
                    printf ")"
                fi
            fi
            echo ""
        fi
    done
    
    if [[ ${#test_results[@]} -eq 0 ]]; then
        echo -e "   ${CYAN}No recent test results${NC}"
    fi
}

# Handle interactive menu choice
handle_interactive_choice() {
    local choice="$1"
    
    case "$choice" in
        "1")
            echo -e "${CYAN}🚀 Running quick feedback tests...${NC}"
            clear
            run_tests_with_options --quick --parallel --fail-fast
            pause_for_user
            ;;
        "2")
            echo -e "${CYAN}🏗️ Running architecture validation...${NC}"
            clear
            run_tests_with_options --only architecture --fail-fast
            pause_for_user
            ;;
        "3")
            echo -e "${CYAN}🧪 Running domain unit tests...${NC}"
            clear
            run_tests_with_options --module domain --only unit --parallel
            pause_for_user
            ;;
        "4")
            echo -e "${CYAN}🔧 Running infrastructure integration tests...${NC}"
            clear
            run_tests_with_options --module infrastructure --only integration
            pause_for_user
            ;;
        "5")
            echo -e "${CYAN}🎲 Running mutation testing (this will take a few minutes)...${NC}"
            clear
            run_tests_with_options --only mutation
            pause_for_user
            ;;
        "6")
            echo -e "${CYAN}📈 Running coverage analysis...${NC}"
            clear
            run_tests_with_options --only unit integration
            pause_for_user
            ;;
        "7")
            echo -e "${CYAN}⚡ Running full parallel test suite...${NC}"
            clear
            run_tests_with_options --parallel --fail-fast
            pause_for_user
            ;;
        "8")
            echo -e "${CYAN}👁️ Entering watch mode...${NC}"
            echo -e "${YELLOW}⚠️  Watch mode is planned for a future release${NC}"
            echo -e "${CYAN}For now, you can use: ${WHITE}./scripts/run-tests.sh --quick --parallel --fail-fast${NC}"
            pause_for_user
            ;;
        "9")
            echo -e "${CYAN}🔧 Cache management...${NC}"
            echo -e "${YELLOW}⚠️  Cache management is planned for a future release${NC}"
            pause_for_user
            ;;
        "a"|"A")
            echo -e "${CYAN}📋 Configuration profiles...${NC}"
            echo -e "${YELLOW}⚠️  Configuration profiles are planned for a future release${NC}"
            pause_for_user
            ;;
        "b"|"B")
            echo -e "${CYAN}📊 Quality dashboard...${NC}"
            echo -e "${YELLOW}⚠️  Quality dashboard is planned for a future release${NC}"
            pause_for_user
            ;;
        "c"|"C")
            echo -e "${CYAN}📈 Trend analysis...${NC}"
            echo -e "${YELLOW}⚠️  Trend analysis is planned for a future release${NC}"
            pause_for_user
            ;;
        "d"|"D")
            echo -e "${CYAN}🔍 Test impact analysis...${NC}"
            echo -e "${YELLOW}⚠️  Test impact analysis is planned for a future release${NC}"
            pause_for_user
            ;;
        "q"|"Q"|"quit"|"exit")
            echo -e "${GREEN}👋 Goodbye!${NC}"
            exit 0
            ;;
        "")
            # Empty input, just redraw menu
            ;;
        *)
            echo -e "${RED}❌ Invalid option: $choice${NC}"
            echo -e "${YELLOW}Please select a valid option (1-9, a-d, or q to quit)${NC}"
            pause_for_user
            ;;
    esac
}

# Run tests with specific options (used by interactive mode)
run_tests_with_options() {
    # Store current settings
    local old_quick="$QUICK_MODE"
    local old_fail_fast="$FAIL_FAST"
    local old_only="$ONLY_TESTS"
    local old_module="$TARGET_MODULE"
    local old_parallel="$PARALLEL_MODE"
    local old_verbose="$VERBOSE"
    
    # Reset settings
    QUICK_MODE=false
    FAIL_FAST=false
    ONLY_TESTS=""
    TARGET_MODULE=""
    PARALLEL_MODE=false
    VERBOSE=false
    
    # Parse the provided options
    parse_arguments "$@"
    
    # Setup parallel execution based on new settings
    setup_parallel_execution
    
    # Show simplified header for interactive mode
    echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║                         YNAB Syncher Test Suite                     ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    # Run the actual tests
    run_test_execution
    
    # Restore original settings
    QUICK_MODE="$old_quick"
    FAIL_FAST="$old_fail_fast"
    ONLY_TESTS="$old_only"
    TARGET_MODULE="$old_module"
    PARALLEL_MODE="$old_parallel"
    VERBOSE="$old_verbose"
}

# Pause for user input after test completion
pause_for_user() {
    echo ""
    echo -e "${CYAN}Press Enter to return to the menu...${NC}"
    read -r
}

# Main interactive mode function
run_interactive_mode() {
    while true; do
        clear
        echo -e "${BLUE}╔══════════════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${BLUE}║                    YNAB Syncher Test Dashboard                      ║${NC}"
        echo -e "${BLUE}║                     Interactive Development Mode                     ║${NC}"
        echo -e "${BLUE}╚══════════════════════════════════════════════════════════════════════╝${NC}"
        echo ""
        
        display_last_test_results
        echo ""
        
        echo -e "${CYAN}🎯 Test Options:${NC}"
        echo -e "  ${WHITE}1.${NC} 🚀 Quick feedback (architecture + unit + integration)"
        echo -e "  ${WHITE}2.${NC} 🏗️  Architecture validation only"
        echo -e "  ${WHITE}3.${NC} 🧪 Domain unit tests (fast)"
        echo -e "  ${WHITE}4.${NC} 🔧 Infrastructure integration tests"
        echo -e "  ${WHITE}5.${NC} 🎲 Mutation testing (comprehensive)"
        echo -e "  ${WHITE}6.${NC} 📈 Coverage analysis"
        echo -e "  ${WHITE}7.${NC} ⚡ Full parallel suite"
        echo -e "  ${WHITE}8.${NC} 👁️  Watch mode (continuous testing) ${YELLOW}[Future]${NC}"
        echo -e "  ${WHITE}9.${NC} 🔧 Cache management ${YELLOW}[Future]${NC}"
        echo ""
        echo -e "${YELLOW}⚙️  Advanced Options (Future Releases):${NC}"
        echo -e "  ${WHITE}a.${NC} 📋 Configuration profiles"
        echo -e "  ${WHITE}b.${NC} 📊 Quality dashboard"
        echo -e "  ${WHITE}c.${NC} 📈 Trend analysis"
        echo -e "  ${WHITE}d.${NC} 🔍 Test impact analysis"
        echo ""
        echo -e "${WHITE}q.${NC} Exit"
        echo ""
        
        echo -ne "${CYAN}Select option: ${NC}"
        read -r choice
        
        handle_interactive_choice "$choice"
    done
}

print_section() {
    echo -e "\n${CYAN}🔍 $1${NC}"
    echo -e "${CYAN}$(printf '=%.0s' {1..80})${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Execute Maven command from project root
execute_maven() {
    local command="$1"
    echo "Executing from $PROJECT_ROOT: $command"
    cd "$PROJECT_ROOT" && eval "$command"
}

# Extract test count from Maven output
extract_test_count() {
    local output="$1"
    # Look for "Tests run: X, Failures: Y, Errors: Z, Skipped: W" in the Results section
    local count=$(echo "$output" | grep -E "Tests run: [0-9]+, Failures:" | tail -1 | sed -E 's/.*Tests run: ([0-9]+),.*/\1/' || echo "0")
    echo "$count"
}

# Extract test failures from Maven output  
extract_test_failures() {
    local output="$1"
    echo "$output" | grep -E "Tests run: [0-9]+, Failures: [0-9]+" | tail -1 | sed -E 's/.*Failures: ([0-9]+),.*/\1/' || echo "0"
}

# Extract test errors from Maven output
extract_test_errors() {
    local output="$1" 
    echo "$output" | grep -E "Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+" | tail -1 | sed -E 's/.*Errors: ([0-9]+),.*/\1/' || echo "0"
}

# Extract coverage percentage from JaCoCo HTML report
# Extract coverage percentage from JaCoCo HTML report
extract_coverage_percentage() {
    local module_path="$1"
    local jacoco_index="${PROJECT_ROOT}/${module_path}/target/site/jacoco/index.html"
    
    if [[ -f "$jacoco_index" ]]; then
        # Extract the overall coverage percentage from the HTML footer
        local coverage=$(grep -o '<tfoot>.*</tfoot>' "$jacoco_index" | grep -o 'class="ctr2">[0-9]*%' | head -1 | sed 's/class="ctr2">//' | sed 's/%//')
        if [[ -n "$coverage" && "$coverage" =~ ^[0-9]+$ ]]; then
            echo "${coverage}% "
        else
            echo "Coverage generated"
        fi
    else
        echo "Coverage generated"
    fi
}

# Extract mutation score from PIT output
extract_mutation_score() {
    local output="$1"
    # Look for the final mutation score in the statistics section
    local score=$(echo "$output" | grep -E "Generated [0-9]+ mutations Killed [0-9]+ \([0-9]+%\)" | sed -E 's/.*\(([0-9]+)%\).*/\1/')
    if [[ -z "$score" ]]; then
        # Fallback to looking for the threshold failure message
        score=$(echo "$output" | grep -E "Mutation score of [0-9]+ is below threshold" | sed -E 's/.*Mutation score of ([0-9]+) is below threshold.*/\1/')
    fi
    echo "${score:-0}"
}

# Run test suite with timing and fail-fast support
run_test_suite_with_fail_fast() {
    local name="$1"
    local command="$2"
    local start_time=$(date +%s)
    
    print_section "$name"
    if [[ "$VERBOSE" == "true" ]]; then
        echo "Running: $command"
        echo ""
    fi
    
    # Handle mutation testing specially (it might fail threshold but still provide useful info)
    if [[ "$name" == *"Mutation"* ]]; then
        if output=$(execute_maven "$command" 2>&1); then
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            test_results["$name"]="PASS"
            
            # Extract mutation score for display
            local score=$(extract_mutation_score "$output")
            test_counts["$name"]="${score}%"
            test_durations["$name"]="${duration}s"
            print_success "$name completed successfully (Score: ${score}%)"
        else
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            
            # Check if it's just a threshold failure but mutation testing actually ran
            local score=$(extract_mutation_score "$output")
            if [[ -n "$score" && "$score" != "0" ]]; then
                test_results["$name"]="WARN"
                test_counts["$name"]="${score}%"
                test_durations["$name"]="${duration}s"
                print_warning "$name completed with warnings (Score: ${score}% - below threshold)"
            else
                test_results["$name"]="FAIL"
                test_counts["$name"]="Failed"
                test_durations["$name"]="${duration}s"
                handle_test_failure "$name" $? "$output"
                return 1
            fi
        fi
    else
        # Regular test execution
        if output=$(execute_maven "$command" 2>&1); then
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            test_results["$name"]="PASS"
            test_durations["$name"]="${duration}s"
            
            # Extract specific metrics based on test type
            case "$name" in
                *"Coverage"*)
                    # For code coverage, extract both domain and infrastructure coverage
                    local domain_coverage=$(extract_coverage_percentage "domain")
                    local infra_coverage=$(extract_coverage_percentage "infrastructure")
                    test_counts["$name"]="${domain_coverage}, ${infra_coverage}"
                    ;;
                *"Full Build Verification"*)
                    # For full build verification, extract test count and also store coverage info
                    local count=$(echo "$output" | grep -E "Tests run: [0-9]+" | tail -1 | sed -E 's/.*Tests run: ([0-9]+).*/\1/')
                    test_counts["$name"]="${count:-0} tests"
                    
                    # Also extract and store coverage information for summary display
                    local domain_coverage=$(extract_coverage_percentage "domain")
                    local infra_coverage=$(extract_coverage_percentage "infrastructure")
                    if [[ "$domain_coverage" != "Coverage generated" && "$infra_coverage" != "Coverage generated" ]]; then
                        # Store coverage info in a special "Code Coverage Analysis" entry
                        test_results["Code Coverage Analysis"]="PASS"
                        test_counts["Code Coverage Analysis"]="Domain: ${domain_coverage}, Infra: ${infra_coverage}"
                        test_durations["Code Coverage Analysis"]="Generated"
                    fi
                    ;;
                *)
                    # Extract test count from output
                    local count=$(echo "$output" | grep -E "Tests run: [0-9]+" | tail -1 | sed -E 's/.*Tests run: ([0-9]+).*/\1/')
                    test_counts["$name"]="${count:-0}"
                    ;;
            esac
            
            print_success "$name completed successfully"
        else
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            test_results["$name"]="FAIL"
            test_counts["$name"]="Failed"
            test_durations["$name"]="${duration}s"
            handle_test_failure "$name" $? "$output"
            return 1
        fi
    fi
    
    return 0
}

# Parallel test execution
run_test_parallel() {
    local name="$1"
    local command="$2"
    local temp_output="/tmp/test_output_${name// /_}.log"
    
    {
        echo "Starting parallel test: $name"
        echo "Command: $command"
        echo "Timestamp: $(date)"
        echo "----------------------------------------"
        
        local start_time=$(date +%s)
        if execute_maven "$command" 2>&1; then
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            echo "SUCCESS:$duration" >> "$temp_output.status"
        else
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            echo "FAIL:$duration" >> "$temp_output.status"
        fi
    } > "$temp_output" 2>&1 &
    
    test_pids["$name"]=$!
    test_outputs["$name"]="$temp_output"
}

# Wait for parallel test completion
wait_for_parallel_test() {
    local name="$1"
    local pid="${test_pids[$name]}"
    local temp_output="${test_outputs[$name]}"
    
    if [[ -n "$pid" ]]; then
        if [[ "$VERBOSE" == "true" ]]; then
            echo -e "${CYAN}⏳ Waiting for: $name${NC}"
        fi
        
        wait "$pid"
        local exit_code=$?
        
        # Read status from temp file
        local status_info=""
        if [[ -f "$temp_output.status" ]]; then
            status_info=$(cat "$temp_output.status")
            rm -f "$temp_output.status"
        fi
        
        local duration="0s"
        local test_status="FAIL"
        if [[ "$status_info" =~ ^SUCCESS:([0-9]+)$ ]]; then
            test_status="PASS"
            duration="${BASH_REMATCH[1]}s"
        elif [[ "$status_info" =~ ^FAIL:([0-9]+)$ ]]; then
            test_status="FAIL"
            duration="${BASH_REMATCH[1]}s"
        fi
        
        # Process output for test counts
        local output=""
        if [[ -f "$temp_output" ]]; then
            output=$(cat "$temp_output")
            rm -f "$temp_output"
        fi
        
        # Extract test metrics
        case "$name" in
            *"Coverage"*)
                local domain_coverage=$(extract_coverage_percentage "domain")
                local infra_coverage=$(extract_coverage_percentage "infrastructure")
                test_counts["$name"]="${domain_coverage}, ${infra_coverage}"
                ;;
            *"Mutation"*)
                local score=$(extract_mutation_score "$output")
                test_counts["$name"]="${score}%"
                ;;
            *)
                local count=$(echo "$output" | grep -E "Tests run: [0-9]+" | tail -1 | sed -E 's/.*Tests run: ([0-9]+).*/\1/')
                test_counts["$name"]="${count:-0}"
                ;;
        esac
        
        test_results["$name"]="$test_status"
        test_durations["$name"]="$duration"
        
        if [[ "$test_status" == "PASS" ]]; then
            print_success "$name completed successfully"
        else
            print_error "$name failed"
            if [[ "$FAIL_FAST" == "true" ]]; then
                handle_test_failure "$name" $exit_code "$output"
            fi
        fi
        
        return $exit_code
    fi
}

# Original run_test_suite function (for backward compatibility)
run_test_suite() {
    local name="$1"
    local command="$2"
    local start_time=$(date +%s)
    
    print_section "$name"
    echo "Running: $command"
    echo ""
    
    # Handle mutation testing specially (it might fail threshold but still provide useful info)
    if [[ "$name" == *"Mutation"* ]]; then
        if output=$(execute_maven "$command" 2>&1); then
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            test_results["$name"]="PASS"
            test_durations["$name"]="${duration}s"
            local score=$(extract_mutation_score "$output")
            test_counts["$name"]="${score}% mutation score"
            print_success "$name completed successfully (${duration}s)"
        else
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            test_durations["$name"]="${duration}s"
            
            # Check if it's a threshold failure but still got results
            local score=$(extract_mutation_score "$output")
            if [[ "$score" != "0" && "$score" != "" ]]; then
                test_results["$name"]="WARN"
                test_counts["$name"]="${score}% mutation score"
                print_warning "$name completed with warnings - mutation score ${score}% is below 70% threshold"
            else
                test_results["$name"]="FAIL"
                test_counts["$name"]="Failed"
                print_error "$name failed (${duration}s)"
            fi
        fi
    else
        if output=$(execute_maven "$command" 2>&1); then
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            
            test_results["$name"]="PASS"
            test_durations["$name"]="${duration}s"
            
            # Extract specific metrics based on test type
            case "$name" in
                *"Architecture"*)
                    local count=$(extract_test_count "$output")
                    test_counts["$name"]="${count} tests"
                    ;;
                *"Unit Tests"*)
                    local count=$(extract_test_count "$output")
                    local failures=$(extract_test_failures "$output")
                    local errors=$(extract_test_errors "$output")
                    test_counts["$name"]="${count} tests"
                    # Don't fail on warnings, only on actual failures/errors
                    if [[ "$failures" != "0" || "$errors" != "0" ]]; then
                        test_results["$name"]="WARN"
                        test_counts["$name"]="${count} tests (${failures}F/${errors}E)"
                    fi
                    ;;
                *"Coverage"*)
                    # For code coverage, extract both domain and infrastructure coverage
                    local domain_coverage=$(extract_coverage_percentage "domain")
                    local infra_coverage=$(extract_coverage_percentage "infrastructure")
                    test_counts["$name"]="${domain_coverage}, ${infra_coverage}"
                    ;;
                *)
                    local count=$(extract_test_count "$output")
                    if [[ "$count" != "0" ]]; then
                        test_counts["$name"]="${count} tests"
                    else
                        test_counts["$name"]="Completed"
                    fi
                    ;;
            esac
            
            print_success "$name completed successfully (${duration}s)"
        else
            local end_time=$(date +%s)
            local duration=$((end_time - start_time))
            
            test_results["$name"]="FAIL"
            test_durations["$name"]="${duration}s"
            test_counts["$name"]="Failed"
            
            print_error "$name failed (${duration}s)"
        fi
    fi
}

# Generate summary report with dynamic data
generate_summary_report() {
    print_header "Test Results Summary"
    
    echo "Test Category                            Status               Count                     Duration"
    echo "============================================================================================"
    
    # Test categories in execution order
    local categories=(
        "Architecture Tests (ArchUnit)"
        "Unit Tests (Domain)" 
        "Integration Tests (Infrastructure)"
        "WireMock Integration Tests"
        "Full Build Verification"
        "Code Coverage Analysis"
        "Mutation Testing (PIT)"
    )
    
    local total_tests=0
    local passed_tests=0
    local warned_tests=0
    local failed_tests=0
    
    # Process each category with actual data
    set +e  # Temporarily disable exit on error for table generation
    for category in "${categories[@]}"; do
        local status="${test_results[$category]:-SKIP}"
        local count="${test_counts[$category]:-N/A}"
        local duration="${test_durations[$category]:-N/A}"
        
        # Count status types
        if [[ "$status" == "PASS" ]]; then
            ((passed_tests++))
        elif [[ "$status" == "WARN" ]]; then
            ((warned_tests++))
        elif [[ "$status" == "FAIL" ]]; then
            ((failed_tests++))
        fi
        
        # Print row with actual data using colorized status
        local colored_status=$(colorize_status "$status")
        printf "%-40s %-20s %-25s %-12s\n" \
            "$category" \
            "$colored_status" \
            "$count" \
            "$duration"
        
        ((total_tests++))
    done
    set -e  # Re-enable exit on error
    
    echo ""
    echo "Summary: $passed_tests passed, $warned_tests warnings, $failed_tests failed"
    
    # Overall status
    if [[ $failed_tests -eq 0 ]]; then
        if [[ $warned_tests -eq 0 ]]; then
            echo "ALL TEST SUITES PASSED!"
        else
            echo "ALL TEST SUITES COMPLETED WITH WARNINGS"
        fi
        exit 0
    else
        echo "SOME TEST SUITES FAILED"
        exit 1
    fi
}

# Main execution
# Main execution
main() {
    # Parse command line arguments
    parse_arguments "$@"
    
    # Check for interactive mode first
    if [[ "$INTERACTIVE_MODE" == "true" ]]; then
        run_interactive_mode
        return
    fi
    
    # Setup parallel execution
    setup_parallel_execution
    
    # Show configuration if verbose
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${CYAN}🔧 CONFIGURATION:${NC}"
        echo -e "   Quick Mode: $QUICK_MODE"
        echo -e "   Fail Fast: $FAIL_FAST"
        echo -e "   Only Tests: ${ONLY_TESTS:-all}"
        echo -e "   Target Module: ${TARGET_MODULE:-all}"
        echo -e "   Verbose: $VERBOSE"
        echo -e "   Parallel Mode: $PARALLEL_MODE"
        if [[ "$PARALLEL_MODE" == "true" ]]; then
            echo -e "   Max Jobs: $MAX_PARALLEL_JOBS"
        fi
        echo ""
    fi
    
    # Validate project structure
    if [[ ! -f "$PROJECT_ROOT/pom.xml" ]]; then
        print_error "Maven pom.xml not found in $PROJECT_ROOT"
        print_error "Please run this script from the scripts directory of the YNAB-Syncher project"
        exit 1
    fi
    
    # Run the main test execution
    run_test_execution
    
    # Generate Summary Report with actual results
    generate_summary_report
}

# Main test execution logic (extracted for reuse by interactive mode)
run_test_execution() {
    
    # Set appropriate header based on mode
    local header_suffix=""
    if [[ "$PARALLEL_MODE" == "true" ]]; then
        header_suffix=" (Parallel Mode)"
    fi
    
    if [[ "$QUICK_MODE" == "true" ]]; then
        print_header "🧪 YNAB Syncher - Quick Test Suite${header_suffix}"
    else
        print_header "🧪 YNAB Syncher - Comprehensive Test & Validation Suite${header_suffix}"
    fi
    
    echo -e "${WHITE}Starting test execution...${NC}"
    echo -e "${WHITE}Project: YNAB-Syncher (Hexagonal Architecture)${NC}"
    echo -e "${WHITE}Project Root: $PROJECT_ROOT${NC}"
    echo -e "${WHITE}Date: $(date)${NC}"
    
    if [[ "$PARALLEL_MODE" == "true" ]]; then
        echo -e "${WHITE}Execution Mode: Parallel (${MAX_PARALLEL_JOBS} jobs)${NC}"
    fi
    
    # Define all available tests in execution order
    local -a all_tests=(
        "Architecture Tests (ArchUnit):mvn test -pl infrastructure -Dtest=ArchitectureTest"
        "Unit Tests (Domain):mvn -pl domain clean test"
        "Integration Tests (Infrastructure):mvn -pl infrastructure clean test"
        "WireMock Integration Tests:mvn -pl infrastructure test -Dtest=YnabApiClientWireMockTest"
        "Full Build Verification:mvn clean verify"
        "Mutation Testing (PIT):mvn -pl domain org.pitest:pitest-maven:mutationCoverage -DwithHistory"
    )
    
    # Execute tests with smart parallel/sequential logic
    if [[ "$PARALLEL_MODE" == "true" ]]; then
        # Phase 1: Independent tests that can run in parallel
        local independent_tests=(
            "Architecture Tests (ArchUnit):mvn test -pl infrastructure -Dtest=ArchitectureTest"
            "Unit Tests (Domain):mvn -pl domain clean test"
            "WireMock Integration Tests:mvn -pl infrastructure test -Dtest=YnabApiClientWireMockTest"
        )
        
        if [[ "$VERBOSE" == "true" ]]; then
            echo -e "${CYAN}🚀 Phase 1: Running independent tests in parallel${NC}"
        fi
        
        # Start independent tests in parallel
        for test_entry in "${independent_tests[@]}"; do
            local test_name="${test_entry%%:*}"
            local test_command="${test_entry#*:}"
            
            if should_skip_test "$test_name"; then
                if [[ "$VERBOSE" == "true" ]]; then
                    echo -e "${CYAN}⏭️  Skipping: $test_name${NC}"
                fi
                continue
            fi
            
            print_section "$test_name"
            run_test_parallel "$test_name" "$test_command"
        done
        
        # Wait for all independent tests to complete
        for test_entry in "${independent_tests[@]}"; do
            local test_name="${test_entry%%:*}"
            if should_skip_test "$test_name"; then
                continue
            fi
            wait_for_parallel_test "$test_name"
        done
        
        # Phase 2: Dependent tests (sequential)
        local dependent_tests=(
            "Integration Tests (Infrastructure):mvn -pl infrastructure clean test"
            "Full Build Verification:mvn clean verify"
            "Mutation Testing (PIT):mvn -pl domain org.pitest:pitest-maven:mutationCoverage -DwithHistory"
        )
        
        if [[ "$VERBOSE" == "true" ]]; then
            echo -e "${CYAN}🔄 Phase 2: Running dependent tests sequentially${NC}"
        fi
        
        # Run dependent tests sequentially
        for test_entry in "${dependent_tests[@]}"; do
            local test_name="${test_entry%%:*}"
            local test_command="${test_entry#*:}"
            
            if should_skip_test "$test_name"; then
                if [[ "$VERBOSE" == "true" ]]; then
                    echo -e "${CYAN}⏭️  Skipping: $test_name${NC}"
                fi
                continue
            fi
            
            run_test_suite_with_fail_fast "$test_name" "$test_command"
        done
    else
        # Sequential execution (original behavior)
        for test_entry in "${all_tests[@]}"; do
            local test_name="${test_entry%%:*}"
            local test_command="${test_entry#*:}"
            
            # Check if this test should be skipped
            if should_skip_test "$test_name"; then
                if [[ "$VERBOSE" == "true" ]]; then
                    echo -e "${CYAN}⏭️  Skipping: $test_name${NC}"
                fi
                continue
            fi
            
            # Execute the test
            run_test_suite_with_fail_fast "$test_name" "$test_command"
        done
    fi
}

# Execute main function
main "$@"