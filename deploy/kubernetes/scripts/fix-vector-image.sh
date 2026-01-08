#!/usr/bin/env bash
# Quick fix script to push vector image to ECR and update deployment
# Usage: ./fix-vector-image.sh [tag]

set -euo pipefail

TAG="${1:-latest}"
AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-954754807219}"
AWS_REGION="${AWS_REGION:-us-east-1}"
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE_NAME="logwise-vector"
ECR_IMAGE="${ECR_REGISTRY}/${IMAGE_NAME}:${TAG}"
LOCAL_IMAGE="${IMAGE_NAME}:${TAG}"

echo "=== Fixing Vector Image Pull Issue ==="
echo "Local image: ${LOCAL_IMAGE}"
echo "ECR image: ${ECR_IMAGE}"
echo ""

# Check if local image exists
if ! docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${LOCAL_IMAGE}$"; then
  echo "ERROR: Local image ${LOCAL_IMAGE} not found!"
  echo "Available vector images:"
  docker images | grep -E "vector|logwise-vector" || true
  exit 1
fi

# Login to ECR
echo "Step 1: Logging into ECR..."
aws ecr get-login-password --region "${AWS_REGION}" | \
  docker login --username AWS --password-stdin "${ECR_REGISTRY}" || {
  echo "ERROR: Failed to login to ECR"
  exit 1
}

# Create ECR repository if it doesn't exist
echo ""
echo "Step 2: Ensuring ECR repository exists..."
if ! aws ecr describe-repositories --repository-names "${IMAGE_NAME}" --region "${AWS_REGION}" >/dev/null 2>&1; then
  echo "Creating ECR repository: ${IMAGE_NAME}"
  aws ecr create-repository \
    --repository-name "${IMAGE_NAME}" \
    --region "${AWS_REGION}" \
    --image-scanning-configuration scanOnPush=true \
    --encryption-configuration encryptionType=AES256 || {
    echo "ERROR: Failed to create ECR repository"
    exit 1
  }
  echo "Repository created successfully"
else
  echo "Repository already exists"
fi

# Tag image
echo ""
echo "Step 3: Tagging image..."
docker tag "${LOCAL_IMAGE}" "${ECR_IMAGE}" || {
  echo "ERROR: Failed to tag image"
  exit 1
}
echo "Tagged: ${LOCAL_IMAGE} -> ${ECR_IMAGE}"

# Push image
echo ""
echo "Step 4: Pushing image to ECR..."
docker push "${ECR_IMAGE}" || {
  echo "ERROR: Failed to push image"
  exit 1
}
echo "Pushed successfully!"

# Update deployment
echo ""
echo "Step 5: Updating Kubernetes deployment..."
kubectl set image deployment/vector-logs \
  vector="${ECR_IMAGE}" \
  -n logwise || {
  echo "ERROR: Failed to update deployment"
  exit 1
}
echo "Deployment updated successfully!"

echo ""
echo "=== Done ==="
echo "Image is now available at: ${ECR_IMAGE}"
echo "Deployment will restart with the new image."
echo ""
echo "Monitor the rollout with:"
echo "  kubectl rollout status deployment/vector-logs -n logwise"
echo ""
echo "Check pod status with:"
echo "  kubectl get pods -n logwise -l app=vector-logs"

