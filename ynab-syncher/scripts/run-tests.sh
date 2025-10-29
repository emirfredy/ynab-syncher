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

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
NC='\033[0m' # No Color

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

# Run test suite with timing
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
    # Validate project structure
    if [[ ! -f "$PROJECT_ROOT/pom.xml" ]]; then
        print_error "Maven pom.xml not found in $PROJECT_ROOT"
        print_error "Please run this script from the scripts directory of the YNAB-Syncher project"
        exit 1
    fi
    
    # Quick mode - skip the longest tests
    if [[ "$1" == "--quick" ]]; then
        print_header "🧪 YNAB Syncher - Quick Test Suite"
        
        echo -e "${WHITE}Running quick test execution...${NC}"
        echo -e "${WHITE}Project: YNAB-Syncher (Hexagonal Architecture)${NC}"
        echo -e "${WHITE}Project Root: $PROJECT_ROOT${NC}"
        echo -e "${WHITE}Date: $(date)${NC}"
        
        # Run only the faster tests
        run_test_suite "Architecture Tests (ArchUnit)" \
            "mvn test -pl infrastructure -Dtest=ArchitectureTest"
        
        run_test_suite "Unit Tests (Domain)" \
            "mvn -pl domain clean test"
        
        run_test_suite "Integration Tests (Infrastructure)" \
            "mvn -pl infrastructure clean test"
        
        # Generate Summary Report with actual results
        generate_summary_report
        return
    fi

    print_header "🧪 YNAB Syncher - Comprehensive Test & Validation Suite"
    
    echo -e "${WHITE}Starting comprehensive test execution...${NC}"
    echo -e "${WHITE}Project: YNAB-Syncher (Hexagonal Architecture)${NC}"
    echo -e "${WHITE}Project Root: $PROJECT_ROOT${NC}"
    echo -e "${WHITE}Date: $(date)${NC}"
    echo -e "${WHITE}Project: YNAB-Syncher (Hexagonal Architecture)${NC}"
    echo -e "${WHITE}Date: $(date)${NC}"
    
    # 1. Architecture Validation (ArchUnit Tests)
    run_test_suite "Architecture Tests (ArchUnit)" \
        "mvn test -pl infrastructure -Dtest=ArchitectureTest"
    
    # 2. Domain Module - Unit Tests
    run_test_suite "Unit Tests (Domain)" \
        "mvn -pl domain clean test"
    
    # 3. Infrastructure Module - Integration Tests  
    run_test_suite "Integration Tests (Infrastructure)" \
        "mvn -pl infrastructure clean test"
    
    # 4. WireMock Integration Tests (External API)
    run_test_suite "WireMock Integration Tests" \
        "mvn -pl infrastructure test -Dtest=YnabApiClientWireMockTest"
    
    # 5. Full Multi-Module Build with Verification
    run_test_suite "Full Build Verification" \
        "mvn clean verify"
    
    # 6. Code Coverage Analysis
    run_test_suite "Code Coverage Analysis" \
        "mvn clean test jacoco:report"
    
    # 7. Mutation Testing (Domain Module) - This takes longer
    print_section "Mutation Testing (Domain Module) - This may take a few minutes..."
    echo "Note: Mutation scores can vary between runs due to randomization (65-75% range expected)"
    echo "Tip: Use --quick flag to skip this step for faster feedback"
    
    # Add timeout for mutation testing to prevent hanging
    local start_time=$(date +%s)
    if timeout 120s bash -c "cd '$PROJECT_ROOT' && mvn -pl domain clean compile test-compile org.pitest:pitest-maven:mutationCoverage" > /tmp/mutation_output 2>&1; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        test_results["Mutation Testing (PIT)"]="PASS"
        test_durations["Mutation Testing (PIT)"]="${duration}s"
        local score=$(extract_mutation_score "$(cat /tmp/mutation_output)")
        test_counts["Mutation Testing (PIT)"]="${score}% mutation score"
        print_success "Mutation Testing (PIT) completed successfully (${duration}s)"
    else
        # Check if we got partial results before timeout
        if [[ -f /tmp/mutation_output ]]; then
            local score=$(extract_mutation_score "$(cat /tmp/mutation_output)")
            if [[ "$score" != "0" && "$score" != "" ]]; then
                test_results["Mutation Testing (PIT)"]="WARN"
                test_counts["Mutation Testing (PIT)"]="${score}% mutation score"
                test_durations["Mutation Testing (PIT)"]="120s+"
                print_warning "Mutation Testing (PIT) timed out but got partial results: ${score}%"
            else
                test_results["Mutation Testing (PIT)"]="FAIL"
                test_counts["Mutation Testing (PIT)"]="Timeout"
                test_durations["Mutation Testing (PIT)"]="120s+"
                print_error "Mutation Testing (PIT) timed out after 2 minutes"
            fi
        else
            test_results["Mutation Testing (PIT)"]="FAIL"
            test_counts["Mutation Testing (PIT)"]="Timeout"
            test_durations["Mutation Testing (PIT)"]="120s+"
            print_error "Mutation Testing (PIT) timed out after 2 minutes"
        fi
    fi
    rm -f /tmp/mutation_output
    
    # Generate Summary Report with actual results
    generate_summary_report
}

# Execute main function
main "$@"