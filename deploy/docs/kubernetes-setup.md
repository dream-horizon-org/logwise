# Kubernetes Deployment Guide

Complete guide for deploying LogWise to Kubernetes clusters.

## Prerequisites

Before deploying, ensure you have:

1. **Kubernetes cluster** (1.24+) accessible via `kubectl`
2. **kubectl** installed and configured
3. **kustomize** (included with kubectl 1.14+)
4. **Docker** installed (for building images)
5. **Container registry** access (for nonprod/prod)
6. **Secrets configured** (see Secrets Management section below)

## Quick Deployment

The recommended way to deploy LogWise is using the unified `setup-k8s.sh` script, which handles everything automatically.

### Local Development (kind)

For local development and testing:

**Step 1: Create environment and sync configuration**

```bash
# From repository root
cd deploy
./scripts/create-env.sh --kubernetes

# Edit deploy/kubernetes/.env with your configuration values

# Sync .env to Kubernetes ConfigMaps/Secrets
./kubernetes/scripts/sync-config.sh kubernetes/.env
```

**Step 2: Deploy to Kubernetes**

```bash
cd deploy/kubernetes
./scripts/setup-k8s.sh local
```

This will:
- ✅ Install required tools (if missing)
- ✅ Create a kind cluster (using common `setup-kind-cluster.sh` script)
- ✅ Build all Docker images locally
- ✅ **Load images directly into kind cluster** (no registry needed)
- ✅ Deploy all services
- ✅ Wait for pods to be ready

**Important:** For kind clusters, images are **loaded** directly into the cluster (not pulled from a registry). This is handled automatically by `setup-kind-cluster.sh`.

**Note:** The kind cluster setup uses a common script (`scripts/setup-kind-cluster.sh`) that works for both Kustomize and Helm deployments, ensuring consistency across deployment methods.

**Access services:**
- Orchestrator: http://localhost:30081
- Grafana: http://localhost:30080
- Spark Master: http://localhost:30082
- Spark Worker: http://localhost:30083
- Vector OTLP: http://localhost:30418

### Non-Production Environment

**Step 1: Create environment and sync configuration**

```bash
# From repository root
cd deploy
./scripts/create-env.sh --kubernetes

# Edit deploy/kubernetes/.env with your configuration values

# Sync .env to Kubernetes ConfigMaps/Secrets
./kubernetes/scripts/sync-config.sh kubernetes/.env
```

**Step 2: Deploy to Kubernetes**

```bash
cd deploy/kubernetes
ENV=nonprod \
  REGISTRY=ghcr.io/your-org \
  TAG=1.0.0 \
  ./scripts/setup-k8s.sh nonprod
```

This will:
- ✅ **Build all Docker images** locally
- ✅ **Push images to the specified registry** (ECR, Docker Hub, GHCR, etc.)
- ✅ Configure image pull secrets (if needed for private registry)
- ✅ Deploy all services to your Kubernetes cluster
- ✅ **Cluster pulls images from registry** during deployment
- ✅ Wait for pods to be ready

**Important:** For nonprod/prod, images must be:
1. **Pushed to a registry** (handled by `build-and-push.sh`)
2. **Pulled by the cluster** (requires registry authentication via image pull secrets)

**Prerequisites:**
- Registry access and authentication configured
- See [Image Management Guide](../kubernetes/IMAGE_MANAGEMENT.md) for registry-specific setup

### Production Environment

```bash
cd deploy/kubernetes
ENV=prod \
  REGISTRY=ghcr.io/your-org \
  TAG=1.0.0 \
  ./scripts/setup-k8s.sh prod
```

**Environment Variables:**
- `ENV`: Environment (`local`, `nonprod`, `prod`) - **required**
- `REGISTRY`: Container registry (e.g., `ghcr.io/your-org`, `123456789012.dkr.ecr.us-east-1.amazonaws.com`) - **required for nonprod/prod**
- `TAG`: Image tag (default: `latest` for local, should use version tags for nonprod/prod)
- `CLUSTER_TYPE`: `kind`, `docker-desktop`, or `other` (default: `docker-desktop`)

