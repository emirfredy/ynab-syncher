#!/bin/bash

# Dry-run version of merge-to-master.sh for testing
# Usage: ./scripts/dry-run-merge.sh [branch-name]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    echo -e "${BLUE}[DRY-RUN INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[DRY-RUN SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[DRY-RUN WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[DRY-RUN ERROR]${NC} $1"
}

# Check if we're in a git repository
check_git_repo() {
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a Git repository!"
        return 1
    fi
    print_success "✓ In Git repository"
}

# Check if branch exists
check_branch_exists() {
    local branch=$1
    if ! git show-ref --verify --quiet refs/heads/"$branch"; then
        print_error "Branch '$branch' does not exist!"
        return 1
    fi
    print_success "✓ Branch '$branch' exists"
}

# Check for uncommitted changes
check_uncommitted_changes() {
    if ! git diff-index --quiet HEAD --; then
        print_warning "You have uncommitted changes:"
        git status --short
        return 1
    fi
    print_success "✓ No uncommitted changes"
}

# Check if remote exists
check_remote() {
    if ! git remote get-url origin > /dev/null 2>&1; then
        print_error "No 'origin' remote found."
        return 1
    fi
    print_success "✓ Origin remote configured: $(git remote get-url origin)"
}

# Simulate test run
simulate_tests() {
    print_status "Would run: mvn clean test -q"
    print_success "✓ Tests would be executed"
}

# Main dry-run function
dry_run_merge() {
    local source_branch=$1
    
    print_status "DRY RUN: Merge process for branch: $source_branch"
    print_status "Commands that would be executed:"
    echo
    
    echo "1. git fetch origin"
    echo "2. git checkout master"
    echo "3. git pull origin master"
    echo "4. git merge --no-ff $source_branch -m \"Merge branch '$source_branch' into master\""
    echo "5. git push origin master"
    echo
    
    print_success "✓ All operations would succeed (in dry-run mode)"
}

# Main script execution
main() {
    print_status "YNAB-Syncher Branch Merger - DRY RUN"
    print_status "===================================="
    
    local all_checks_passed=true
    
    # Run all checks
    check_git_repo || all_checks_passed=false
    check_remote || all_checks_passed=false
    check_uncommitted_changes || all_checks_passed=false
    
    # Determine source branch
    local source_branch
    if [ $# -eq 0 ]; then
        source_branch=$(git branch --show-current)
        if [ "$source_branch" = "master" ] || [ "$source_branch" = "main" ]; then
            print_error "Cannot merge master/main branch into itself!"
            all_checks_passed=false
        else
            print_status "Using current branch: $source_branch"
        fi
    else
        source_branch=$1
        check_branch_exists "$source_branch" || all_checks_passed=false
        print_status "Using specified branch: $source_branch"
    fi
    
    simulate_tests
    
    if [ "$all_checks_passed" = true ]; then
        echo
        dry_run_merge "$source_branch"
        print_success "DRY RUN COMPLETED: All prerequisites met!"
        print_status "Run './scripts/merge-to-master.sh $source_branch' to execute the actual merge."
    else
        print_error "DRY RUN FAILED: Please fix the issues above before merging."
        exit 1
    fi
}

# Check for help flag
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    echo "YNAB-Syncher Branch Merger - Dry Run"
    echo "Usage: $0 [branch-name]"
    echo ""
    echo "This script performs a dry-run of the merge process to validate prerequisites."
    echo "No actual git operations are performed."
    exit 0
fi

# Run main function
main "$@"