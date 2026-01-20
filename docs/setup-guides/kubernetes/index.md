---
title: Kubernetes Deployment Guide
---

# Kubernetes Deployment Guide

This guide provides comprehensive instructions for deploying Logwise on Kubernetes using either **Kustomize** or **Helm Charts**. Both methods are fully supported and provide different benefits depending on your deployment preferences.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Choosing a Deployment Method](#choosing-a-deployment-method)
- [Kustomize Deployment](#kustomize-deployment)
- [Helm Chart Deployment](#helm-chart-deployment)
- [Configuration](#configuration)
- [Accessing Services](#accessing-services)
- [Upgrading](#upgrading)
- [Troubleshooting](#troubleshooting)
- [Production Considerations](#production-considerations)

## Prerequisites

Before deploying Logwise on Kubernetes, ensure you have:

1. **Kubernetes Cluster**: A running Kubernetes cluster (version 1.20+)
   - For local development: [kind](https://kind.sigs.k8s.io/), [minikube](https://minikube.sigs.k8s.io/), or Docker Desktop Kubernetes
   - For production: EKS, GKE, AKS, or any managed Kubernetes service

2. **kubectl**: Configured to access your cluster
   ```bash
   kubectl cluster-info
   ```

3. **kustomize** (for Kustomize method): Version 3.5.3+ or use `kubectl` (which includes kustomize)
   ```bash
   kubectl version --client
   # or
   kustomize version
   ```

4. **Helm** (for Helm method): Version 3.8+
   ```bash
   helm version
   ```

5. **AWS Credentials** (optional): If using S3/Athena for log storage
   - AWS Access Key ID
   - AWS Secret Access Key
   - S3 Bucket Name
   - S3 Athena Output location

6. **Docker** (required for non-prod/prod): For building and pushing images to container registry
   - Docker Engine or Docker Desktop installed
   - Authenticated access to your container registry (ECR, Docker Hub, GHCR, GCR, etc.)
   - For local development with kind: Docker is required to build and load images

7. **Container Registry Access** (required for non-prod/prod): Access to push images
   - **Non-production/Production**: Must build and push images to registry before deployment
   - **Local development**: Can use local images or load directly into kind (no registry needed)
   - Supported registries: AWS ECR, Docker Hub, GitHub Container Registry (GHCR), Google Container Registry (GCR), Azure Container Registry (ACR)

8. **Metrics Server** (required for HPA): Automatically included in both Kustomize and Helm deployments
   - Provides CPU and memory metrics for Horizontal Pod Autoscaler (HPA)
   - Deployed to `kube-system` namespace
   - For kind/local clusters: Automatically configured with `--kubelet-insecure-tls` flag
   - For managed clusters (EKS, GKE, AKS): May already be installed; our deployment is idempotent

## Choosing a Deployment Method

### Kustomize

**Best for:**
- Teams familiar with native Kubernetes manifests
- GitOps workflows (ArgoCD, Flux)
- Environments requiring fine-grained control over manifests
- Organizations preferring declarative, template-free configuration

**Advantages:**
- Native Kubernetes tooling (built into kubectl)
- No additional dependencies
- Easy to version control and review changes
- Flexible patching and overlays for different environments

### Helm Charts

**Best for:**
- Teams familiar with Helm ecosystem
- Environments requiring parameterized deployments
- CI/CD pipelines using Helm
- Quick deployments with sensible defaults

**Advantages:**
- Rich templating capabilities
- Easy value overrides via values files
- Built-in upgrade/rollback mechanisms
- Large ecosystem and community support

---

## Kustomize Deployment

Kustomize uses a base configuration with environment-specific overlays, plus helper scripts to make local/nonprod/prod deployments consistent and repeatable.

### Directory Structure

```
deploy/kubernetes/
├── base/                    # Base manifests for all components
│   ├── kustomization.yaml
│   ├── orchestrator/
│   ├── spark/
│   ├── kafka/
│   ├── vector/
│   ├── grafana/
│   └── ...
├── overlays/                # Environment-specific overlays
│   ├── local/              # Local development
│   ├── nonprod/            # Non-production
│   └── prod/               # Production
├── scripts/                 # Deployment automation (recommended)
│   ├── setup-k8s.sh         # End-to-end deployment (local/nonprod/prod)
│   ├── setup-kind-cluster.sh# Common kind cluster setup (Kustomize & Helm)
│   ├── sync-config.sh       # Sync .env -> ConfigMaps/Secrets manifests
│   ├── build-and-push.sh    # Build images (and push/load depending on env)
│   ├── deploy.sh            # Apply manifests + wait/validate
│   └── destroy-k8s.sh       # Teardown
└── config/                  # Configuration templates (examples)
    └── secrets.example.yaml
```

### Quick Deployment (Recommended)

The recommended workflow is:
- Create a Kubernetes-specific `.env`
- Sync it into generated Kubernetes manifests (ConfigMap/Secret YAMLs in `deploy/kubernetes/base/`)
- Run a single setup script per environment

#### Local Development (kind)

**Step 1: Create environment and sync configuration**

```bash
# From repository root
./deploy/scripts/create-env.sh --kubernetes

# Edit deploy/kubernetes/.env with your configuration values

# Sync .env to Kubernetes ConfigMaps/Secrets manifests
./deploy/kubernetes/scripts/sync-config.sh ./deploy/kubernetes/.env
```

**Step 2: Deploy to Kubernetes**

```bash
./deploy/kubernetes/scripts/setup-k8s.sh local
```

This will:
- Create a kind cluster (if needed)
- Build all Docker images locally
- **Load images directly into the kind cluster** (no registry required)
- Deploy all services and wait for readiness

> Note: For kind clusters, images are **loaded** into the cluster (not pulled). This is handled by `deploy/kubernetes/scripts/setup-kind-cluster.sh` and used by both Kustomize and Helm local workflows.

#### Non-Production Environment

**Step 1: Create environment and sync configuration** (same as local)

```bash
# From repository root
cd deploy
./scripts/create-env.sh --kubernetes

# Edit deploy/kubernetes/.env with your configuration values

# Sync .env to Kubernetes ConfigMaps/Secrets manifests
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

#### Production Environment

```bash
cd deploy/kubernetes
ENV=prod \
  REGISTRY=ghcr.io/your-org \
  TAG=v1.2.3 \
  ./scripts/setup-k8s.sh prod
```

### Building and Pushing Images

LogWise uses different image handling strategies depending on the environment:

| Environment | Image Handling | Registry Required |
|------------|----------------|-------------------|
| **Local (kind)** | Build → **Load into kind** | ❌ No |
| **Nonprod/Prod** | Build → **Push to registry** → Cluster pulls | ✅ Yes |

**Important:** For **non-production** and **production** environments, your cluster must be able to pull images from the configured registry (and may require image pull secrets).

#### For Non-Production and Production

**Step 1: Authenticate with Container Registry**

Choose your registry and authenticate:

**AWS ECR:**
```bash
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com
```

**Docker Hub:**
```bash
docker login --username your-username
# Or with password
echo "your-password" | docker login --username your-username --password-stdin
```

**GitHub Container Registry (GHCR):**
```bash
echo "$GITHUB_TOKEN" | docker login ghcr.io -u your-username --password-stdin
```

**Google Container Registry (GCR):**
```bash
gcloud auth configure-docker
```

**Step 2: Build and Push Images**

Use the build-and-push script to build all required images and push them to your registry:

```bash
cd deploy/kubernetes

# For non-production
ENV=nonprod \
  REGISTRY=ghcr.io/your-org \
  TAG=1.0.0 \
  ./scripts/build-and-push.sh

# For production (use semantic versioning)
ENV=prod \
  REGISTRY=ghcr.io/your-org \
  TAG=v1.2.3 \
  ./scripts/build-and-push.sh
```

**What gets built:**
- `logwise-orchestrator`
- `logwise-spark`
- `logwise-vector`
- `logwise-healthcheck-dummy`

**Registry Formats:**
- **ECR**: `123456789012.dkr.ecr.us-east-1.amazonaws.com`
- **Docker Hub**: `dockerhub` (requires `DOCKERHUB_USERNAME` environment variable)
- **GHCR**: `ghcr.io/your-org`
- **GCR**: `gcr.io/project-id`
- **ACR**: `your-registry.azurecr.io`

**Step 3: Configure Image Pull Secrets (if using private registry)**

Create Kubernetes secrets for your registry:

**For ECR:**
```bash
kubectl create secret docker-registry ecr-secret \
  --docker-server=123456789012.dkr.ecr.us-east-1.amazonaws.com \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-login-password --region us-east-1) \
  -n logwise
```

**For Docker Hub:**
```bash
kubectl create secret docker-registry dockerhub-secret \
  --docker-server=docker.io \
  --docker-username=your-username \
  --docker-password=your-password \
  --docker-email=your-email@example.com \
  -n logwise
```

**For GHCR:**
```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=your-username \
  --docker-password=$GITHUB_TOKEN \
  --docker-email=your-email@example.com \
  -n logwise
```

**Step 4: Update Kustomize Overlay with Image References**

After pushing images, update your overlay to use the registry images:

```bash
cd deploy/kubernetes/overlays/nonprod  # or prod

# Update images in kustomization.yaml
kustomize edit set image logwise-orchestrator=ghcr.io/your-org/logwise-orchestrator:1.0.0
kustomize edit set image logwise-spark=ghcr.io/your-org/logwise-spark:1.0.0
kustomize edit set image logwise-vector=ghcr.io/your-org/logwise-vector:1.0.0
kustomize edit set image logwise-healthcheck-dummy=ghcr.io/your-org/logwise-healthcheck-dummy:1.0.0
```

**Note:** The nonprod and prod overlays include patches that set `imagePullPolicy: Always` and reference image pull secrets. Ensure your secret name matches the one configured in the overlay patches.

#### For Local Development (kind)

For local development with kind, images are loaded directly into the cluster (no registry needed):

```bash
cd deploy/kubernetes
ENV=local CLUSTER_TYPE=kind TAG=latest ./scripts/build-and-push.sh
```

This builds images locally and loads them into the kind cluster automatically.

### Environment-Specific Deployments

#### Local Development

The `local` overlay includes:
- NodePort services for easy access
- Reduced resource limits
- Ephemeral storage (emptyDir)
- Images loaded directly into kind (no registry)

```bash
cd deploy/kubernetes/overlays/local
kubectl apply -k .
```

#### Non-Production

The `nonprod` overlay includes:
- Standard resource configurations
- ClusterIP services (use ingress or port-forward)
- Example ingress configurations
- **Requires images to be pushed to registry first** (see Building and Pushing Images above)

**Deployment steps:**
1. Build and push images to registry (see above)
2. Create image pull secrets (if private registry)
3. Update overlay with image references
4. Deploy:

```bash
cd deploy/kubernetes/overlays/nonprod
kubectl apply -k .
```

#### Production

The `prod` overlay includes:
- Higher resource limits
- Persistent volumes for data
- Anti-affinity rules for high availability
- Pod disruption budgets
- **Requires images to be pushed to registry first** (see Building and Pushing Images above)

**Deployment steps:**
1. Build and push images to registry with version tag (see above)
2. Create image pull secrets (if private registry)
3. Update overlay with image references
4. Deploy:

```bash
cd deploy/kubernetes/overlays/prod
kubectl apply -k .
```

**Important:** Always use semantic versioning tags (e.g., `v1.2.3`) for production. Never use `latest` tag in production.

### Customizing with Kustomize

#### Using Environment Variables for Image Registry

You can override images using environment variables:

```bash
cd deploy/kubernetes/overlays/prod
REGISTRY=ghcr.io/your-org TAG=v1.0.0 kustomize build . | kubectl apply -f -
```

Or use `kustomize edit`:

```bash
cd deploy/kubernetes/overlays/prod
kustomize edit set image logwise-orchestrator=ghcr.io/your-org/logwise-orchestrator:v1.0.0
kustomize edit set image logwise-spark=ghcr.io/your-org/logwise-spark:v1.0.0
kustomize edit set image logwise-vector=ghcr.io/your-org/logwise-vector:v1.0.0
kubectl apply -k .
```

#### Creating Custom Overlays

1. Create a new overlay directory:
   ```bash
   mkdir -p deploy/kubernetes/overlays/custom-env
   ```

2. Create `kustomization.yaml`:
   ```yaml
   apiVersion: kustomize.config.k8s.io/v1beta1
   kind: Kustomization
   
   resources:
     - ../../base
   
   patchesStrategicMerge:
     - custom-patch.yaml
   
   images:
     - name: logwise-orchestrator
       newName: your-registry/logwise-orchestrator
       newTag: v1.0.0
   ```

3. Create patches as needed:
   ```yaml
   # custom-patch.yaml
   apiVersion: apps/v1
   kind: Deployment
   metadata:
     name: orchestrator
   spec:
     replicas: 3
   ```

4. Deploy:
   ```bash
   kubectl apply -k deploy/kubernetes/overlays/custom-env
   ```

### Using Deployment Scripts

The repository includes deployment scripts that simplify the process:

```bash
# Using the unified deployment script
cd deploy/kubernetes
ENV=local ./scripts/deploy.sh

# With custom registry
ENV=prod REGISTRY=ghcr.io/your-org TAG=v1.0.0 ./scripts/deploy.sh
```

The script handles:
- Pre-deployment validation
- Namespace creation
- Image registry configuration
- Deployment and health checks
- Rollback on failure

### Components Deployed

The Kustomize base includes:
- **Orchestrator**: Service coordination and job management
- **Spark**: Master and Worker deployments for stream processing
- **Kafka**: Message broker for log streaming
- **Vector**: Log collection and forwarding
- **Grafana**: Visualization and dashboards
- **MySQL**: Separate instances for orchestrator and Grafana
- **OTEL Collector**: OpenTelemetry log collection
- **Healthcheck Dummy**: Test service for generating sample logs
- **Cron Jobs**: Scheduled tasks for orchestrator synchronization

---

## Helm Chart Deployment

Helm provides a templated approach to deploying Logwise with parameterized values.

### Quick Start (Local Development)

1. **Set up a kind cluster** (optional, if you don't have one):
   ```bash
   cd deploy/kubernetes/helm/logwise
   ./setup-kind-cluster.sh
   ```

2. **Install using the quick install script**:
   ```bash
   ./quick-install.sh [AWS_ACCESS_KEY] [AWS_SECRET] [S3_BUCKET] [S3_ATHENA_OUTPUT] [AWS_SESSION_TOKEN]
   ```
   
   Note: `AWS_SESSION_TOKEN` is optional and only needed for temporary credentials (e.g., STS tokens).

   Or install manually:
   ```bash
   helm install logwise . \
     --namespace logwise \
     --create-namespace \
     --values values-local.yaml \
     --set aws.accessKeyId=YOUR_KEY \
     --set aws.secretAccessKey=YOUR_SECRET
   ```

3. **Set up port forwarding** (if not using kind with port mappings):
   ```bash
   ./post-install.sh
   ```

4. **Access services**:
   - Orchestrator: `http://localhost:30081`
   - Grafana: `http://localhost:30080` (admin/admin)
   - Spark Master: `http://localhost:30082`
   - Spark Worker UI: `http://localhost:30083`

### Installation Methods

#### Method 1: Quick Install Script (Recommended for Local)

```bash
cd deploy/kubernetes/helm/logwise
./quick-install.sh [AWS_KEY] [AWS_SECRET] [S3_BUCKET] [S3_ATHENA_OUTPUT] [AWS_SESSION_TOKEN]
```

**Features:**
- Automatically detects install vs upgrade
- Creates namespace if needed
- Simple command-line interface

#### Method 2: Install/Upgrade Script (Interactive)

```bash
./install-or-upgrade.sh [AWS_KEY] [AWS_SECRET] [S3_BUCKET] [S3_ATHENA_OUTPUT]
```

**Features:**
- Interactive prompts for missing values
- Confirmation before installation
- Optional post-install port forwarding

#### Method 3: Manual Helm Commands

**Install:**
```bash
helm install logwise . \
  --namespace logwise \
  --create-namespace \
  --values values-local.yaml \
  --set aws.accessKeyId=YOUR_KEY \
  --set aws.secretAccessKey=YOUR_SECRET
```

**Upgrade:**
```bash
helm upgrade logwise . \
  --namespace logwise \
  --values values-local.yaml \
  --set aws.accessKeyId=YOUR_KEY
```

### Environment-Specific Values

#### Local Development (`values-local.yaml`)

Optimized for local development:
- NodePort services for easy access
- Reduced CPU/memory limits
- Ephemeral storage (emptyDir)
- Image pull policy: IfNotPresent

```bash
helm install logwise . \
  --namespace logwise \
  --create-namespace \
  --values values-local.yaml
```

#### Non-Production (`values-nonprod.yaml`)

Configuration for staging/development:
- ClusterIP services (use ingress or port-forward)
- Ingress enabled with example hosts
- Standard resource configuration
- Image pull policy: Always
- **Requires images to be pushed to registry first** (see Building and Pushing Images above)

**Deployment steps:**
1. Build and push images to registry (see Building and Pushing Images above)
2. Create image pull secrets (if private registry)
3. Configure image registry in values file or via --set flags
4. Deploy:

```bash
helm install logwise . \
  --namespace logwise \
  --create-namespace \
  --values values-nonprod.yaml \
  --set global.imageRegistry=ghcr.io/your-org \
  --set global.imagePullSecrets[0].name=ghcr-secret \
  --set images.orchestrator.tag=1.0.0 \
  --set images.spark.tag=1.0.0 \
  --set images.vector.tag=1.0.0 \
  --set aws.accessKeyId=KEY \
  --set aws.secretAccessKey=SECRET
```

#### Production (`values-prod.yaml`)

Production-ready configuration:
- Higher CPU/memory limits
- Increased replicas for high availability
- Persistent volumes for data persistence
- Kafka: 3 replicas, 7-day log retention
- Ingress enabled with production hosts
- **Requires images to be pushed to registry first** (see Building and Pushing Images above)

**Deployment steps:**
1. Build and push images to registry with version tag (see Building and Pushing Images above)
2. Create image pull secrets (if private registry)
3. Configure image registry in values file or via --set flags
4. Deploy:

```bash
helm install logwise . \
  --namespace logwise \
  --create-namespace \
  --values values-prod.yaml \
  --set global.imageRegistry=ghcr.io/your-org \
  --set global.imagePullSecrets[0].name=ghcr-secret \
  --set images.orchestrator.tag=v1.2.3 \
  --set images.spark.tag=v1.2.3 \
  --set images.vector.tag=v1.2.3 \
  --set aws.accessKeyId=KEY \
  --set aws.secretAccessKey=SECRET
```

**Important:** Always use semantic versioning tags (e.g., `v1.2.3`) for production. Never use `latest` tag in production.

### Key Configuration Sections

#### AWS Configuration

```yaml
aws:
  region: us-east-1
  accessKeyId: ""  # Set via --set or values file
  secretAccessKey: ""  # Set via --set or values file
  sessionToken: ""  # Optional, for temporary credentials
  s3BucketName: ""
  s3AthenaOutput: ""
  athenaWorkgroup: primary
  athenaCatalog: AwsDataCatalog
  athenaDatabase: logwise
```

#### Component Enable/Disable

```yaml
components:
  orchestrator:
    enabled: true
  spark:
    enabled: true
  kafka:
    enabled: true
  vector:
    enabled: true
  grafana:
    enabled: true
  mysql:
    enabled: true
  otel:
    enabled: true
  healthcheck:
    enabled: true
  cron:
    enabled: true
```

#### Service Configuration

```yaml
services:
  type: ClusterIP  # ClusterIP, NodePort, or LoadBalancer
  nodePorts:
    orchestrator: 30081
    grafana: 30080
    sparkMaster: 30082
    sparkWorker: 30083
    vectorOtlp: 30418
```

#### Ingress Configuration

```yaml
ingress:
  enabled: false
  className: nginx
  orchestrator:
    host: orchestrator.example.com
  grafana:
    host: grafana.example.com
```

### Setting Values

You can override values in multiple ways:

1. **Using values files:**
   ```bash
   helm install logwise . --values values-prod.yaml
   ```

2. **Using --set flags:**
   ```bash
   helm install logwise . --set aws.accessKeyId=YOUR_KEY
   ```

3. **Using multiple values files:**
   ```bash
   helm install logwise . --values values.yaml --values values-prod.yaml
   ```

4. **Using --set-file for sensitive data:**
   ```bash
   helm install logwise . --set-file aws.secretAccessKey=./secret.txt
   ```


### Building and Pushing Images

**Important:** For **non-production** and **production** environments, you must build and push Docker images to a container registry before deployment. Local development can use local images or load into kind.

#### For Non-Production and Production

**Step 1: Authenticate with Container Registry**

Choose your registry and authenticate:

**AWS ECR:**
```bash
aws ecr get-login-password --region us-east-1 | \
  docker login --username AWS --password-stdin \
  123456789012.dkr.ecr.us-east-1.amazonaws.com
```

**Docker Hub:**
```bash
docker login --username your-username
# Or with password
echo "your-password" | docker login --username your-username --password-stdin
```

**GitHub Container Registry (GHCR):**
```bash
echo "$GITHUB_TOKEN" | docker login ghcr.io -u your-username --password-stdin
```

**Google Container Registry (GCR):**
```bash
gcloud auth configure-docker
```

**Step 2: Build and Push Images**

Use the build-and-push script to build all required images and push them to your registry:

```bash
cd deploy/kubernetes/scripts

# For non-production
ENV=nonprod \
  REGISTRY=ghcr.io/your-org \
  TAG=1.0.0 \
  ./build-and-push.sh

# For production (use semantic versioning)
ENV=prod \
  REGISTRY=ghcr.io/your-org \
  TAG=v1.2.3 \
  ./build-and-push.sh
```

**What gets built:**
- `logwise-orchestrator`
- `logwise-spark`
- `logwise-vector`
- `logwise-healthcheck-dummy`

**Registry Formats:**
- **ECR**: `123456789012.dkr.ecr.us-east-1.amazonaws.com`
- **Docker Hub**: `dockerhub` (requires `DOCKERHUB_USERNAME` environment variable)
- **GHCR**: `ghcr.io/your-org`
- **GCR**: `gcr.io/project-id`
- **ACR**: `your-registry.azurecr.io`

**Step 3: Configure Image Pull Secrets (if using private registry)**

Create Kubernetes secrets for your registry:

**For ECR:**
```bash
kubectl create secret docker-registry ecr-secret \
  --docker-server=123456789012.dkr.ecr.us-east-1.amazonaws.com \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-login-password --region us-east-1) \
  -n logwise
```

**For Docker Hub:**
```bash
kubectl create secret docker-registry dockerhub-secret \
  --docker-server=docker.io \
  --docker-username=your-username \
  --docker-password=your-password \
  --docker-email=your-email@example.com \
  -n logwise
```

**For GHCR:**
```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=your-username \
  --docker-password=$GITHUB_TOKEN \
  --docker-email=your-email@example.com \
  -n logwise
```

**Step 4: Configure Helm Values with Image Registry**

Update your Helm values file (`values-nonprod.yaml` or `values-prod.yaml`) with the registry and image pull secrets:

```yaml
global:
  imageRegistry: ghcr.io/your-org
  imagePullSecrets:
    - name: ghcr-secret  # or ecr-secret, dockerhub-secret, etc.
  imagePullPolicy: Always

images:
  orchestrator:
    repository: logwise-orchestrator
    tag: "1.0.0"  # Use semantic versioning for production
  spark:
    repository: logwise-spark
    tag: "1.0.0"
  vector:
    repository: logwise-vector
    tag: "1.0.0"
  healthcheck:
    repository: logwise-healthcheck-dummy
    tag: "1.0.0"
```

Or set via command line:

```bash
helm install logwise . \
  --namespace logwise \
  --create-namespace \
  --values values-nonprod.yaml \
  --set global.imageRegistry=ghcr.io/your-org \
  --set global.imagePullSecrets[0].name=ghcr-secret \
  --set images.orchestrator.tag=1.0.0 \
  --set images.spark.tag=1.0.0 \
  --set images.vector.tag=1.0.0 \
  --set images.healthcheck.tag=1.0.0
```

**Note:** The `values-nonprod.yaml` and `values-prod.yaml` files already configure `imagePullPolicy: Always`. Ensure your image pull secret name matches what's configured in the values file.

#### For Local Development

For local development, you can use local images or load into kind:

```bash
# Option 1: Build and load into kind
cd deploy/kubernetes/scripts
ENV=local CLUSTER_TYPE=kind TAG=latest ./build-and-push.sh

# Option 2: Use local images (imagePullPolicy: IfNotPresent)
# No build-and-push needed, just deploy with values-local.yaml
```

---

## Configuration

### Secrets Management

#### For Kustomize

1. **Create secrets from example:**
   ```bash
   cd deploy/kubernetes/config
   cp secrets.example.yaml secrets.yaml
   # Edit secrets.yaml with your actual values
   ```

2. **Apply secrets:**
   ```bash
   kubectl apply -f secrets.yaml
   ```

3. **For production**, use one of:
   - External Secrets Operator
   - AWS Secrets Manager with External Secrets
   - HashiCorp Vault
   - Sealed Secrets

#### For Helm

**DO NOT** use `--set` flags for secrets in production. Use one of:

1. **External Secrets Operator:**
   ```yaml
   # See deploy/kubernetes/config/secrets.example.yaml
   ```

2. **HashiCorp Vault:**
   ```bash
   # Use vault-secrets-operator or similar
   ```

3. **Sealed Secrets:**
   ```bash
   kubectl create secret generic aws-credentials \
     --from-literal=accessKeyId=KEY \
     --from-literal=secretAccessKey=SECRET \
     --dry-run=client -o yaml | kubeseal -o yaml > sealed-secret.yaml
   ```

### AWS Configuration

Both methods require AWS credentials for S3 and Athena access:

**Required:**
- AWS Access Key ID
- AWS Secret Access Key
- S3 Bucket Name
- S3 Athena Output location

**Optional:**
- AWS Session Token (for temporary credentials)
- AWS Region (defaults to us-east-1)

### Database Configuration

Logwise uses two MySQL instances:
1. **Orchestrator DB**: Stores service metadata, job history, scaling decisions
2. **Grafana DB**: Stores Grafana dashboards and metadata

Both can be configured with custom usernames, passwords, and storage options.

### Resource Requirements

**Minimum (Local Development):**
- CPU: 4 cores
- Memory: 8 GiB
- Storage: 20 GiB (ephemeral)

**Recommended (Production):**
- CPU: 16+ cores
- Memory: 32+ GiB
- Storage: 200+ GiB (persistent)

---

## Accessing Services

### Local Development

#### Using NodePort (kind/minikube)

If you created the cluster with NodePort mappings:
- `http://localhost:30080` (Grafana)
- `http://localhost:30081` (Orchestrator)
- `http://localhost:30082` (Spark Master)
- `http://localhost:30083` (Spark Worker UI)

#### Using Port Forwarding

```bash
# Grafana
kubectl port-forward -n logwise svc/grafana 30080:3000

# Orchestrator
kubectl port-forward -n logwise svc/orchestrator 30081:8080

# Spark Master
kubectl port-forward -n logwise svc/spark-master 30082:8080

# Spark Worker UI
kubectl port-forward -n logwise svc/spark-worker-ui 30083:8081
```

Or use the post-install script (Helm):
```bash
cd deploy/kubernetes/helm/logwise
./post-install.sh
```

### Production/Remote Clusters

#### Using Ingress

Configure ingress hosts in your values/overlay and access via domain names:
- `https://grafana.prod.example.com`
- `https://orchestrator.prod.example.com`

#### Using LoadBalancer

Set `services.type: LoadBalancer` (Helm) or configure LoadBalancer services (Kustomize) and use the external IPs provided.

#### Using Port Forwarding

Same as local development, but connect to your remote cluster.

---

## Metrics Server and HPA

Logwise includes **Metrics Server** in both Kustomize and Helm deployments to enable Horizontal Pod Autoscaler (HPA) functionality for the Vector component.

### What is Metrics Server?

Metrics Server collects resource usage metrics (CPU and memory) from Kubernetes nodes and pods, and exposes them via the Metrics API. This enables:
- **HPA** to make scaling decisions based on CPU and memory utilization
- **kubectl top** commands to display resource usage
- **Resource-based autoscaling** for better resource management

### Automatic Installation

Metrics Server is **automatically deployed** with Logwise:

- **Kustomize**: Included in `base/metrics-server/` and automatically applied
- **Helm**: Included as a template and enabled by default via `metricsServer.enabled: true`

### Cluster-Specific Configuration

**For kind/local clusters:**
- Kustomize: Automatically patched with `--kubelet-insecure-tls` flag in `overlays/local/`
- Helm: Set `metricsServer.kubeletInsecureTLS: true` in values.yaml

**For managed clusters (EKS, GKE, AKS):**
- Standard installation (no insecure-tls flag needed)
- If Metrics Server already exists, Kubernetes handles conflicts gracefully

### Verifying Metrics Server

After deployment, verify Metrics Server is working:

```bash
# Check Metrics Server pod
kubectl get pods -n kube-system | grep metrics-server

# Test Metrics API
kubectl top nodes
kubectl top pods -n logwise

# Check HPA status (should show CPU and memory metrics)
kubectl get hpa -n logwise
kubectl describe hpa vector-logs -n logwise
```

### Vector HPA Configuration

The Vector component uses HPA with both CPU and memory metrics:

- **CPU target**: 70% utilization (configurable)
- **Memory target**: 80% utilization (configurable)
- **Min replicas**: 1
- **Max replicas**: 5 (configurable)

HPA automatically scales Vector pods based on resource usage, ensuring optimal performance under varying load conditions.


## Upgrading

### Kustomize

1. **Update manifests:**
   ```bash
   cd deploy/kubernetes/overlays/prod
   # Update kustomization.yaml or patches as needed
   ```

2. **Apply changes:**
   ```bash
   kubectl apply -k .
   ```

3. **Update images:**
   ```bash
   kustomize edit set image logwise-orchestrator=ghcr.io/your-org/logwise-orchestrator:v1.1.0
   kubectl apply -k .
   ```

### Helm

1. **Upgrade existing release:**
   ```bash
   helm upgrade logwise . \
     --namespace logwise \
     --values values-prod.yaml \
     --set aws.accessKeyId=NEW_KEY
   ```

2. **Upgrade with new values file:**
   ```bash
   helm upgrade logwise . \
     --namespace logwise \
     --values values-prod.yaml \
     --reuse-values  # Keep existing values not in new file
   ```

3. **Check upgrade status:**
   ```bash
   helm status logwise -n logwise
   kubectl get pods -n logwise
   ```

---

## Troubleshooting

### Pods Not Starting

1. **Check pod status:**
   ```bash
   kubectl get pods -n logwise
   kubectl describe pod <pod-name> -n logwise
   ```

2. **Check logs:**
   ```bash
   kubectl logs <pod-name> -n logwise
   kubectl logs <pod-name> -n logwise --previous  # Previous container
   ```

3. **Common issues:**
   - **ImagePullBackOff**: Check image registry and credentials
   - **CrashLoopBackOff**: Check application logs
   - **Pending**: Check resource availability and PVCs

### Services Not Accessible

1. **Check service endpoints:**
   ```bash
   kubectl get svc -n logwise
   kubectl get endpoints -n logwise
   ```

2. **Verify port forwarding:**
   ```bash
   ps aux | grep "kubectl port-forward"
   ```

3. **Check ingress:**
   ```bash
   kubectl get ingress -n logwise
   kubectl describe ingress -n logwise
   ```

### Database Connection Issues

1. **Check MySQL pods:**
   ```bash
   kubectl get pods -n logwise | grep mysql
   kubectl logs <mysql-pod> -n logwise
   ```

2. **Verify secrets:**
   ```bash
   kubectl get secrets -n logwise
   kubectl describe secret <secret-name> -n logwise
   ```

### Kafka Connection Issues

1. **Check Kafka pod:**
   ```bash
   kubectl get pods -n logwise | grep kafka
   kubectl logs <kafka-pod> -n logwise
   ```

2. **Verify bootstrap servers:**
   ```bash
   kubectl get configmap logwise-config -n logwise -o yaml
   ```

### Resource Constraints

If pods are being evicted or not scheduling:

1. **Check resource requests/limits:**
   ```bash
   kubectl describe node
   ```

2. **Reduce resources** in values file (Helm) or patches (Kustomize)

### AWS Credentials Issues

1. **Verify secrets:**
   ```bash
   kubectl get secret aws-credentials -n logwise -o jsonpath='{.data}' | base64 -d
   ```

2. **Check IAM permissions:**
   - S3 read/write access
   - Athena query execution
   - Glue catalog access (if using)

---

## Production Considerations

### Pre-Deployment Checklist

- [ ] Review and update configuration files with production values
- [ ] Set up persistent volumes for Kafka and MySQL
- [ ] Configure ingress with proper TLS certificates
- [ ] Set up AWS credentials using secrets management (not --set flags)
- [ ] Configure resource limits based on expected load
- [ ] Set up monitoring and alerting
- [ ] Configure backup strategy for databases
- [ ] Review security settings (RBAC, network policies)
- [ ] Test in non-production environment first

### High Availability

For HA deployments:

1. **Multiple replicas**: Set replicas > 1 for critical components
2. **Pod Disruption Budgets**: Consider adding PDBs (included in prod overlay)
3. **Anti-affinity rules**: Spread pods across nodes (included in prod overlay)
4. **Persistent storage**: Use network-attached storage (EBS, EFS, etc.)
5. **Load balancers**: Use LoadBalancer or Ingress with multiple replicas

### Monitoring

Set up monitoring for production:

1. **Prometheus metrics**: Components expose metrics endpoints
2. **Grafana dashboards**: Pre-configured dashboards available
3. **Log aggregation**: Use Vector to forward logs to your SIEM
4. **Alerting**: Configure alerts for:
   - Pod failures
   - High resource usage
   - Kafka lag
   - Database connection issues

### Backup Strategy

1. **MySQL backups:**
   - Use Velero or similar for PVC backups
   - Or set up MySQL dump cron jobs

2. **Kafka data**: Consider replication factor > 1 for production

3. **Configuration**: Version control all values files and overlays

### Scaling

The deployments support horizontal scaling:

1. **Vector**: HPA enabled by default with CPU and memory metrics (requires Metrics Server)
   - Automatically scales based on CPU (70% target) and memory (80% target) utilization
   - Min replicas: 1, Max replicas: 5 (configurable)
   - Metrics Server is automatically included in deployments
2. **Spark Workers**: Increase replicas
3. **Kafka**: Increase replicas (requires proper configuration)
4. **Orchestrator**: Increase replicas (ensure shared state handling)

**Note**: Vector HPA requires Metrics Server to function. Metrics Server is automatically deployed with Logwise, but if you're using a managed cluster that already has Metrics Server, ensure it's properly configured and accessible.

### Security

1. **Network Security:**
   - Use NetworkPolicies to restrict pod-to-pod communication
   - Configure TLS for ingress
   - Use private networks where possible

2. **Access Control:**
   - Use RBAC for Kubernetes access
   - Configure IAM roles for AWS services
   - Use secrets management for sensitive data

3. **Image Security:**
   - Use private image registries
   - Scan images for vulnerabilities
   - Use image pull secrets

---

## Support

For issues and questions:
1. Check the troubleshooting section above
2. Review component-specific documentation
3. Check Kubernetes and Helm logs
4. Open an issue in the repository