**Image Handling:**
- **Local (kind)**: Images are built and **loaded** directly into kind (no registry)
- **Nonprod/Prod**: Images are built, **pushed** to registry, and **pulled** by cluster
- See [Image Management Guide](../kubernetes/IMAGE_MANAGEMENT.md) for detailed information

## Directory Structure

```
kubernetes/
├── base/              # Base Kubernetes manifests
├── overlays/          # Environment-specific overlays
│   ├── local/        # Local development
│   ├── nonprod/      # Non-production
│   └── prod/         # Production
├── scripts/          # Deployment scripts
│   └── setup-kind-cluster.sh  # Common kind cluster setup (Kustomize & Helm)
├── helm/             # Helm chart (alternative deployment method)
│   └── logwise/
│       └── setup-kind-cluster.sh  # Helm wrapper (calls common script)
└── config/           # Configuration files
```

## Image Management Overview

LogWise uses different image handling strategies depending on the environment:

| Environment | Image Handling | Registry Required |
|------------|----------------|-------------------|
| **Local (kind)** | Build → **Load into kind** | ❌ No |
| **Nonprod/Prod** | Build → **Push to registry** → **Cluster pulls** | ✅ Yes |

### Key Differences

**Local (kind):**
- Images are built locally and loaded directly into kind cluster
- No registry authentication needed
- Faster iteration for development
- Handled automatically by `setup-kind-cluster.sh`

**Nonprod/Prod:**
- Images must be pushed to a container registry
- Registry authentication required (via image pull secrets)
- Cluster pulls images from registry during deployment
- Standard Kubernetes workflow

**For detailed information, see:** [Image Management Guide](../kubernetes/IMAGE_MANAGEMENT.md)

## Common Kind Cluster Setup Script

For local development, both **Kustomize** and **Helm** deployments use the same common script to set up kind clusters:

### Using the Common Script

**For Kustomize deployments:**
```bash
cd deploy/kubernetes
./scripts/setup-kind-cluster.sh
```

**For Helm deployments:**
```bash
# Option 1: Use Helm wrapper (recommended)
cd deploy/kubernetes/helm/logwise
./setup-kind-cluster.sh

# Option 2: Use common script directly
cd deploy/kubernetes
KIND_CLUSTER_NAME=logwise ./scripts/setup-kind-cluster.sh
```

**Features:**
- Creates kind cluster with port mappings
- Builds Docker images (orchestrator, spark, vector, healthcheck)
- Loads images into kind cluster
- Works for both Kustomize and Helm
- Configurable via command-line options and environment variables

**Options:**
```bash
./scripts/setup-kind-cluster.sh [OPTIONS]

Options:
  -n, --name NAME          Cluster name (default: logwise-local)
  -c, --config PATH        Path to kind config file
  --no-build              Skip building images (only load existing ones)
  --no-load               Skip loading images into kind
  --skip-existing         Skip if cluster already exists (don't prompt)
  -h, --help              Show help message
```

**Note:** The `setup-k8s.sh` script automatically calls `setup-kind-cluster.sh` when deploying to local, so you typically don't need to run it manually. See `kubernetes/COMMON_SETUP_SCRIPT.md` for detailed documentation.

## Advanced: Manual Step-by-Step Deployment

> **Note:** For most use cases, the `setup-k8s.sh` script (shown above) is recommended as it handles all steps automatically. Use the manual approach below only if you need more control or are integrating into custom CI/CD pipelines.

### Step 1: Prepare Configuration

#### For Local Development:

```bash
# Create .env file if you haven't already
cd deploy
./scripts/create-env.sh --kubernetes

# Sync .env to Kubernetes ConfigMaps/Secrets
./kubernetes/scripts/sync-config.sh kubernetes/.env
```

This creates:
- `kubernetes/base/configmap-logwise-config.yaml`
- `kubernetes/base/secret-aws.yaml`

