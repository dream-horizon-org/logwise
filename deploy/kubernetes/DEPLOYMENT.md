# Kubernetes Deployment Guide

Complete guide for deploying LogWise to Kubernetes.

## Prerequisites

Before deploying, ensure you have:

1. **Kubernetes cluster** (1.24+) accessible via `kubectl`
2. **kubectl** installed and configured
3. **Docker** installed (for building images)
4. **Container registry** access (for nonprod/prod)
5. **Secrets configured** (see Secrets section below)

## Quick Deployment

### Option 1: One-Command Setup (Local with kind)

For local development and testing:

```bash
cd deploy/kubernetes
./scripts/setup-k8s.sh local
```

This single command will:
- ✅ Install required tools (if missing)
- ✅ Create a kind cluster
- ✅ Build all Docker images
- ✅ Load images into kind
- ✅ Deploy all services
- ✅ Wait for pods to be ready

**Access services:**
- Orchestrator: http://localhost:30081
- Grafana: http://localhost:30080
- Spark Master: http://localhost:30082

### Option 2: Step-by-Step Deployment

For more control or for nonprod/prod environments:

## Step-by-Step Deployment

### Step 1: Prepare Configuration

#### For Local Development:
```bash
# Create .env file if you haven't already
cd deploy
./scripts/create-env.sh --docker

# Sync .env to Kubernetes ConfigMaps/Secrets
./shared/scripts/sync-config.sh docker/.env
```

#### For Production:
**DO NOT use .env files!** Use external secret management:
- AWS Secrets Manager
- HashiCorp Vault
- Sealed Secrets
- Or manually create Kubernetes Secrets

See `config/secrets.example.yaml` for examples.

### Step 2: Build and Push Images

#### Local (kind) - No registry needed:
```bash
cd deploy/kubernetes
ENV=local CLUSTER_TYPE=kind TAG=latest \
  ./scripts/build-and-push.sh
```

#### Non-Production:
```bash
cd deploy/kubernetes
ENV=nonprod \
  REGISTRY=ghcr.io/your-org \
  TAG=1.0.0 \
  ./scripts/build-and-push.sh
```

#### Production:
```bash
cd deploy/kubernetes
ENV=prod \
  REGISTRY=ghcr.io/your-org \
  TAG=1.0.0 \
  ./scripts/build-and-push.sh
```

**Environment Variables:**
- `REGISTRY`: Container registry (e.g., `ghcr.io/your-org`, `123456789012.dkr.ecr.us-east-1.amazonaws.com`)
- `TAG`: Image tag (default: `1.0.0`)
- `CLUSTER_TYPE`: `kind`, `docker-desktop`, or `other` (default: `docker-desktop`)
- `PARALLEL_BUILD`: `true` or `false` (default: `true`)

### Step 3: Deploy to Kubernetes

#### Local:
```bash
cd deploy/kubernetes
ENV=local ./scripts/deploy.sh
```

#### Non-Production:
```bash
cd deploy/kubernetes
ENV=nonprod ./scripts/deploy.sh
```

#### Production:
```bash
cd deploy/kubernetes
ENV=prod ./scripts/deploy.sh
```

**Environment Variables:**
- `ENV`: Environment (`local`, `nonprod`, `prod`)
- `NAMESPACE`: Kubernetes namespace (default: `logwise`)
- `DRY_RUN`: `true` to validate without deploying (default: `false`)
- `WAIT_TIMEOUT`: Timeout in seconds (default: `600`)

### Step 4: Verify Deployment

```bash
# Check all pods are running
kubectl get pods -n logwise

# Check services
kubectl get svc -n logwise

# Check logs
kubectl logs -n logwise deployment/orchestrator
kubectl logs -n logwise -l app=vector-logs

# Describe a pod if issues
kubectl describe pod -n logwise <pod-name>
```

## Complete Examples

### Example 1: Local Development (kind)

```bash
# From repository root
cd deploy/kubernetes

# One-command setup
./scripts/setup-k8s.sh local

# Or step-by-step:
# 1. Build images
ENV=local CLUSTER_TYPE=kind TAG=latest ./scripts/build-and-push.sh

# 2. Deploy
ENV=local ./scripts/deploy.sh

# 3. Access services
kubectl port-forward -n logwise svc/orchestrator 8080:8080
```

### Example 2: Non-Production Deployment

```bash
# From repository root
cd deploy/kubernetes

# 1. Build and push images
ENV=nonprod \
  REGISTRY=ghcr.io/myorg \
  TAG=v1.0.0 \
  ./scripts/build-and-push.sh

# 2. Deploy
ENV=nonprod ./scripts/deploy.sh

# 3. Verify
kubectl get pods -n logwise
kubectl get ingress -n logwise
```

