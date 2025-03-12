# GitHub Actions Workflow Tests

This directory contains tests for the GitHub Actions workflow using [act](https://github.com/nektos/act).

## Prerequisites

1. Install `act`:
```bash
# macOS
brew install act

# Linux
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash
```

2. Install Docker (required by act)

3. Set up environment variables:
```bash
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export AWS_REGION=your_region
export AWS_ACCOUNT_ID=your_account_id
```

## Running Tests

To run all tests:
```bash
chmod +x .github/tests/test-workflow.sh
.github/tests/test-workflow.sh
```

## Test Cases

1. **Push Event Test**: Tests the workflow trigger on push to RRTB-* branches
2. **Pull Request Test**: Tests the workflow trigger on pull requests to main
3. **Workflow Dispatch Test**: Tests manual workflow trigger

## Test Structure

- `test-workflow.sh`: Main test script
- `mock-policy.json`: Mock IAM policy for testing
- `.actrc`: Configuration file for act

## Notes

- Tests are run locally using act, which simulates GitHub Actions environment
- AWS credentials are required but operations are performed in a mock environment
- The script automatically creates and cleans up a .secrets file
- Each test case runs the workflow with different event triggers 