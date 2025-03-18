#!/bin/bash

# Script to upload post templates to an S3 bucket
# Usage: ./upload_templates.sh [bucket_name]
# If bucket_name is not provided, it will use the default bucket

set -e  # Exit immediately if a command exits with a non-zero status

# Default bucket name
DEFAULT_BUCKET="rrtbappstack-rrtbposts27ca6c36-j9b8hmjn5oul"

# Get bucket name from command line argument or use default
BUCKET_NAME=${1:-$DEFAULT_BUCKET}

# Directory containing post templates
TEMPLATES_DIR="$(dirname "$0")"

echo "Starting upload to S3 bucket: $BUCKET_NAME"

# Check if AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo "Error: AWS CLI is not installed. Please install it first."
    exit 1
fi

# Check if user is authenticated with AWS
if ! aws sts get-caller-identity &> /dev/null; then
    echo "Error: You are not authenticated with AWS. Please run 'aws configure' first."
    exit 1
fi

# Count total number of .post files
POST_FILES=$(find "$TEMPLATES_DIR" -name "*.post" | wc -l)
echo "Found $POST_FILES post template files to upload."

# Upload each .post file to S3
UPLOADED=0
for file in "$TEMPLATES_DIR"/*.post; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        echo "Uploading $filename to s3://$BUCKET_NAME/templates/$filename"
        aws s3 cp "$file" "s3://$BUCKET_NAME/templates/$filename"
        UPLOADED=$((UPLOADED + 1))
        echo "Progress: $UPLOADED/$POST_FILES"
    fi
done

echo "Upload complete! $UPLOADED files uploaded to s3://$BUCKET_NAME/templates/"
echo "Templates are accessible at: https://$BUCKET_NAME.s3.amazonaws.com/templates/" 