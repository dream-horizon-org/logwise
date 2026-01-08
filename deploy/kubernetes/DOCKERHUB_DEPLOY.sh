#!/bin/bash
# Quick deployment script for Docker Hub
# Usage: ./DOCKERHUB_DEPLOY.sh [nonprod|prod] [tag]

set -e

ENV="${1:-nonprod}"
TAG="${2:-1.0.0}"
DOCKERHUB_USERNAME="varunwalia"
REGISTRY="dockerhub"

echo "=== Deploying LogWise to Kubernetes ==="
echo "Environment: $ENV"
echo "Tag: $TAG"
echo "Docker Hub Username: $DOCKERHUB_USERNAME"
echo "Images will be pushed as:"
echo "  - ${DOCKERHUB_USERNAME}/logwise-orchestrator:${TAG}"
echo "  - ${DOCKERHUB_USERNAME}/logwise-spark:${TAG}"
echo "  - ${DOCKERHUB_USERNAME}/logwise-vector:${TAG}"
echo "  - ${DOCKERHUB_USERNAME}/logwise-healthcheck-dummy:${TAG}"
echo ""

# Check if logged into Docker Hub
if ! docker info | grep -q "Username:"; then
  echo "⚠️  Not logged into Docker Hub. Please login first:"
  echo "   docker login"
  echo ""
  read -p "Press Enter to continue after logging in, or Ctrl+C to cancel..."
fi

# Navigate to scripts directory
cd "$(dirname "$0")/scripts"

# Run setup
ENV="$ENV" \
  DOCKERHUB_USERNAME="$DOCKERHUB_USERNAME" \
  REGISTRY="$REGISTRY" \
  TAG="$TAG" \
  ./setup-k8s.sh "$ENV"

echo ""
echo "✅ Deployment complete!"
echo ""
echo "Verify with:"
echo "  kubectl get pods -n logwise"
echo "  kubectl get deployments -n logwise"


