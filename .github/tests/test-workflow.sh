#!/bin/bash

# Exit on error
set -e

# Function to print with timestamp
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# Check if act is installed
if ! command -v act &> /dev/null; then
    log "Error: 'act' is not installed. Please install it first:"
    log "brew install act"
    exit 1
}

# Check if required environment variables are set
required_vars=("AWS_ACCESS_KEY_ID" "AWS_SECRET_ACCESS_KEY" "AWS_REGION" "AWS_ACCOUNT_ID")
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        log "Error: $var is not set"
        exit 1
    fi
done

# Create test secrets file if it doesn't exist
if [ ! -f ".secrets" ]; then
    log "Creating .secrets file..."
    cat > .secrets << EOF
AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
AWS_REGION=${AWS_REGION}
AWS_ACCOUNT_ID=${AWS_ACCOUNT_ID}
EOF
fi

# Test cases
test_workflow() {
    local test_name=$1
    local event=$2
    shift 2
    local extra_args=("$@")

    log "Running test: $test_name"
    
    # Run the workflow with act
    if act $event --secret-file .secrets "${extra_args[@]}" --workflows .github/workflows/ci-cd.yml; then
        log "✅ Test passed: $test_name"
        return 0
    else
        log "❌ Test failed: $test_name"
        return 1
    fi
}

# Run tests
log "Starting workflow tests..."

# Test 1: Basic push event
test_workflow "Push event test" "push" --branch "RRTB-14"

# Test 2: Pull request event
test_workflow "Pull request event test" "pull_request" --branch "main"

# Test 3: Manual workflow dispatch
test_workflow "Workflow dispatch test" "workflow_dispatch"

# Cleanup
log "Cleaning up..."
rm -f .secrets

log "All tests completed!" 