#!/bin/bash

# Exit on error
set -e

# Function to print with timestamp
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

# Check if act is installed
if ! command -v act &> /dev/null
then
    log "Error: 'act' is not installed. Please install it first:"
    log "brew install act"
    exit 1
fi

# Check if Docker is running
if ! docker info > /dev/null 2>&1
then
    log "Error: Docker is not running. Please start Docker first."
    exit 1
fi

# Create test secrets file if it doesn't exist
if [ ! -f ".github/tests/test.secrets" ]
then
    log "Creating test secrets file..."
    cat > .github/tests/test.secrets << EOF
AWS_ACCESS_KEY_ID=test-key-id
AWS_SECRET_ACCESS_KEY=test-secret-key
AWS_REGION=us-east-1
AWS_ACCOUNT_ID=123456789012
ACT=true
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
    if act "$event" --container-architecture linux/amd64 --secret-file .github/tests/test.secrets --workflows .github/workflows/ci-cd.yml -e .github/tests/events/$event.json
    then
        log "✅ Test passed: $test_name"
        return 0
    else
        log "❌ Test failed: $test_name"
        return 1
    fi
}

# Create test event files directory
mkdir -p .github/tests/events

# Create push event test file
cat > .github/tests/events/push.json << EOF
{
  "ref": "refs/heads/RRTB-14",
  "repository": {
    "name": "roboroxtestbot",
    "full_name": "nick-barban/roboroxtestbot"
  }
}
EOF

# Create pull request event test file
cat > .github/tests/events/pull_request.json << EOF
{
  "pull_request": {
    "head": {
      "ref": "main"
    }
  },
  "repository": {
    "name": "roboroxtestbot",
    "full_name": "nick-barban/roboroxtestbot"
  }
}
EOF

# Create workflow dispatch event test file
cat > .github/tests/events/workflow_dispatch.json << EOF
{
  "repository": {
    "name": "roboroxtestbot",
    "full_name": "nick-barban/roboroxtestbot"
  }
}
EOF

# Run tests
log "Starting workflow tests..."

# Test 1: Basic push event
test_workflow "Push event test" "push"

# Test 2: Pull request event
test_workflow "Pull request event test" "pull_request"

# Test 3: Manual workflow dispatch
test_workflow "Workflow dispatch test" "workflow_dispatch"

# Cleanup
log "Cleaning up..."
rm -rf .github/tests/events

log "All tests completed!" 