#### For Production:

**DO NOT use .env files!** Use external secret management:
- AWS Secrets Manager
- HashiCorp Vault
- Sealed Secrets
- Or manually create Kubernetes Secrets

See `kubernetes/config/secrets.example.yaml` for examples.

### Step 2: Build and Push Images

#### Local (kind) - Images Loaded Directly:

```bash
cd deploy/kubernetes
ENV=local CLUSTER_TYPE=kind TAG=latest \
  ./scripts/build-and-push.sh
```

**What happens:**
- ✅ Builds all Docker images locally
- ✅ **Loads images directly into kind cluster** (no registry push)
- ❌ Does NOT push to registry (not needed for kind)

**Verification:**
```bash
# Check images are loaded in kind
docker exec -it logwise-local-control-plane crictl images | grep logwise
```

#### Non-Production - Push to Registry:

**Step 2a: Authenticate with Registry**

```bash
# For ECR
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com

# For Docker Hub
docker login --username your-username

# For GHCR
echo "$GITHUB_TOKEN" | docker login ghcr.io -u your-username --password-stdin
```

**Step 2b: Build and Push Images**

```bash
cd deploy/kubernetes
ENV=nonprod \
  REGISTRY=ghcr.io/your-org \
  TAG=1.0.0 \
  ./scripts/build-and-push.sh
```

**What happens:**
- ✅ Builds all Docker images with registry prefix
- ✅ **Pushes images to registry** (e.g., `ghcr.io/your-org/logwise-orchestrator:1.0.0`)
- ✅ Images are now available for cluster to pull

**Verification:**
```bash
# Verify images are in registry
docker pull ghcr.io/your-org/logwise-orchestrator:1.0.0
```

#### Production - Push to Registry:

**Step 2a: Authenticate with Registry** (same as nonprod)

**Step 2b: Build and Push with Version Tag**

```bash
cd deploy/kubernetes
ENV=prod \
  REGISTRY=ghcr.io/your-org \
  TAG=v1.2.3 \
  ./scripts/build-and-push.sh
```

**What happens:**
- ✅ Builds all Docker images with registry prefix
- ✅ **Pushes images to registry** with version tag
- ✅ Use semantic versioning (never use `latest` in production)

**Environment Variables:**
- `REGISTRY`: Container registry (e.g., `ghcr.io/your-org`, `123456789012.dkr.ecr.us-east-1.amazonaws.com`) - **required for nonprod/prod**
- `TAG`: Image tag (default: `1.0.0`, use semantic versioning for prod)
- `CLUSTER_TYPE`: `kind`, `docker-desktop`, or `other` (default: `docker-desktop`)
- `PARALLEL_BUILD`: `true` or `false` (default: `true`)

**Important Notes:**
- **Local (kind)**: Images are loaded directly, no registry needed
- **Nonprod/Prod**: Images must be pushed to registry and cluster will pull them
- **Registry Authentication**: Required before pushing (see examples above)
- **Image Pull Secrets**: Configured automatically by `deploy.sh` for Docker Hub, or manually for other registries

**For detailed registry setup, see:** [Image Management Guide](../kubernetes/IMAGE_MANAGEMENT.md) and [Registry Setup Guide](../kubernetes/REGISTRY_SETUP.md)

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

**Recommended approach:**
```bash
# From repository root
# Step 1: Create environment and sync configuration
cd deploy
./scripts/create-env.sh --kubernetes
# Edit deploy/kubernetes/.env with your configuration values
./kubernetes/scripts/sync-config.sh kubernetes/.env

# Step 2: Deploy to Kubernetes
cd kubernetes
./scripts/setup-k8s.sh local

# Access services
kubectl port-forward -n logwise svc/orchestrator 8080:8080
```

