#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
WHITE='\033[1;37m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Declare associative arrays for tracking results
declare -A test_results
declare -A test_counts
declare -A test_durations

# Print functions
print_header() {
    echo ""
    echo "=================================================================================="
    echo -e "${BLUE}$1${NC}"
    echo "=================================================================================="
    echo ""
}

print_section() {
    echo -e "${CYAN}🔍 $1${NC}"
    echo "================================================================================"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
    echo ""
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
    echo ""
}

# Extract test count from Maven output
extract_test_count() {
    local output="$1"
    # Try different patterns since -q might suppress some output
    count=$(echo "$output" | grep -E "Tests run: [0-9]+" | tail -1 | sed -E 's/.*Tests run: ([0-9]+).*/\1/' || echo "")
    if [[ -z "$count" ]]; then
        count="N/A"
    fi
    echo "${count}"
}

# Run test suite with timing
run_test_suite() {
    local name="$1"
    local command="$2"
    local start_time=$(date +%s)
    
    print_section "$name"
    echo "Running: $command"
    echo ""
    
    if output=$(eval "$command" 2>&1); then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        test_results["$name"]="PASS"
        test_durations["$name"]="${duration}s"
        
        local count=$(extract_test_count "$output")
        test_counts["$name"]="$count"
        
        print_success "$name completed successfully (${duration}s)"
    else
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        
        test_results["$name"]="FAIL"
        test_durations["$name"]="${duration}s"
        test_counts["$name"]="Failed"
        
        print_error "$name failed (${duration}s)"
        echo -e "${RED}Error output:${NC}"
        echo "$output" | tail -10
    fi
}

# Main execution
main() {
    print_header "🧪 YNAB Syncher - Quick Test & Validation Suite"
    
    echo -e "${WHITE}Running key test validations...${NC}"
    echo -e "${WHITE}Project: YNAB-Syncher (Hexagonal Architecture)${NC}"
    echo -e "${WHITE}Date: $(date)${NC}"
    
    # 1. Architecture Validation (ArchUnit Tests)
    run_test_suite "Architecture Tests (ArchUnit)" \
        "mvn test -pl infrastructure -Dtest=ArchitectureTest -q"
    
    # 2. Domain Unit Tests
    run_test_suite "Unit Tests (Domain)" \
        "mvn -pl domain test -q"
    
    # 3. Infrastructure Integration Tests
    run_test_suite "Integration Tests (Infrastructure)" \
        "mvn -pl infrastructure test -q"
    
    # 4. Full Build Verification
    run_test_suite "Full Build Verification" \
        "mvn clean verify -q"
    
    # Results Summary
    print_header "📊 Quick Test Results Summary"
    
    echo -e "${WHITE}Test Results:${NC}\n"
    
    local total_tests=0
    local passed_tests=0
    local failed_tests=0
    
    # Test categories in order
    local categories=(
        "Architecture Tests (ArchUnit)"
        "Unit Tests (Domain)" 
        "Integration Tests (Infrastructure)"
        "Full Build Verification"
    )
    
    for category in "${categories[@]}"; do
        local status="${test_results[$category]:-SKIP}"
        local count="${test_counts[$category]:-0}"
        local duration="${test_durations[$category]:-N/A}"
        
        if [[ "$status" == "PASS" ]]; then
            echo -e "  ${GREEN}✅${NC} $category: ${GREEN}PASS${NC} ($count tests, $duration)"
            ((passed_tests++))
        elif [[ "$status" == "FAIL" ]]; then
            echo -e "  ${RED}❌${NC} $category: ${RED}FAIL${NC} ($duration)"
            ((failed_tests++))
        else
            echo -e "  ${YELLOW}⏸️${NC}  $category: ${YELLOW}SKIP${NC}"
        fi
        ((total_tests++))
    done
    
    # Summary statistics
    echo ""
    echo -e "${WHITE}📈 Summary Statistics:${NC}"
    echo -e "  Total Test Suites: $total_tests"
    echo -e "  ${GREEN}Passed: $passed_tests${NC}"
    echo -e "  ${RED}Failed: $failed_tests${NC}"
    
    # Overall status
    echo ""
    if [[ $failed_tests -eq 0 ]]; then
        echo -e "${GREEN}🎉 ALL TEST SUITES PASSED! 🎉${NC}"
        echo -e "${GREEN}✅ Production-ready quality confirmed${NC}"
        echo ""
        echo -e "${CYAN}💡 For complete validation including mutation testing, run: ./run-tests.sh${NC}"
        return 0
    else
        echo -e "${RED}❌ SOME TEST SUITES FAILED${NC}"
        echo -e "${RED}Please check the failed test outputs above${NC}"
        return 1
    fi
}

# Execute main function
main "$@"