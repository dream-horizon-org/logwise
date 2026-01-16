#!/bin/bash

# Smart install/upgrade script for Logwise Helm chart
# Automatically detects if release exists and uses install or upgrade accordingly

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHART_DIR="$SCRIPT_DIR"
NAMESPACE="logwise"
RELEASE_NAME="logwise"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}=== Logwise Helm Chart Install/Upgrade ===${NC}\n"

# Check prerequisites
command -v helm >/dev/null 2>&1 || { echo -e "${RED}Error: helm is not installed${NC}" >&2; exit 1; }
command -v kubectl >/dev/null 2>&1 || { echo -e "${RED}Error: kubectl is not installed${NC}" >&2; exit 1; }

# Check if cluster is accessible
if ! kubectl cluster-info >/dev/null 2>&1; then
    echo -e "${RED}Error: Cannot connect to Kubernetes cluster${NC}"
    exit 1
fi

# Check if release exists
if helm list -n "$NAMESPACE" | grep -q "^${RELEASE_NAME}"; then
    ACTION="upgrade"
    echo -e "${YELLOW}Existing release found. Will upgrade...${NC}\n"
else
    ACTION="install"
    echo -e "${YELLOW}No existing release found. Will install...${NC}\n"
fi

# Build base command
HELM_CMD="helm $ACTION $RELEASE_NAME ."
HELM_CMD="$HELM_CMD --namespace $NAMESPACE"

if [ "$ACTION" == "install" ]; then
    HELM_CMD="$HELM_CMD --create-namespace"
fi

HELM_CMD="$HELM_CMD --values values-local.yaml"

# Add AWS credentials if provided
if [ -n "$1" ] && [ "$1" != "--" ]; then
    HELM_CMD="$HELM_CMD --set aws.accessKeyId=$1"
fi

if [ -n "$2" ] && [ "$2" != "--" ]; then
    HELM_CMD="$HELM_CMD --set aws.secretAccessKey=$2"
fi

if [ -n "$3" ] && [ "$3" != "--" ]; then
    HELM_CMD="$HELM_CMD --set aws.s3BucketName=$3"
fi

if [ -n "$4" ] && [ "$4" != "--" ]; then
    HELM_CMD="$HELM_CMD --set aws.s3AthenaOutput=$4"
fi

# If no arguments provided, prompt for them
if [ -z "$1" ]; then
    echo -e "${YELLOW}AWS Configuration (press Enter to skip):${NC}"
    read -p "AWS Access Key ID: " AWS_ACCESS_KEY
    read -p "AWS Secret Access Key: " AWS_SECRET_KEY
    read -p "S3 Bucket Name: " S3_BUCKET
    read -p "S3 Athena Output (e.g., s3://bucket/athena-output/): " S3_ATHENA_OUTPUT
    
    if [ -n "$AWS_ACCESS_KEY" ]; then
        HELM_CMD="$HELM_CMD --set aws.accessKeyId=$AWS_ACCESS_KEY"
    fi
    if [ -n "$AWS_SECRET_KEY" ]; then
        HELM_CMD="$HELM_CMD --set aws.secretAccessKey=$AWS_SECRET_KEY"
    fi
    if [ -n "$S3_BUCKET" ]; then
        HELM_CMD="$HELM_CMD --set aws.s3BucketName=$S3_BUCKET"
    fi
    if [ -n "$S3_ATHENA_OUTPUT" ]; then
        HELM_CMD="$HELM_CMD --set aws.s3AthenaOutput=$S3_ATHENA_OUTPUT"
    fi
fi

echo -e "\n${YELLOW}Command:${NC}"
echo "$HELM_CMD"
echo ""

read -p "Proceed with $ACTION? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Cancelled${NC}"
    exit 0
fi

# Execute command
cd "$CHART_DIR"
eval $HELM_CMD

if [ $? -eq 0 ]; then
    echo -e "\n${GREEN}✓ $ACTION completed successfully${NC}\n"
    
    # Offer to set up port forwarding
    if [ -f "$CHART_DIR/post-install.sh" ]; then
        read -p "Set up port forwarding now? (Y/n): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Nn]$ ]]; then
            "$CHART_DIR/post-install.sh"
        fi
    fi
else
    echo -e "\n${RED}✗ $ACTION failed${NC}"
    exit 1
fi