**Alternative (manual steps):**
```bash
# 1. Create environment and sync configuration
cd deploy
./scripts/create-env.sh --kubernetes
# Edit deploy/kubernetes/.env with your configuration values
./kubernetes/scripts/sync-config.sh kubernetes/.env

# 2. Set up kind cluster (optional - setup-k8s.sh does this automatically)
cd kubernetes
./scripts/setup-kind-cluster.sh

# 3. Build images (if not already built by setup-kind-cluster.sh)
ENV=local CLUSTER_TYPE=kind TAG=latest ./scripts/build-and-push.sh

# 4. Deploy
ENV=local ./scripts/deploy.sh
```

**Note:** The `setup-kind-cluster.sh` script builds and loads images automatically, so step 3 may be skipped if you ran the setup script.

### Example 2: Non-Production Deployment

**Recommended approach:**
```bash
# From repository root
# Step 1: Create environment and sync configuration
cd deploy
./scripts/create-env.sh --kubernetes
# Edit deploy/kubernetes/.env with your configuration values
./kubernetes/scripts/sync-config.sh kubernetes/.env

# Step 2: Deploy to Kubernetes
cd kubernetes
ENV=nonprod \
  REGISTRY=ghcr.io/myorg \
  TAG=v1.0.0 \
  ./scripts/setup-k8s.sh nonprod

# Verify
kubectl get pods -n logwise
kubectl get ingress -n logwise
```

**Alternative (manual steps):**
```bash
# 1. Create environment and sync configuration
cd deploy
./scripts/create-env.sh --kubernetes
# Edit deploy/kubernetes/.env with your configuration values
./kubernetes/scripts/sync-config.sh kubernetes/.env

# 2. Build and push images
cd kubernetes
ENV=nonprod \
  REGISTRY=ghcr.io/myorg \
  TAG=v1.0.0 \
  ./scripts/build-and-push.sh

# 3. Deploy
ENV=nonprod ./scripts/deploy.sh
```

### Example 3: Production Deployment

**Recommended approach:**
```bash
# From repository root
cd deploy/kubernetes

# One-command deployment
ENV=prod \
  REGISTRY=ghcr.io/myorg \
  TAG=v1.0.0 \
  ./scripts/setup-k8s.sh prod

# Monitor deployment
kubectl rollout status deployment/orchestrator -n logwise
kubectl get pods -n logwise -w
```

**Alternative (with validation first):**
```bash
# 1. Validate first (dry-run)
ENV=prod DRY_RUN=true ./scripts/deploy.sh

# 2. Full deployment
ENV=prod \
  REGISTRY=ghcr.io/myorg \
  TAG=v1.0.0 \
  ./scripts/setup-k8s.sh prod
```

## Configuration

### Environment Variables

Configuration is managed via ConfigMaps and Secrets. See `shared/config/` for default values.

### Environment Files

**Location:**
```
deploy/kubernetes/.env
```

**Setup:**
1. Create Kubernetes-specific `.env` file:
   ```bash
   # Create Kubernetes-specific env file
   cp deploy/shared/templates/env.template deploy/kubernetes/.env
   ```

2. Edit `.env` with your Kubernetes-specific values:
   ```bash
   # Required AWS credentials
   AWS_ACCESS_KEY_ID=your-access-key
   AWS_SECRET_ACCESS_KEY=your-secret-key
   AWS_REGION=us-east-1
   
   # Required S3/Athena configuration
   S3_BUCKET_NAME=your-bucket-name
   S3_ATHENA_OUTPUT=s3://your-bucket/athena-output/
   ATHENA_WORKGROUP=logwise
   ATHENA_DATABASE=logwise
   ```

3. Sync to Kubernetes manifests:
   ```bash
   # From repository root
   ./deploy/kubernetes/scripts/sync-config.sh deploy/kubernetes/.env
   
   # Or from kubernetes directory
   cd deploy/kubernetes
   ./scripts/sync-config.sh .env
   ```

**Security Notes:**
- Never commit `.env` files to version control
- Add to `.gitignore`: `deploy/kubernetes/.env`
- For production, use external secret management (see Secrets Management section)

### Image Registry

Configure image registry in `shared/config/image-registry.yaml` or via environment:

