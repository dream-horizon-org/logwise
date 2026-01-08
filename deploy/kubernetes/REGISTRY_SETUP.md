# Container Registry Setup Guide

This guide explains how to configure container registries for LogWise deployments.

## Quick Fix for Current Issue

You're getting an error because `REGISTRY=dockerhub` is not a valid format. Choose one of the options below.

## Option 1: Use AWS ECR (Recommended for AWS EKS)

Since you're using AWS EKS, ECR is the best choice:

```bash
# Set your AWS account ID and region
export AWS_ACCOUNT_ID=954754807219
export AWS_REGION=us-east-1
export REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
export TAG=1.0.0

# Deploy
cd deploy/kubernetes
ENV=nonprod REGISTRY="$REGISTRY" TAG="$TAG" ./scripts/setup-k8s.sh nonprod
```

The script will automatically:
- Authenticate with ECR
- Create repositories if they don't exist
- Push images to ECR

## Option 2: Use Docker Hub

If you want to use Docker Hub, you need to provide your Docker Hub username:

```bash
# Set your Docker Hub credentials
export DOCKERHUB_USERNAME=your-dockerhub-username
export DOCKERHUB_PASSWORD=your-dockerhub-password  # Or use docker login first
export REGISTRY=dockerhub
export TAG=1.0.0

# Deploy
cd deploy/kubernetes
ENV=nonprod \
  DOCKERHUB_USERNAME="$DOCKERHUB_USERNAME" \
  DOCKERHUB_PASSWORD="$DOCKERHUB_PASSWORD" \
  REGISTRY=dockerhub \
  TAG="$TAG" \
  ./scripts/setup-k8s.sh nonprod
```

**Note:** The images will be pushed as `your-dockerhub-username/logwise-orchestrator:1.0.0`, etc.

## Option 3: Use GitHub Container Registry (GHCR)

```bash
export REGISTRY=ghcr.io/your-org
export TAG=1.0.0
export GITHUB_TOKEN=your-github-token

# Login first
echo "$GITHUB_TOKEN" | docker login ghcr.io -u your-username --password-stdin

# Deploy
cd deploy/kubernetes
ENV=nonprod REGISTRY="$REGISTRY" TAG="$TAG" ./scripts/setup-k8s.sh nonprod
```

## Option 4: Use Local Docker (Only for kind clusters)

For local development with kind:

```bash
cd deploy/kubernetes
ENV=local CLUSTER_TYPE=kind ./scripts/setup-k8s.sh local
```

No registry needed - images are loaded directly into kind.

## Registry Formats

| Registry | REGISTRY Format | Example |
|----------|----------------|---------|
| AWS ECR | `ACCOUNT.dkr.ecr.REGION.amazonaws.com` | `954754807219.dkr.ecr.us-east-1.amazonaws.com` |
| Docker Hub | `dockerhub` (requires DOCKERHUB_USERNAME) | `dockerhub` |
| GHCR | `ghcr.io/org` | `ghcr.io/myorg` |
| GCR | `gcr.io/project-id` | `gcr.io/myproject` |
| Local | `local` or empty | (no registry) |

## Troubleshooting

### "push access denied" error

This usually means:
1. **Not authenticated**: Login to your registry first
2. **Wrong format**: Check the REGISTRY format above
3. **Missing permissions**: Ensure your credentials have push access

### For ECR:
```bash
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  954754807219.dkr.ecr.us-east-1.amazonaws.com
```

### For Docker Hub:
```bash
docker login
# Or with credentials:
echo "$DOCKERHUB_PASSWORD" | docker login --username "$DOCKERHUB_USERNAME" --password-stdin
```

### For GHCR:
```bash
echo "$GITHUB_TOKEN" | docker login ghcr.io -u your-username --password-stdin
```

## Recommended Setup for AWS EKS

For production AWS EKS deployments, use ECR:

```bash
#!/bin/bash
# deploy-to-eks.sh

export AWS_ACCOUNT_ID=954754807219
export AWS_REGION=us-east-1
export REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
export TAG="${TAG:-1.0.0}"
export ENV="${ENV:-nonprod}"

cd deploy/kubernetes
ENV="$ENV" REGISTRY="$REGISTRY" TAG="$TAG" ./scripts/setup-k8s.sh "$ENV"
```

