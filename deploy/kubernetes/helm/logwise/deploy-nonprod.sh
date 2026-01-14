#!/usr/bin/env bash
# Quick deployment script for LogWise non-production environment
# Usage: ./deploy-nonprod.sh [AWS_ACCESS_KEY] [AWS_SECRET_KEY]

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
DOCKERHUB_USERNAME="${DOCKERHUB_USERNAME:-varunwalia}"
DOCKERHUB_PASSWORD="${DOCKERHUB_PASSWORD:-dckr_pat_9NjWEeLVlVr10A-DyZjrJYHYs_k}"
REGISTRY="dockerhub"
TAG="${TAG:-1.0.0}"
NAMESPACE="logwise"
IMAGE_REGISTRY="varunwalia"
PLATFORM="linux/amd64"

# AWS credentials (optional, can be passed as arguments)
AWS_ACCESS_KEY="${1:-}"
AWS_SECRET_KEY="${2:-}"

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

echo -e "${GREEN}=== LogWise Non-Production Deployment ===${NC}\n"

# Step 1: Build and push images
echo -e "${YELLOW}Step 1: Building and pushing Docker images...${NC}"
cd "$REPO_ROOT"

PLATFORM="$PLATFORM" \
ENV=nonprod \
DOCKERHUB_USERNAME="$DOCKERHUB_USERNAME" \
DOCKERHUB_PASSWORD="$DOCKERHUB_PASSWORD" \
REGISTRY="$REGISTRY" \
TAG="$TAG" \
./deploy/kubernetes/scripts/build-and-push.sh

if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to build/push images. Exiting.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Images built and pushed successfully${NC}\n"

# Step 2: Create namespace
echo -e "${YELLOW}Step 2: Creating namespace...${NC}"
if kubectl get namespace "$NAMESPACE" &>/dev/null; then
    echo "Namespace $NAMESPACE already exists. Adopting it for Helm..."
    kubectl label namespace "$NAMESPACE" app.kubernetes.io/managed-by=Helm --overwrite || true
    kubectl annotate namespace "$NAMESPACE" meta.helm.sh/release-name=logwise --overwrite || true
    kubectl annotate namespace "$NAMESPACE" meta.helm.sh/release-namespace=logwise --overwrite || true
else
    kubectl create namespace "$NAMESPACE"
fi
echo -e "${GREEN}✓ Namespace ready${NC}\n"

# Step 3: Create Docker Hub secret
echo -e "${YELLOW}Step 3: Creating Docker Hub image pull secret...${NC}"
if kubectl get secret dockerhub-secret -n "$NAMESPACE" &>/dev/null; then
    echo "Secret dockerhub-secret already exists. Updating..."
    kubectl delete secret dockerhub-secret -n "$NAMESPACE" || true
fi

kubectl create secret docker-registry dockerhub-secret \
  --docker-server=docker.io \
  --docker-username="$DOCKERHUB_USERNAME" \
  --docker-password="$DOCKERHUB_PASSWORD" \
  --docker-email="${DOCKERHUB_USERNAME}@example.com" \
  --namespace="$NAMESPACE" \
  --dry-run=client -o yaml | kubectl apply -f -

echo -e "${GREEN}✓ Docker Hub secret created${NC}\n"

# Step 4: Deploy with Helm
echo -e "${YELLOW}Step 4: Deploying with Helm...${NC}"
cd "$SCRIPT_DIR"

HELM_CMD="helm upgrade --install logwise . \
  --namespace $NAMESPACE \
  --create-namespace \
  --values values-nonprod.yaml \
  --set global.imageRegistry=$IMAGE_REGISTRY \
  --set 'global.imagePullSecrets[0]=dockerhub-secret' \
  --set metricsServer.enabled=false \
  --set orchestrator.image.tag=$TAG \
  --set spark.master.image.tag=$TAG \
  --set spark.worker.image.tag=$TAG \
  --set vector.image.tag=$TAG \
  --set healthcheck.image.tag=$TAG"

# Add AWS credentials if provided
if [ -n "$AWS_ACCESS_KEY" ] && [ -n "$AWS_SECRET_KEY" ]; then
    HELM_CMD="$HELM_CMD \
      --set aws.accessKeyId=$AWS_ACCESS_KEY \
      --set aws.secretAccessKey=$AWS_SECRET_KEY"
    echo "AWS credentials will be set via --set flags"
else
    echo -e "${YELLOW}Warning: AWS credentials not provided. Set them manually or use secrets management.${NC}"
fi

eval $HELM_CMD

if [ $? -ne 0 ]; then
    echo -e "${RED}Helm deployment failed. Exiting.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Helm deployment completed${NC}\n"

# Step 5: Wait for pods
echo -e "${YELLOW}Step 5: Waiting for pods to be ready...${NC}"
echo "This may take a few minutes. Press Ctrl+C to skip waiting..."

if kubectl wait --for=condition=ready pod -l app=orchestrator -n "$NAMESPACE" --timeout=300s 2>/dev/null; then
    echo -e "${GREEN}✓ Orchestrator is ready${NC}"
else
    echo -e "${YELLOW}⚠ Orchestrator not ready yet (this is okay, it may take longer)${NC}"
fi

# Step 6: Show status
echo -e "\n${GREEN}=== Deployment Status ===${NC}\n"
kubectl get pods -n "$NAMESPACE"

echo -e "\n${GREEN}=== Services ===${NC}\n"
kubectl get svc -n "$NAMESPACE"

echo -e "\n${GREEN}=== Next Steps ===${NC}"
echo "1. Check pod status: kubectl get pods -n $NAMESPACE"
echo "2. Check logs: kubectl logs <pod-name> -n $NAMESPACE"
echo "3. Port forward services:"
echo "   - Orchestrator: kubectl port-forward -n $NAMESPACE svc/orchestrator 30081:8080"
echo "   - Grafana: kubectl port-forward -n $NAMESPACE svc/grafana 30080:3000"
echo "4. Access services:"
echo "   - Orchestrator: http://localhost:30081"
echo "   - Grafana: http://localhost:30080 (admin/admin)"

echo -e "\n${GREEN}Deployment completed!${NC}"