```bash
export REGISTRY=ghcr.io/your-org
export TAG=1.0.0
```

## Secrets Management

### Local Development

Generate secrets from Kubernetes `.env` file:

```bash
# From repository root
./deploy/kubernetes/scripts/sync-config.sh deploy/kubernetes/.env

# Or from kubernetes directory
cd deploy/kubernetes
./scripts/sync-config.sh .env
```

This creates:
- `kubernetes/base/configmap-logwise-config.yaml`
- `kubernetes/base/secret-aws.yaml`

**Warning**: The generated secrets are base64 encoded but NOT encrypted. For production, use proper secret management.

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

4. **HashiCorp Vault**: Use Vault operator or CSI driver

5. **AWS Secrets Manager**: Use External Secrets Operator

## Environment Overlays

### Local

Minimal configuration for local development with kind:

```bash
ENV=local ./kubernetes/scripts/setup-k8s.sh local
```

Features:
- NodePort services for external access
- Local image loading (no registry required)
- Single replica deployments

### Non-Production

Standard configuration for testing:

```bash
ENV=nonprod ./kubernetes/scripts/deploy.sh
```

Features:
- Standard resource limits
- Ingress configuration (if available)
- Single replica deployments

### Production

Production-ready configuration:

```bash
ENV=prod ./kubernetes/scripts/deploy.sh
```

Features:
- High availability (multiple replicas)
- Pod disruption budgets
- Resource limits and requests
- Anti-affinity rules
- Production-grade monitoring

See `overlays/prod/README.md` for details.

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

Example Ingress configuration:

```yaml
# overlays/nonprod/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: logwise-ingress
spec:
  rules:
    - host: logwise.example.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: orchestrator
                port:
                  number: 8080
```

Access via the Ingress hostname configured in `overlays/nonprod/ingress.yaml` or `overlays/prod/ingress.yaml`.

## Monitoring

### Health Checks

All services include liveness and readiness probes:

```bash
kubectl describe pod -n logwise <pod-name>
```

### Logs

```bash
# View logs
kubectl logs -n logwise deployment/orchestrator
kubectl logs -n logwise -l app=vector-logs

# Follow logs
kubectl logs -n logwise -f deployment/orchestrator

# View previous container logs
kubectl logs -n logwise <pod-name> --previous
```

### Metrics

If Prometheus is installed, ServiceMonitors can be added. See production overlay for examples.

## Scaling

### Horizontal Pod Autoscaling

Vector includes HPA configuration. Adjust in `base/vector/vector-hpa.yaml`:

```yaml
spec:
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

### Manual Scaling

```bash
kubectl scale deployment/orchestrator -n logwise --replicas=3
```

## Troubleshooting

### Pods in CrashLoopBackOff

**Symptoms**: Pods restart repeatedly

**Solutions**:
1. Check pod events:
   ```bash
   kubectl describe pod -n logwise <pod-name>
   ```

2. Check logs:
   ```bash
   kubectl logs -n logwise <pod-name>
   kubectl logs -n logwise <pod-name> --previous
   ```

3. Verify image exists and is accessible

4. Check resource limits:
   ```bash
   kubectl describe pod -n logwise <pod-name> | grep -A 5 Resources
   ```

5. Verify secrets and configmaps:
   ```bash
   kubectl get secrets -n logwise
   kubectl get configmaps -n logwise
   ```

### Image Pull Errors

**Symptoms**: `ErrImagePull` or `ImagePullBackOff`

**Solutions by Environment:**

#### For Local (kind) Clusters:

1. **Verify images are loaded in kind:**
   ```bash
   docker exec -it logwise-local-control-plane crictl images | grep logwise
   ```

2. **Load images if missing:**
   ```bash
   # Use the common setup script (recommended)
   cd deploy/kubernetes
   ./scripts/setup-kind-cluster.sh
   
   # Or manually load specific images
   kind load docker-image logwise-orchestrator:latest --name logwise-local
   ```

3. **Verify imagePullPolicy is IfNotPresent:**
   ```bash
   kubectl get deployment orchestrator -n logwise -o yaml | grep imagePullPolicy
   ```

#### For Nonprod/Prod Clusters:

1. **Verify image exists in registry:**
   ```bash
   docker pull $REGISTRY/logwise-orchestrator:$TAG
   ```

2. **Check image pull secret exists:**
   ```bash
   kubectl get secrets -n logwise | grep -E 'dockerhub|ecr|ghcr'
   ```

3. **Verify registry authentication:**
   ```bash
   # Re-authenticate if needed
   # For ECR:
   aws ecr get-login-password --region us-east-1 | \
     docker login --username AWS --password-stdin $REGISTRY
   
   # For Docker Hub:
   docker login
   
   # For GHCR:
   echo $GITHUB_TOKEN | docker login ghcr.io -u username --password-stdin
   ```

4. **Update image pull secret if needed:**
   ```bash
   # Delete old secret
   kubectl delete secret dockerhub-secret -n logwise
   
   # Create new secret (example for Docker Hub)
   kubectl create secret docker-registry dockerhub-secret \
     --docker-server=docker.io \
     --docker-username=your-username \
     --docker-password=your-password \
     --docker-email=your-email@example.com \
     -n logwise
   ```

5. **Verify imagePullPolicy is Always (for nonprod/prod):**
   ```bash
   kubectl get deployment orchestrator -n logwise -o yaml | grep imagePullPolicy
   # Should show: imagePullPolicy: Always
   ```

6. **Check pod events for detailed error:**
   ```bash
   kubectl describe pod -n logwise <pod-name> | grep -A 10 Events
   ```

7. **Test registry connectivity from cluster:**
   ```bash
   # Run a test pod to check registry access
   kubectl run -it --rm debug --image=busybox --restart=Never -- \
     sh -c "wget -O- https://$REGISTRY"
   ```

**For detailed troubleshooting, see:** [Image Management Guide](../kubernetes/IMAGE_MANAGEMENT.md)

### Services Not Accessible

**Symptoms**: Can't access services via port-forward or ingress

**Solutions**:
1. Check service exists:
   ```bash
   kubectl get svc -n logwise
   ```

2. Verify service selector matches pod labels:
   ```bash
   kubectl describe svc -n logwise <service-name>
   kubectl get pods -n logwise --show-labels
   ```

3. Check endpoints:
   ```bash
   kubectl get endpoints -n logwise <service-name>
   ```

4. Test port-forward:
   ```bash
   kubectl port-forward -n logwise svc/<service-name> 8080:8080
   ```

5. For ingress, check ingress controller:
   ```bash
   kubectl get ingress -n logwise
   kubectl describe ingress -n logwise <ingress-name>
   ```

### Configuration Issues

**Symptoms**: Services using wrong configuration

**Solutions**:
1. Verify ConfigMap:
   ```bash
   kubectl get configmap -n logwise logwise-config -o yaml
   ```

2. Check environment variables in pod:
   ```bash
   kubectl exec -n logwise <pod-name> -- env | grep -i kafka
   ```

3. Verify secrets:
   ```bash
   kubectl get secret -n logwise -o yaml
   ```

4. Restart deployment to pick up changes:
   ```bash
   kubectl rollout restart deployment/<deployment-name> -n logwise
   ```

### Resource Constraints

**Symptoms**: Pods pending or being evicted

**Solutions**:
1. Check node resources:
   ```bash
   kubectl top nodes
   kubectl describe nodes
   ```

2. Check pod resource requests:
   ```bash
   kubectl describe pod -n logwise <pod-name> | grep -A 5 Requests
   ```

3. Adjust resource limits in deployment

4. Scale down other workloads if needed

5. Add more nodes to cluster

### Logs Not Appearing

**Symptoms**: No logs in Grafana or S3

**Solutions**:
1. Verify Vector is running:
   ```bash
   kubectl get pods -n logwise -l app=vector-logs
   ```

2. Check Vector logs:
   ```bash
   kubectl logs -n logwise -l app=vector-logs
   ```

3. Verify Kafka connectivity:
   ```bash
   kubectl exec -n logwise <vector-pod> -- \
     nc -zv kafka-bootstrap 9092
   ```

4. Check Vector configuration

5. Verify OTLP endpoint is accessible

### Spark Jobs Failing

**Symptoms**: Spark jobs not completing or failing

**Solutions**:
1. Check Spark master logs:
   ```bash
   kubectl logs -n logwise deployment/spark-master
   ```

2. Check Spark worker logs:
   ```bash
   kubectl logs -n logwise -l app=spark-worker
   ```

3. Verify Spark UI:
   ```bash
   kubectl port-forward -n logwise svc/spark-master 8080:8080
   ```

4. Check resource limits for Spark

5. Verify S3 access from Spark

### Orchestrator Errors

**Symptoms**: Orchestrator returning errors

**Solutions**:
1. Check orchestrator logs:
   ```bash
   kubectl logs -n logwise deployment/orchestrator
   ```

2. Verify health endpoint:
   ```bash
   kubectl exec -n logwise <orchestrator-pod> -- \
     curl localhost:8080/healthcheck
   ```

3. Check database connectivity

4. Verify Kafka connectivity

5. Check Spark master connectivity

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

### Remove Deployment

```bash
# Delete namespace (removes everything)
kubectl delete namespace logwise