### Example 3: Production Deployment

```bash
# From repository root
cd deploy/kubernetes

# 1. Validate first (dry-run)
ENV=prod DRY_RUN=true ./scripts/deploy.sh

# 2. Build and push images
ENV=prod \
  REGISTRY=ghcr.io/myorg \
  TAG=v1.0.0 \
  ./scripts/build-and-push.sh

# 3. Deploy
ENV=prod ./scripts/deploy.sh

# 4. Monitor deployment
kubectl rollout status deployment/orchestrator -n logwise
kubectl get pods -n logwise -w
```

## Accessing Services

### Port Forwarding (Local/Development)

```bash
# Orchestrator
kubectl port-forward -n logwise svc/orchestrator 8080:8080

# Grafana
kubectl port-forward -n logwise svc/grafana 3000:3000

# Spark Master UI
kubectl port-forward -n logwise svc/spark-master 8080:8080

# Vector API
kubectl port-forward -n logwise svc/vector-logs 8686:8686
```

### Ingress (Nonprod/Prod)

For nonprod/prod, configure Ingress in the overlay:

```bash
# Check ingress
kubectl get ingress -n logwise

# View ingress details
kubectl describe ingress -n logwise logwise-ingress
```

Access via the Ingress hostname configured in `overlays/nonprod/ingress.yaml` or `overlays/prod/ingress.yaml`.

## Secrets Management

### Local Development

Generate secrets from `.env`:

```bash
cd deploy
./shared/scripts/sync-config.sh docker/.env
```

This creates:
- `kubernetes/base/configmap-logwise-config.yaml`
- `kubernetes/base/secret-aws.yaml`

### Production

**Never use .env files in production!** Use one of:

1. **External Secrets Operator**:
   ```yaml
   # See config/secrets.example.yaml
   apiVersion: external-secrets.io/v1beta1
   kind: ExternalSecret
   ```

2. **Manual Secrets**:
   ```bash
   kubectl create secret generic aws-credentials \
     --from-literal=accessKeyId=xxx \
     --from-literal=secretAccessKey=xxx \
     -n logwise
   ```

3. **Sealed Secrets**:
   ```bash
   kubeseal -f secret.yaml -o sealed-secret.yaml
   ```

## Troubleshooting

### Pods not starting

```bash
# Check pod events
kubectl describe pod -n logwise <pod-name>

# Check logs
kubectl logs -n logwise <pod-name>
kubectl logs -n logwise <pod-name> --previous

# Check resource limits
kubectl describe pod -n logwise <pod-name> | grep -A 5 Resources
```

### Image pull errors

```bash
# Verify image exists
docker pull <registry>/logwise-orchestrator:<tag>

# Check image pull secrets
kubectl get secrets -n logwise

# For kind, load images manually
kind load docker-image <image> --name <cluster-name>
```

### Configuration issues

```bash
# Check ConfigMap
kubectl get configmap -n logwise logwise-config -o yaml

# Check Secrets
kubectl get secrets -n logwise

# Verify environment variables in pod
kubectl exec -n logwise <pod-name> -- env
```

## Rollback

If deployment fails:

```bash
# View rollout history
kubectl rollout history deployment/orchestrator -n logwise

# Rollback to previous version
kubectl rollout undo deployment/orchestrator -n logwise

# Rollback to specific revision
kubectl rollout undo deployment/orchestrator -n logwise --to-revision=2
```

## Cleanup

### Remove deployment

```bash
# Delete namespace (removes everything)
kubectl delete namespace logwise

# Or use destroy script
cd deploy/kubernetes
./scripts/destroy-k8s.sh nonprod
./scripts/destroy-k8s.sh prod
```

### Remove kind cluster (local)

```bash
cd deploy/kubernetes
./scripts/destroy-k8s.sh local
```

## Next Steps

After successful deployment:

1. ✅ Verify all pods are running: `kubectl get pods -n logwise`
2. ✅ Check service endpoints: `kubectl get svc -n logwise`
3. ✅ Access Grafana and configure dashboards
4. ✅ Test log ingestion via Vector
5. ✅ Monitor Spark jobs
6. ✅ Set up monitoring and alerting
7. ✅ Review [Production Checklist](../docs/production-checklist.md)

## Additional Resources

- [Kubernetes Setup Guide](../docs/kubernetes-setup.md) - Detailed setup instructions
- [Troubleshooting Guide](../docs/troubleshooting.md) - Common issues and solutions
- [Production Checklist](../docs/production-checklist.md) - Pre-deployment checklist

