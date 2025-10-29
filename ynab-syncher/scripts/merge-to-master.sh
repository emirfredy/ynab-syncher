#!/bin/bash

# Script to merge a feature branch to master and push to GitHub
# Usage: ./scripts/merge-to-master.sh [branch-name]
# If no branch name is provided, uses current branch

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if we're in a git repository
check_git_repo() {
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        print_error "Not in a Git repository!"
        exit 1
    fi
}

# Function to check if branch exists
check_branch_exists() {
    local branch=$1
    if ! git show-ref --verify --quiet refs/heads/"$branch"; then
        print_error "Branch '$branch' does not exist!"
        exit 1
    fi
}

# Function to check for uncommitted changes
check_uncommitted_changes() {
    if ! git diff-index --quiet HEAD --; then
        print_error "You have uncommitted changes. Please commit or stash them first."
        git status --short
        exit 1
    fi
}

# Function to run tests
run_tests() {
    print_status "Running tests to ensure code quality..."
    if mvn clean test -q; then
        print_success "All tests passed!"
    else
        print_error "Tests failed! Cannot merge to master."
        exit 1
    fi
}

# Function to check if remote exists
check_remote() {
    if ! git remote get-url origin > /dev/null 2>&1; then
        print_error "No 'origin' remote found. Please add a remote first."
        exit 1
    fi
}

# Main merge function
merge_to_master() {
    local source_branch=$1
    
    print_status "Starting merge process for branch: $source_branch"
    
    # Fetch latest changes from remote
    print_status "Fetching latest changes from remote..."
    git fetch origin
    
    # Switch to master and pull latest
    print_status "Switching to master branch..."
    git checkout master
    
    print_status "Pulling latest changes on master..."
    git pull origin master
    
    # Check if master is up to date with remote
    local local_commit=$(git rev-parse master)
    local remote_commit=$(git rev-parse origin/master)
    
    if [ "$local_commit" != "$remote_commit" ]; then
        print_error "Local master is not up to date with remote. Please pull first."
        exit 1
    fi
    
    # Merge the feature branch
    print_status "Merging $source_branch into master..."
    if git merge --no-ff "$source_branch" -m "Merge branch '$source_branch' into master"; then
        print_success "Successfully merged $source_branch into master!"
    else
        print_error "Merge failed! Please resolve conflicts manually."
        exit 1
    fi
    
    # Push to remote
    print_status "Pushing master to remote..."
    if git push origin master; then
        print_success "Successfully pushed master to GitHub!"
    else
        print_error "Failed to push to remote!"
        exit 1
    fi
    
    # Optional: Delete the feature branch
    read -p "Do you want to delete the local branch '$source_branch'? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git branch -d "$source_branch"
        print_success "Deleted local branch '$source_branch'"
        
        # Check if remote branch exists and offer to delete it
        if git show-ref --verify --quiet refs/remotes/origin/"$source_branch"; then
            read -p "Do you want to delete the remote branch 'origin/$source_branch'? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                git push origin --delete "$source_branch"
                print_success "Deleted remote branch 'origin/$source_branch'"
            fi
        fi
    fi
}

# Main script execution
main() {
    print_status "YNAB-Syncher Branch Merger Script"
    print_status "=================================="
    
    # Check prerequisites
    check_git_repo
    check_remote
    check_uncommitted_changes
    
    # Determine source branch
    local source_branch
    if [ $# -eq 0 ]; then
        # No argument provided, use current branch
        source_branch=$(git branch --show-current)
        if [ "$source_branch" = "master" ] || [ "$source_branch" = "main" ]; then
            print_error "Cannot merge master/main branch into itself!"
            exit 1
        fi
        print_status "Using current branch: $source_branch"
    else
        # Use provided branch name
        source_branch=$1
        check_branch_exists "$source_branch"
        print_status "Using specified branch: $source_branch"
    fi
    
    # Confirm merge operation
    print_warning "This will merge '$source_branch' into 'master' and push to GitHub."
    read -p "Are you sure you want to continue? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        print_status "Merge cancelled."
        exit 0
    fi
    
    # Switch to source branch and run tests
    print_status "Switching to $source_branch for testing..."
    git checkout "$source_branch"
    
    # Run tests on the source branch
    run_tests
    
    # Perform the merge
    merge_to_master "$source_branch"
    
    print_success "Branch merge completed successfully!"
    print_status "Current branch: $(git branch --show-current)"
    print_status "Latest commit: $(git log -1 --oneline)"
}

# Help function
show_help() {
    echo "YNAB-Syncher Branch Merger Script"
    echo "Usage: $0 [branch-name]"
    echo ""
    echo "This script safely merges a feature branch into master and pushes to GitHub."
    echo ""
    echo "Options:"
    echo "  branch-name    The name of the branch to merge (optional)"
    echo "                 If not provided, uses the current branch"
    echo "  -h, --help     Show this help message"
    echo ""
    echo "Prerequisites:"
    echo "  - Must be in a Git repository"
    echo "  - Must have 'origin' remote configured"
    echo "  - No uncommitted changes"
    echo "  - All tests must pass"
    echo ""
    echo "Example:"
    echo "  $0                           # Merge current branch"
    echo "  $0 feature/new-feature       # Merge specific branch"
}

# Check for help flag
if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
    show_help
    exit 0
fi

# Run main function
main "$@"