# Or use destroy script
cd deploy/kubernetes
./scripts/destroy-k8s.sh nonprod
./scripts/destroy-k8s.sh prod
```

### Remove kind Cluster (Local)

```bash
# Option 1: Use destroy script
cd deploy/kubernetes
./scripts/destroy-k8s.sh local

# Option 2: Delete kind cluster directly
kind delete cluster --name logwise-local  # For Kustomize
kind delete cluster --name logwise        # For Helm
```

## Production Checklist

Before deploying to production, ensure:

### Infrastructure
- [ ] Kubernetes cluster is provisioned and accessible
- [ ] Cluster has sufficient resources (CPU, memory, storage)
- [ ] Network policies are configured (if required)
- [ ] Ingress controller is installed and configured
- [ ] TLS certificates are provisioned for Ingress
- [ ] DNS records are configured and pointing to Ingress

### Container Registry
- [ ] Container registry is accessible from cluster
- [ ] Registry credentials are configured in cluster
- [ ] Image pull secrets are created (if required)
- [ ] Images are built and pushed to registry
- [ ] Image tags follow semantic versioning

### Secrets Management
- [ ] Secrets are NOT stored in version control
- [ ] Secrets are managed via external secret manager (AWS Secrets Manager, Vault, etc.)
- [ ] Secret rotation policy is defined
- [ ] Access to secrets is restricted via IAM/RBAC
- [ ] Secrets are encrypted at rest

### Configuration
- [ ] Environment variables are reviewed and set
- [ ] Resource limits are appropriate for workload
- [ ] ConfigMaps are created and validated
- [ ] Production overlay is reviewed
- [ ] Kafka configuration is correct (managed or in-cluster)
- [ ] Database configuration is correct
- [ ] AWS credentials have appropriate permissions

### High Availability
- [ ] Multiple replicas are configured for stateless services
- [ ] Pod disruption budgets are set
- [ ] Anti-affinity rules are configured
- [ ] Services are distributed across nodes/zones

### Resource Management
- [ ] Resource requests and limits are set for all containers
- [ ] Resource quotas are configured (if required)
- [ ] Limit ranges are set (if required)
- [ ] Resource usage is monitored

### Networking
- [ ] Services are properly exposed (ClusterIP, NodePort, or Ingress)
- [ ] Network policies are configured (if required)
- [ ] Firewall rules allow required traffic
- [ ] Load balancer is configured (if using)

### Storage
- [ ] Persistent volumes are configured for stateful services
- [ ] Storage classes are appropriate
- [ ] Backup procedures are in place
- [ ] Storage is encrypted at rest

### Monitoring
- [ ] Monitoring stack is installed (Prometheus, Grafana, etc.)
- [ ] ServiceMonitors/PodMonitors are configured
- [ ] Alerts are configured for critical metrics
- [ ] Log aggregation is set up
- [ ] Dashboards are created and accessible

### Health Checks
- [ ] Liveness probes are configured and working
- [ ] Readiness probes are configured and working
- [ ] Startup probes are configured (if needed)
- [ ] Health check endpoints are accessible

### Security
- [ ] RBAC policies are configured
- [ ] Service accounts have minimal required permissions
- [ ] Network policies restrict traffic (if required)
- [ ] Pod security policies are set (if required)
- [ ] Images are scanned for vulnerabilities
- [ ] TLS is enabled for all external traffic

### Backup and Recovery
- [ ] Backup procedures are documented
- [ ] Backup schedule is configured
- [ ] Recovery procedures are tested
- [ ] Disaster recovery plan is documented
- [ ] RTO/RPO targets are defined

## Useful Commands

### General

```bash
# Get all resources
kubectl get all -n logwise

