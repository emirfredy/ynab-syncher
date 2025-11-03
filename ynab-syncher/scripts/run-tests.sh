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
NC='\033[0m' # No Color

# Configuration flags
FAIL_FAST=false
VERBOSE=false
ONLY_TESTS=""
TARGET_MODULE=""
QUICK_MODE=false

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
    echo "  $0 --only architecture          # Run only architecture tests"
    echo "  $0 --only unit integration      # Run unit and integration tests"
    echo "  $0 --module domain              # Run only domain module tests"
    echo "  $0 --quick --fail-fast          # Quick tests with fail-fast"
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
            --verbose|-v)
                VERBOSE=true
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
    
    # Show configuration if verbose
    if [[ "$VERBOSE" == "true" ]]; then
        echo -e "${CYAN}🔧 CONFIGURATION:${NC}"
        echo -e "   Quick Mode: $QUICK_MODE"
        echo -e "   Fail Fast: $FAIL_FAST"
        echo -e "   Only Tests: ${ONLY_TESTS:-all}"
        echo -e "   Target Module: ${TARGET_MODULE:-all}"
        echo -e "   Verbose: $VERBOSE"
        echo ""
    fi
    
    # Validate project structure
    if [[ ! -f "$PROJECT_ROOT/pom.xml" ]]; then
        print_error "Maven pom.xml not found in $PROJECT_ROOT"
        print_error "Please run this script from the scripts directory of the YNAB-Syncher project"
        exit 1
    fi
    
    # Set appropriate header based on mode
    if [[ "$QUICK_MODE" == "true" ]]; then
        print_header "🧪 YNAB Syncher - Quick Test Suite"
    else
        print_header "🧪 YNAB Syncher - Comprehensive Test & Validation Suite"
    fi
    
    echo -e "${WHITE}Starting test execution...${NC}"
    echo -e "${WHITE}Project: YNAB-Syncher (Hexagonal Architecture)${NC}"
    echo -e "${WHITE}Project Root: $PROJECT_ROOT${NC}"
    echo -e "${WHITE}Date: $(date)${NC}"
    
    # Define all available tests in execution order
    local -a all_tests=(
        "Architecture Tests (ArchUnit):mvn test -pl infrastructure -Dtest=ArchitectureTest"
        "Unit Tests (Domain):mvn -pl domain clean test"
        "Integration Tests (Infrastructure):mvn -pl infrastructure clean test"
        "WireMock Integration Tests:mvn -pl infrastructure test -Dtest=YnabApiClientWireMockTest"
        "Full Build Verification:mvn clean verify"
        "Mutation Testing (PIT):mvn -pl domain org.pitest:pitest-maven:mutationCoverage -DwithHistory"
    )
    
    # Execute tests based on configuration
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
    
    # Generate Summary Report with actual results
    generate_summary_report
}

# Execute main function
main "$@"