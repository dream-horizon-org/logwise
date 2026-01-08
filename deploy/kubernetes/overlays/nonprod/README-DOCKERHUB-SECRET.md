# Docker Hub Image Pull Secret

To fix image pull errors (429 rate limiting), you need to create a Docker Hub secret for authenticated image pulls.

## Quick Fix

Run this command to create the secret:

```bash
kubectl create secret docker-registry dockerhub-secret \
  --docker-server=docker.io \
  --docker-username=varunwalia \
  --docker-password=YOUR_DOCKERHUB_PASSWORD \
  --docker-email=YOUR_EMAIL \
  -n logwise
```

**Or use a Docker Hub Access Token (recommended):**

1. Go to https://hub.docker.com/settings/security
2. Create a new access token
3. Use the token as the password:

```bash
kubectl create secret docker-registry dockerhub-secret \
  --docker-server=docker.io \
  --docker-username=varunwalia \
  --docker-password=YOUR_ACCESS_TOKEN \
  --docker-email=varunwalia@example.com \
  -n logwise
```

## Automatic Creation

The deployment script will automatically create this secret if you set:
- `DOCKERHUB_USERNAME=varunwalia`
- `DOCKERHUB_PASSWORD=your_password_or_token`
- `REGISTRY=dockerhub`

Example:
```bash
ENV=nonprod \
  DOCKERHUB_USERNAME=varunwalia \
  DOCKERHUB_PASSWORD=your_token \
  REGISTRY=dockerhub \
  TAG=1.0.0 \
  ./deploy/kubernetes/scripts/deploy.sh
```

## Verify Secret

```bash
kubectl get secret dockerhub-secret -n logwise
```

## After Creating Secret

The deployments are already configured to use this secret. Once created, the pods should automatically retry pulling images with authentication, avoiding rate limits.