# Describe resource
kubectl describe <resource> -n logwise <name>

# View logs
kubectl logs -n logwise <pod-name>
kubectl logs -n logwise -l app=<app-label>

# Execute command in pod
kubectl exec -n logwise <pod-name> -- <command>

# Port forward
kubectl port-forward -n logwise svc/<service-name> <local-port>:<remote-port>

# Restart deployment
kubectl rollout restart deployment/<name> -n logwise

# View rollout history
kubectl rollout history deployment/<name> -n logwise

# Rollback
kubectl rollout undo deployment/<name> -n logwise
```

## Deployment Methods: Kustomize vs Helm

LogWise supports two deployment methods:

### Kustomize (Recommended for GitOps)

- Uses base manifests with environment overlays
- Better for GitOps workflows
- More declarative and version-controlled
- Default method used by `setup-k8s.sh`

**Quick start:**
```bash
cd deploy/kubernetes
./scripts/setup-k8s.sh local
```

### Helm (Alternative)

- Uses Helm charts with values files
- Better for complex templating needs
- More familiar to Helm users

**Quick start:**
```bash
# Set up kind cluster (uses common script)
cd deploy/kubernetes/helm/logwise
./setup-kind-cluster.sh

# Install Helm chart
./quick-install.sh [AWS_ACCESS_KEY] [AWS_SECRET] [S3_BUCKET] [S3_ATHENA_OUTPUT]
```

**Note:** Both methods use the same common `setup-kind-cluster.sh` script for local kind cluster setup, ensuring consistency. See `kubernetes/KUBERNETES_ARCHITECTURE.md` for detailed comparison.

## Next Steps

After successful deployment:

1. ✅ Verify all pods are running: `kubectl get pods -n logwise`
2. ✅ Check service endpoints: `kubectl get svc -n logwise`
3. ✅ Access Grafana and configure dashboards
4. ✅ Test log ingestion via Vector
5. ✅ Monitor Spark jobs
6. ✅ Set up monitoring and alerting
7. ✅ Review the Production Checklist section above before deploying to production

## Additional Resources

- **Image Management Guide**: `kubernetes/IMAGE_MANAGEMENT.md` - Complete guide for image handling across environments
- **Architecture Overview**: `kubernetes/KUBERNETES_ARCHITECTURE.md` - System architecture and components
- **Common Setup Script**: `kubernetes/COMMON_SETUP_SCRIPT.md` - Kind cluster setup for both Kustomize and Helm
- **Registry Setup**: `kubernetes/REGISTRY_SETUP.md` - Container registry configuration
- **Metrics Server Testing**: `kubernetes/TESTING_METRICS_SERVER.md` - Metrics Server deployment and testing
- **Documentation Improvement Plan**: `kubernetes/DOCUMENTATION_IMPROVEMENT_PLAN.md` - Planned documentation enhancements
