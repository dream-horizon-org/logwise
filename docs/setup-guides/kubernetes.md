---
title: Kubernetes Deployment Guide
---

# Kubernetes Deployment Guide

This guide provides comprehensive instructions for deploying Logwise on Kubernetes using either **Kustomize** or **Helm Charts**. Both methods are fully supported and provide different benefits depending on your deployment preferences.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Choosing a Deployment Method](#choosing-a-deployment-method)
- [Common Setup Steps](#common-setup-steps)
- [Logwise Components Overview](#logwise-components-overview)
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

5. **Docker Hub Account** (or other container registry):
   - Username
   - Access token or password
   - Images built and pushed to registry

6. **AWS Credentials** (optional): If using S3/Athena for log storage
   - AWS Access Key ID
   - AWS Secret Access Key
   - S3 Bucket Name
   - S3 Athena Output location

## Quick Reference

### Helm Deployment (Non-Production)

```bash
# 1. Build and push images
cd /path/to/logwise
PLATFORM=linux/amd64 ENV=nonprod DOCKERHUB_USERNAME=your-username \
DOCKERHUB_PASSWORD=your-token REGISTRY=dockerhub TAG=1.0.0 \
./deploy/kubernetes/scripts/build-and-push.sh

# 2. Setup namespace and secret
cd deploy/kubernetes/helm/logwise
kubectl create namespace logwise
kubectl label namespace logwise app.kubernetes.io/managed-by=Helm --overwrite
kubectl annotate namespace logwise meta.helm.sh/release-name=logwise --overwrite
kubectl annotate namespace logwise meta.helm.sh/release-namespace=logwise --overwrite

kubectl create secret docker-registry dockerhub-secret \
  --docker-server=docker.io \
  --docker-username=your-username \
  --docker-password=your-token \
  --docker-email=your-email@example.com \
  --namespace=logwise

# 3. Deploy
helm upgrade --install logwise . \
  --namespace logwise \
  --values values-nonprod.yaml \
  --set global.imageRegistry=your-username \
  --set 'global.imagePullSecrets[0]=dockerhub-secret' \
  --set metricsServer.enabled=false \
  --set orchestrator.image.tag=1.0.0 \
  --set spark.master.image.tag=1.0.0 \
  --set spark.worker.image.tag=1.0.0 \
  --set vector.image.tag=1.0.0 \
  --set healthcheck.image.tag=1.0.0 \
  --force

# 4. Verify
kubectl get pods -n logwise -o wide
```

### Kustomize Deployment (Non-Production)

```bash
# 1. Build and push images
cd /path/to/logwise
PLATFORM=linux/amd64 ENV=nonprod DOCKERHUB_USERNAME=your-username \
DOCKERHUB_PASSWORD=your-token REGISTRY=dockerhub TAG=1.0.0 \
./deploy/kubernetes/scripts/build-and-push.sh

# 2. Prepare secrets
cd deploy/kubernetes/config
cp secrets.example.yaml secrets.yaml
# Edit secrets.yaml with your credentials

# 3. Apply secrets
kubectl apply -f secrets.yaml

# 4. Deploy
cd ../overlays/nonprod
REGISTRY=your-username TAG=1.0.0 kustomize build . | kubectl apply -f -

# 5. Verify
kubectl get pods -n logwise -o wide
```

See the detailed sections below for explanations and troubleshooting.

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

### Comparison Table

| Feature | Kustomize | Helm |
|---------|-----------|------|
| **Learning Curve** | Low (native K8s) | Medium (templating) |
| **GitOps Support** | Excellent (ArgoCD, Flux) | Good (with Helm operator) |
| **Template Engine** | None (pure YAML) | Go templates |
| **Value Overrides** | Patches/overlays | Values files + --set |
| **Rollback** | Manual (git) | Built-in (`helm rollback`) |
| **Dependencies** | None (built into kubectl) | Helm CLI required |
| **Best For** | GitOps, declarative workflows | CI/CD, parameterized deployments |

---

## Common Setup Steps

These steps are common to both deployment methods and should be completed before deploying.

### Step 1: Build and Push Docker Images

Both deployment methods require Docker images to be built and pushed to a container registry.

```bash
cd /path/to/logwise

PLATFORM=linux/amd64 \
ENV=nonprod \
DOCKERHUB_USERNAME=your-username \
DOCKERHUB_PASSWORD=your-token \
REGISTRY=dockerhub \
TAG=1.0.0 \
./deploy/kubernetes/scripts/build-and-push.sh
```

**Notes:**
- `PLATFORM=linux/amd64` is recommended for most cloud providers (EKS, GKE, AKS)
- For local development (kind/minikube), you can omit `PLATFORM` or use `PLATFORM=linux/amd64`
- Replace `your-username` and `your-token` with your Docker Hub credentials
- For other registries (ECR, GHCR), adjust `REGISTRY` accordingly

### Step 2: Create Namespace

Both methods deploy to the `logwise` namespace:

```bash
kubectl create namespace logwise
```

**For Helm deployments only**, configure namespace ownership:

```bash
kubectl label namespace logwise app.kubernetes.io/managed-by=Helm --overwrite
kubectl annotate namespace logwise meta.helm.sh/release-name=logwise --overwrite
kubectl annotate namespace logwise meta.helm.sh/release-namespace=logwise --overwrite
```

### Step 3: Create Image Pull Secret

If using a private registry (Docker Hub, ECR, GHCR), create a Kubernetes secret:

```bash
kubectl create secret docker-registry dockerhub-secret \
  --docker-server=docker.io \
  --docker-username=your-username \
  --docker-password=your-token \
  --docker-email=your-email@example.com \
  --namespace=logwise
```

**Important:**
- For Docker Hub, use an access token (recommended) instead of your password
- For ECR: `--docker-server=ACCOUNT.dkr.ecr.REGION.amazonaws.com`
- For GHCR: `--docker-server=ghcr.io`

### Step 4: Prepare Secrets (Kustomize Only)

For Kustomize deployments, prepare secrets from the example:

```bash
cd deploy/kubernetes/config
cp secrets.example.yaml secrets.yaml
# Edit secrets.yaml with your AWS credentials and database passwords
kubectl apply -f secrets.yaml
```

**Note:** Helm deployments can use secrets management tools or `--set` flags (not recommended for production).

---

## Logwise Components Overview

Logwise consists of the following components, deployed by both Kustomize and Helm:

### Core Services

1. **Orchestrator** (`orchestrator/`)
   - Main control service for Logwise
   - Manages Spark jobs, Kafka topics, and data pipelines
   - Requires: MySQL, Kafka, Spark, Vector
   - Port: 8080
   - **Purpose**: Coordinates all Logwise operations and manages the log processing pipeline

2. **Vector** (`vector/`)
   - Log aggregation and processing
   - Receives logs via OTLP (gRPC/HTTP)
   - Forwards to Kafka
   - Has HPA (Horizontal Pod Autoscaler) for scaling
   - Ports: 8686 (API), 4317 (OTLP gRPC), 4318 (OTLP HTTP)
   - **Purpose**: Collects, transforms, and routes logs to Kafka

3. **Spark** (`spark/`)
   - **Spark Master**: Job coordination (port 7077, UI 8080)
   - **Spark Worker**: Executes Spark jobs
   - Used for log processing and analytics
   - **Purpose**: Processes log streams from Kafka and performs analytics

4. **Kafka** (`kafka/`)
   - Message broker for log streaming
   - Single broker setup (can be scaled)
   - Port: 9092
   - **Purpose**: Buffers and streams logs between Vector and Spark

5. **MySQL** (`mysql/`)
   - **Orchestrator DB**: Stores orchestrator metadata, job history, scaling decisions
   - **Grafana DB**: Stores Grafana dashboards and metadata
   - Two separate MySQL instances
   - **Purpose**: Persistent storage for application and dashboard data

6. **Grafana** (`grafana/`)
   - Visualization and dashboards
   - Pre-configured datasources (Athena, Infinity)
   - Port: 3000
   - **Purpose**: Provides UI for log visualization and querying

### Supporting Services

7. **OTEL Collector** (`otel/`)
   - OpenTelemetry collector
   - Receives telemetry data
   - Forwards to Vector
   - **Purpose**: Collects OpenTelemetry traces and metrics

8. **Metrics Server** (`metrics-server/`)
   - Provides resource metrics for HPA
   - Required for Vector HPA to function
   - Deployed in `kube-system` namespace (or `logwise` for local)
   - **Purpose**: Enables horizontal pod autoscaling

9. **Healthcheck Dummy** (`healthcheck/`)
   - Generates test logs for validation
   - Used for testing the pipeline
   - **Purpose**: Generates sample logs for testing and validation

10. **Cron Jobs** (`cron/`)
    - Scheduled tasks (e.g., orchestrator sync)
    - **Purpose**: Periodic maintenance and synchronization tasks

### Component Dependencies

```
┌─────────────┐
│  OTEL/Apps  │
└──────┬──────┘
       │
       ▼
┌─────────────┐     ┌─────────────┐
│   Vector    │────▶│    Kafka    │
└─────────────┘     └──────┬───────┘
                          │
                          ▼
                    ┌─────────────┐
                    │    Spark    │
                    └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ Orchestrator│
                    └──────┬───────┘
                           │
        ┌──────────────────┼──────────────────┐
        ▼                  ▼                  ▼
  ┌──────────┐      ┌──────────┐      ┌──────────┐
  │MySQL(Orch)│      │MySQL(Graf)│      │  Grafana │
  └──────────┘      └──────────┘      └──────────┘
```

---

## Kustomize Deployment

Kustomize uses a base configuration with environment-specific overlays to manage deployments across different environments.

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
└── config/                 # Configuration templates
    └── secrets.example.yaml
```

### Quick Start (Local Development)

1. **Prepare secrets**:
   ```bash
   cd deploy/kubernetes/config
   cp secrets.example.yaml secrets.yaml
   # Edit secrets.yaml with your AWS credentials and database passwords
   ```

2. **Apply secrets**:
   ```bash
   kubectl apply -f secrets.yaml
   ```

3. **Deploy using local overlay**:
   ```bash
   cd ../overlays/local
   kubectl apply -k .
   ```

4. **Verify deployment**:
   ```bash
   kubectl get pods -n logwise
   kubectl get services -n logwise
   ```

### Environment-Specific Deployments

#### Local Development

The `local` overlay includes:
- NodePort services for easy access
- Reduced resource limits
- Ephemeral storage (emptyDir)

```bash
cd deploy/kubernetes/overlays/local
kubectl apply -k .
```

#### Non-Production

The `nonprod` overlay includes:
- Standard resource configurations
- ClusterIP services (use ingress or port-forward)
- Example ingress configurations

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
- Image registry configuration

```bash
cd deploy/kubernetes/overlays/prod
kubectl apply -k .
```

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
cd deploy/kubernetes/scripts
ENV=local ./deploy.sh

# With custom registry
ENV=prod REGISTRY=ghcr.io/your-org TAG=v1.0.0 ./deploy.sh
```

The script handles:
- Pre-deployment validation
- Namespace creation
- Image registry configuration
- Deployment and health checks
- Rollback on failure

### Image Registry Configuration

Kustomize uses image replacement to configure container images. You can set images in multiple ways:

#### Method 1: Environment Variables (Recommended)

```bash
cd deploy/kubernetes/overlays/nonprod
REGISTRY=ghcr.io/your-org TAG=v1.0.0 kustomize build . | kubectl apply -f -
```

#### Method 2: kustomize edit

```bash
cd deploy/kubernetes/overlays/nonprod
kustomize edit set image logwise-orchestrator=ghcr.io/your-org/logwise-orchestrator:v1.0.0
kustomize edit set image logwise-spark=ghcr.io/your-org/logwise-spark:v1.0.0
kustomize edit set image logwise-vector=ghcr.io/your-org/logwise-vector:v1.0.0
kustomize edit set image logwise-healthcheck-dummy=ghcr.io/your-org/logwise-healthcheck-dummy:v1.0.0
kubectl apply -k .
```

#### Method 3: Edit kustomization.yaml

Edit the `images` section in your overlay's `kustomization.yaml`:

```yaml
images:
  - name: logwise-orchestrator
    newName: your-registry/logwise-orchestrator
    newTag: v1.0.0
  - name: logwise-spark
    newName: your-registry/logwise-spark
    newTag: v1.0.0
  # ... other images
```

### Verifying Kustomize Deployment

After deployment, verify all components are running:

```bash
# Check all pods
kubectl get pods -n logwise -o wide

# Check specific components
kubectl get pods -n logwise -l app=orchestrator
kubectl get pods -n logwise -l app=spark-master
kubectl get pods -n logwise -l app=spark-worker
kubectl get pods -n logwise -l app=vector-logs

# Check services
kubectl get svc -n logwise

# Check deployments
kubectl get deployments -n logwise
```

---

## Helm Chart Deployment

Helm provides a templated approach to deploying Logwise with parameterized values. The Helm chart is located at `deploy/kubernetes/helm/logwise/`.

### Directory Structure

```
deploy/kubernetes/helm/logwise/
├── Chart.yaml              # Chart metadata
├── values.yaml            # Default values
├── values-local.yaml      # Local development values
├── values-nonprod.yaml    # Non-production values
├── values-prod.yaml       # Production values
├── templates/             # Kubernetes resource templates
│   ├── orchestrator/
│   ├── spark/
│   ├── kafka/
│   ├── vector/
│   ├── grafana/
│   └── ...
└── scripts/               # Helper scripts
    ├── quick-install.sh
    ├── post-install.sh
    └── ...
```

### Quick Start (Non-Production Deployment)

This is the recommended approach for deploying to non-production environments (staging, development, testing).

**Prerequisites:** Complete the [Common Setup Steps](#common-setup-steps) first.

#### Step 1: Navigate to Helm Chart Directory

```bash
cd deploy/kubernetes/helm/logwise
```

#### Step 2: Deploy with Helm

Deploy Logwise using Helm with non-production values:

```bash
helm upgrade --install logwise . \
  --namespace logwise \
  --values values-nonprod.yaml \
  --set global.imageRegistry=your-username \
  --set 'global.imagePullSecrets[0]=dockerhub-secret' \
  --set metricsServer.enabled=false \
  --set orchestrator.image.tag=1.0.0 \
  --set spark.master.image.tag=1.0.0 \
  --set spark.worker.image.tag=1.0.0 \
  --set vector.image.tag=1.0.0 \
  --set healthcheck.image.tag=1.0.0 \
  --force
```

**Parameters explained:**
- `--upgrade --install`: Installs if release doesn't exist, upgrades if it does
- `--values values-nonprod.yaml`: Uses non-production configuration
- `--set global.imageRegistry`: Your Docker Hub username or registry prefix
- `--set 'global.imagePullSecrets[0]=dockerhub-secret'`: References the secret created in Step 4
- `--set metricsServer.enabled=false`: Disables metrics-server (EKS/GKE already provide it)
- `--set *.image.tag=1.0.0`: Sets image tags to match what you built in Step 1
- `--force`: Forces update even if resources were modified outside Helm

**Optional:** Add AWS credentials if using S3/Athena:
```bash
--set aws.accessKeyId=YOUR_AWS_KEY \
--set aws.secretAccessKey=YOUR_AWS_SECRET
```

#### Step 3: Verify Deployment

Check pod status:

```bash
kubectl get pods -n logwise -o wide
```

Wait for all pods to be in `Running` state. This may take 2-5 minutes.

Check specific components:
```bash
# Check orchestrator
kubectl get pods -n logwise -l app=orchestrator

# Check Spark
kubectl get pods -n logwise -l app=spark-master
kubectl get pods -n logwise -l app=spark-worker

# Check Vector
kubectl get pods -n logwise -l app=vector-logs

# Check all services
kubectl get svc -n logwise

# Check Helm release status
helm status logwise -n logwise
```

See [Accessing Services](#accessing-services) for how to access the deployed services.


### Quick Start (Local Development)

For local development with kind/minikube:

1. **Set up a kind cluster** (optional, if you don't have one):
   ```bash
   cd deploy/kubernetes/helm/logwise
   ./setup-kind-cluster.sh
   ```

2. **Install using the quick install script**:
   ```bash
   ./quick-install.sh [AWS_ACCESS_KEY] [AWS_SECRET] [S3_BUCKET] [S3_ATHENA_OUTPUT] [AWS_SESSION_TOKEN]
   ```
   
   **Note:** `AWS_SESSION_TOKEN` is optional and only needed for temporary credentials (e.g., STS tokens).

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

4. **Access services** (see [Accessing Services](#accessing-services) for details):
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

**Complete deployment steps:**

```bash
# 1. Complete common setup steps (see Common Setup Steps section)
#    - Build and push images
#    - Create namespace
#    - Create image pull secret

# 2. Navigate to Helm chart directory
cd deploy/kubernetes/helm/logwise

# 3. Configure namespace for Helm (if not done in common setup)
kubectl label namespace logwise app.kubernetes.io/managed-by=Helm --overwrite
kubectl annotate namespace logwise meta.helm.sh/release-name=logwise --overwrite
kubectl annotate namespace logwise meta.helm.sh/release-namespace=logwise --overwrite

# 4. Deploy with Helm
helm upgrade --install logwise . \
  --namespace logwise \
  --values values-nonprod.yaml \
  --set global.imageRegistry=your-username \
  --set 'global.imagePullSecrets[0]=dockerhub-secret' \
  --set metricsServer.enabled=false \
  --set orchestrator.image.tag=1.0.0 \
  --set spark.master.image.tag=1.0.0 \
  --set spark.worker.image.tag=1.0.0 \
  --set vector.image.tag=1.0.0 \
  --set healthcheck.image.tag=1.0.0 \
  --force

# 5. Verify deployment
kubectl get pods -n logwise -o wide
helm status logwise -n logwise
```

**Important Notes:**
- Replace `your-username` with your Docker Hub username or registry prefix
- Use `--upgrade --install` instead of just `install` to handle both new installations and upgrades
- Set `metricsServer.enabled=false` for EKS/GKE/AKS (they already provide metrics-server)
- The `--force` flag ensures updates even if resources were modified outside Helm

#### Production (`values-prod.yaml`)

Production-ready configuration:
- Higher CPU/memory limits
- Increased replicas for high availability
- Persistent volumes for data persistence
- Kafka: 3 replicas, 7-day log retention
- Ingress enabled with production hosts

```bash
helm install logwise . \
  --namespace logwise \
  --create-namespace \
  --values values-prod.yaml \
  --set aws.accessKeyId=KEY \
  --set aws.secretAccessKey=SECRET
```

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


---

## Configuration

This section covers configuration options that apply to both Kustomize and Helm deployments.

### Secrets Management

**⚠️ Security Best Practice:** Never commit secrets to version control. Use secrets management tools in production.

#### Kustomize

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

#### Helm

**DO NOT** use `--set` flags for secrets in production. Use one of:

1. **External Secrets Operator:**
   - Create secrets using External Secrets Operator
   - Reference them in Helm values or templates

2. **HashiCorp Vault:**
   ```bash
   # Use vault-secrets-operator or similar
   # Configure vault backend in values.yaml
   ```

3. **Sealed Secrets:**
   ```bash
   kubectl create secret generic aws-credentials \
     --from-literal=accessKeyId=KEY \
     --from-literal=secretAccessKey=SECRET \
     --dry-run=client -o yaml | kubeseal -o yaml > sealed-secret.yaml
   ```

4. **Kubernetes Secrets (Manual):**
   ```bash
   kubectl create secret generic aws-credentials \
     --from-literal=accessKeyId=KEY \
     --from-literal=secretAccessKey=SECRET \
     --namespace=logwise
   ```
   Then reference in Helm values or use `--set-file` for sensitive values.

### AWS Configuration

Both deployment methods require AWS credentials for S3 and Athena access.

**Required:**
- AWS Access Key ID
- AWS Secret Access Key
- S3 Bucket Name
- S3 Athena Output location

**Optional:**
- AWS Session Token (for temporary credentials, e.g., STS tokens)
- AWS Region (defaults to us-east-1)

**IAM Permissions Required:**
- S3: Read/Write access to the specified bucket
- Athena: Query execution permissions
- Glue: Catalog access (if using AWS Glue Data Catalog)

**Kustomize:** Configure in `deploy/kubernetes/config/secrets.yaml`:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: aws-credentials
  namespace: logwise
type: Opaque
stringData:
  AWS_ACCESS_KEY_ID: "your-key-id"
  AWS_SECRET_ACCESS_KEY: "your-secret-key"
  AWS_SESSION_TOKEN: "your-session-token"  # Optional
  AWS_REGION: "us-east-1"
  S3_BUCKET_NAME: "your-bucket"
  S3_ATHENA_OUTPUT: "s3://your-bucket/athena-output/"
```

**Helm:** Configure via values or `--set`:
```bash
helm upgrade --install logwise . \
  --set aws.accessKeyId=YOUR_KEY \
  --set aws.secretAccessKey=YOUR_SECRET \
  --set aws.s3BucketName=your-bucket \
  --set aws.s3AthenaOutput=s3://your-bucket/athena-output/
```

### Database Configuration

Logwise uses two separate MySQL instances:

1. **Orchestrator DB** (`mysql-orch`)
   - Stores service metadata, job history, scaling decisions
   - Default database: `logwise_orch`
   - Default user: `logwise_orch`

2. **Grafana DB** (`mysql-grafana`)
   - Stores Grafana dashboards and metadata
   - Default database: `grafana`
   - Default user: `grafana`

**Kustomize:** Configure in `deploy/kubernetes/config/secrets.yaml`:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-orch-credentials
  namespace: logwise
type: Opaque
stringData:
  MYSQL_USER: "logwise_orch"
  MYSQL_PASSWORD: "your-password"
  MYSQL_ROOT_PASSWORD: "root-password"
  MYSQL_DATABASE: "logwise_orch"
```

**Helm:** Configure via values:
```yaml
mysql:
  orchestrator:
    username: logwise_orch
    password: your-password
    rootPassword: root-password
    database: logwise_orch
  grafana:
    username: grafana
    password: your-password
    rootPassword: root-password
    database: grafana
```

**Storage Options:**
- **Local Development**: `emptyDir` (ephemeral, data lost on pod restart)
- **Production**: `persistentVolumeClaim` (persistent storage)

### Resource Requirements

Resource requirements vary by environment:

**Minimum (Local Development):**
- **Cluster**: 4 CPU cores, 8 GiB RAM
- **Storage**: 20 GiB (ephemeral, emptyDir)
- **Components**: Reduced resource limits, single replicas

**Recommended (Non-Production):**
- **Cluster**: 8 CPU cores, 16 GiB RAM
- **Storage**: 50 GiB (can use ephemeral or persistent)
- **Components**: Standard resource limits, single replicas

**Production:**
- **Cluster**: 16+ CPU cores, 32+ GiB RAM
- **Storage**: 200+ GiB (persistent volumes required)
- **Components**: Higher resource limits, multiple replicas for HA
- **Kafka**: 3 replicas, 7-day log retention
- **Persistent Storage**: Required for MySQL and Kafka

**Component-Specific Resources:**

| Component | Local (requests) | Production (requests) |
|-----------|------------------|----------------------|
| Orchestrator | 250m CPU, 256Mi RAM | 500m CPU, 512Mi RAM |
| Spark Master | 500m CPU, 1Gi RAM | 1000m CPU, 2Gi RAM |
| Spark Worker | 500m CPU, 2Gi RAM | 2000m CPU, 3Gi RAM |
| Vector | 100m CPU, 128Mi RAM | 250m CPU, 256Mi RAM |
| Kafka | 250m CPU, 512Mi RAM | 1000m CPU, 1Gi RAM |
| MySQL (each) | 250m CPU, 256Mi RAM | 500m CPU, 512Mi RAM |
| Grafana | 250m CPU, 512Mi RAM | 500m CPU, 1Gi RAM |

---

## Accessing Services

This section applies to both Kustomize and Helm deployments.

### Service Ports

| Service | Internal Port | NodePort (if enabled) | Purpose |
|---------|---------------|---------------------|---------|
| Orchestrator | 8080 | 30081 | Main API and UI |
| Grafana | 3000 | 30080 | Dashboards and visualization |
| Spark Master | 8080 | 30082 | Spark Master UI |
| Spark Worker UI | 8081 | 30083 | Spark Worker UI |
| Vector API | 8686 | - | Vector management API |
| Vector OTLP gRPC | 4317 | 30418 | OTLP gRPC endpoint |
| Vector OTLP HTTP | 4318 | - | OTLP HTTP endpoint |
| Kafka | 9092 | - | Kafka broker |

### Local Development

#### Method 1: NodePort (kind/minikube)

If you created the cluster with NodePort mappings (see `kind-config.yaml`), services are accessible directly:

```
- **Grafana**: http://localhost:30080 (default: admin/admin)
- **Orchestrator**: http://localhost:30081
- **Spark Master**: http://localhost:30082
- **Spark Worker UI**: http://localhost:30083
```

#### Method 2: Port Forwarding

Set up port forwarding for each service:

```bash
# Grafana
kubectl port-forward -n logwise svc/grafana 30080:3000 &

# Orchestrator
kubectl port-forward -n logwise svc/orchestrator 30081:8080 &

# Spark Master
kubectl port-forward -n logwise svc/spark-master 30082:8080 &

# Spark Worker UI
kubectl port-forward -n logwise svc/spark-worker-ui 30083:8081 &
```

**Helm users:** Use the post-install script:
```bash
cd deploy/kubernetes/helm/logwise
./post-install.sh
```

This script sets up port forwarding for all services automatically.

### Production/Remote Clusters

#### Method 1: Ingress (Recommended)

Configure ingress in your values/overlay:

**Helm:**
```yaml
ingress:
  enabled: true
  className: nginx
  orchestrator:
    host: orchestrator.prod.example.com
  grafana:
    host: grafana.prod.example.com
```

**Kustomize:** Add ingress manifests to your overlay.

Then access via:
- `https://grafana.prod.example.com`
- `https://orchestrator.prod.example.com`

#### Method 2: LoadBalancer

**Helm:** Set `services.type: LoadBalancer` in values file.

**Kustomize:** Configure LoadBalancer services in your overlay.

Use the external IPs provided by your cloud provider.

#### Method 3: Port Forwarding

Same as local development, but connect to your remote cluster:

```bash
# Set up kubectl context for remote cluster first
kubectl config use-context your-remote-cluster

# Then use port forwarding as shown above
```

### Default Credentials

- **Grafana**: 
  - Username: `admin`
  - Password: `admin` (change in production!)
  
- **MySQL**: Configured via secrets (see [Database Configuration](#database-configuration))

### Verifying Service Access

```bash
# Check service endpoints
kubectl get endpoints -n logwise

# Test service connectivity
kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
  curl http://orchestrator:8080/health

# Check service URLs
kubectl get svc -n logwise
```

---

## Upgrading

### Kustomize

#### Updating Images

1. **Using kustomize edit (recommended):**
   ```bash
   cd deploy/kubernetes/overlays/prod
   kustomize edit set image logwise-orchestrator=ghcr.io/your-org/logwise-orchestrator:v1.1.0
   kustomize edit set image logwise-spark=ghcr.io/your-org/logwise-spark:v1.1.0
   kustomize edit set image logwise-vector=ghcr.io/your-org/logwise-vector:v1.1.0
   kustomize edit set image logwise-healthcheck-dummy=ghcr.io/your-org/logwise-healthcheck-dummy:v1.1.0
   kubectl apply -k .
   ```

2. **Using environment variables:**
   ```bash
   cd deploy/kubernetes/overlays/prod
   REGISTRY=ghcr.io/your-org TAG=v1.1.0 kustomize build . | kubectl apply -f -
   ```

3. **Editing kustomization.yaml directly:**
   ```bash
   # Edit images section in kustomization.yaml
   cd deploy/kubernetes/overlays/prod
   # Update image tags
   kubectl apply -k .
   ```

#### Updating Configuration

1. **Update patches:**
   ```bash
   cd deploy/kubernetes/overlays/prod
   # Edit patch files as needed
   kubectl apply -k .
   ```

2. **Update base resources:**
   ```bash
   cd deploy/kubernetes/base
   # Update base manifests
   cd ../overlays/prod
   kubectl apply -k .
   ```

#### Verifying Upgrades

```bash
# Check pod status
kubectl get pods -n logwise

# Check rollout status
kubectl rollout status deployment/orchestrator -n logwise

# View recent changes
kubectl get events -n logwise --sort-by='.lastTimestamp'
```

### Helm

#### Upgrading Images

1. **Upgrade with new image tags (recommended):**
   ```bash
   cd deploy/kubernetes/helm/logwise
   helm upgrade --install logwise . \
     --namespace logwise \
     --values values-nonprod.yaml \
     --set global.imageRegistry=your-username \
     --set 'global.imagePullSecrets[0]=dockerhub-secret' \
     --set orchestrator.image.tag=1.0.1 \
     --set spark.master.image.tag=1.0.1 \
     --set spark.worker.image.tag=1.0.1 \
     --set vector.image.tag=1.0.1 \
     --set healthcheck.image.tag=1.0.1 \
     --force
   ```
   
   **Note:** Use `--upgrade --install` to handle both new installations and upgrades gracefully.

2. **Upgrade with new values file:**
   ```bash
   helm upgrade --install logwise . \
     --namespace logwise \
     --values values-prod.yaml \
     --reuse-values  # Keep existing values not in new file
   ```

#### Managing Upgrades

1. **Check upgrade status:**
   ```bash
   helm status logwise -n logwise
   kubectl get pods -n logwise
   helm list -n logwise
   ```

2. **View current values:**
   ```bash
   helm get values logwise -n logwise
   helm get all logwise -n logwise
   ```

3. **View upgrade history:**
   ```bash
   helm history logwise -n logwise
   ```

4. **Rollback if needed:**
   ```bash
   # Rollback to previous revision
   helm rollback logwise -n logwise
   
   # Rollback to specific revision
   helm rollback logwise 2 -n logwise
   
   # Verify rollback
   helm status logwise -n logwise
   kubectl get pods -n logwise
   ```

#### Upgrade Best Practices

- **Test in non-production first**: Always test upgrades in a non-production environment
- **Backup databases**: Before upgrading, backup MySQL databases
- **Gradual rollout**: Consider upgrading components one at a time
- **Monitor during upgrade**: Watch pod logs and metrics during upgrade
- **Keep values files**: Version control your values files for reproducibility

---

## Troubleshooting

This section covers common issues and solutions for both Kustomize and Helm deployments.

### Quick Diagnostic Commands

Run these commands to get an overview of your deployment:

```bash
# Check all resources
kubectl get all -n logwise

# Check pod status
kubectl get pods -n logwise -o wide

# Check events (recent issues)
kubectl get events -n logwise --sort-by='.lastTimestamp'

# Check resource usage
kubectl top pods -n logwise
kubectl top nodes

# Check services and endpoints
kubectl get svc,endpoints -n logwise
```

### Pods Not Starting

#### Diagnostic Steps

1. **Check pod status:**
   ```bash
   kubectl get pods -n logwise
   kubectl describe pod <pod-name> -n logwise
   ```

2. **Check logs:**
   ```bash
   # Current logs
   kubectl logs <pod-name> -n logwise
   
   # Previous container (if restarted)
   kubectl logs <pod-name> -n logwise --previous
   
   # Follow logs in real-time
   kubectl logs -f <pod-name> -n logwise
   
   # Logs from all containers in pod
   kubectl logs <pod-name> -n logwise --all-containers=true
   ```

3. **Check events:**
   ```bash
   kubectl describe pod <pod-name> -n logwise | grep -A 10 Events
   ```

#### Common Pod States and Solutions

| State | Cause | Solution |
|-------|-------|----------|
| **ImagePullBackOff** | Cannot pull image | Check registry credentials, image exists, network access |
| **ErrImagePull** | Image pull error | Verify image name/tag, check image pull secrets |
| **CrashLoopBackOff** | Container crashes on start | Check application logs, verify configuration |
| **Pending** | Cannot schedule pod | Check resource availability, node capacity, PVCs |
| **Init:0/1** | Init container failing | Check init container logs |
| **Running** | Pod is healthy | No action needed |

#### Specific Component Issues

**Orchestrator:**
```bash
# Check orchestrator logs
kubectl logs -n logwise -l app=orchestrator

# Check if dependencies are ready
kubectl get pods -n logwise -l app=mysql-orch
kubectl get pods -n logwise -l app=kafka
kubectl get pods -n logwise -l app=spark-master
```

**Vector:**
```bash
# Check Vector logs
kubectl logs -n logwise -l app=vector-logs

# Check Vector API
kubectl port-forward -n logwise svc/vector-logs 8686:8686
curl http://localhost:8686/health
```

**Spark:**
```bash
# Check Spark Master
kubectl logs -n logwise -l app=spark-master

# Check Spark Workers
kubectl logs -n logwise -l app=spark-worker

# Check if workers can connect to master
kubectl exec -n logwise <spark-worker-pod> -- \
  curl http://spark-master:8080
```

### Services Not Accessible

#### Diagnostic Steps

1. **Check service and endpoints:**
   ```bash
   # List all services
   kubectl get svc -n logwise
   
   # Check if endpoints exist (should show pod IPs)
   kubectl get endpoints -n logwise
   
   # Detailed service info
   kubectl describe svc <service-name> -n logwise
   ```

2. **Verify pods are running:**
   ```bash
   # Services need running pods to have endpoints
   kubectl get pods -n logwise -l app=<component-name>
   ```

3. **Test service connectivity from within cluster:**
   ```bash
   # Run a debug pod
   kubectl run -it --rm debug --image=curlimages/curl --restart=Never -- \
     curl http://orchestrator:8080/health
   
   # Or use existing pod
   kubectl exec -n logwise <pod-name> -- \
     curl http://orchestrator:8080/health
   ```

#### Port Forwarding Issues

1. **Verify port forwarding:**
   ```bash
   # Check if port forwarding is running
   ps aux | grep "kubectl port-forward"
   
   # Check if port is in use
   lsof -i :30080  # For Grafana
   lsof -i :30081  # For Orchestrator
   ```

2. **Restart port forwarding:**
   ```bash
   # Kill existing port forwards
   pkill -f "kubectl port-forward"
   
   # Restart port forwarding
   kubectl port-forward -n logwise svc/grafana 30080:3000 &
   kubectl port-forward -n logwise svc/orchestrator 30081:8080 &
   ```

#### Ingress Issues

1. **Check ingress:**
   ```bash
   kubectl get ingress -n logwise
   kubectl describe ingress -n logwise
   ```

2. **Verify ingress controller:**
   ```bash
   # Check if ingress controller is running
   kubectl get pods -n ingress-nginx  # For nginx ingress
   
   # Check ingress controller logs
   kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller
   ```

3. **Test ingress connectivity:**
   ```bash
   # Get ingress IP
   kubectl get ingress -n logwise
   
   # Test from outside cluster
   curl -H "Host: grafana.prod.example.com" http://<ingress-ip>
   ```

### Database Connection Issues

#### Diagnostic Steps

1. **Check MySQL pods:**
   ```bash
   # Check orchestrator MySQL
   kubectl get pods -n logwise -l app=mysql-orch
   kubectl logs -n logwise -l app=mysql-orch
   
   # Check Grafana MySQL
   kubectl get pods -n logwise -l app=mysql-grafana
   kubectl logs -n logwise -l app=mysql-grafana
   ```

2. **Verify MySQL is ready:**
   ```bash
   # Test connection from orchestrator pod
   kubectl exec -n logwise <orchestrator-pod> -- \
     mysql -h mysql-orch -u logwise_orch -p<password> -e "SELECT 1"
   ```

3. **Verify secrets:**
   ```bash
   # List secrets
   kubectl get secrets -n logwise | grep mysql
   
   # Check secret exists (don't print values)
   kubectl get secret db-orch-credentials -n logwise
   kubectl get secret db-grafana-credentials -n logwise
   ```

4. **Check database initialization:**
   ```bash
   # Check init container logs
   kubectl logs <mysql-pod> -n logwise -c init-mysql
   
   # Check if database exists
   kubectl exec -n logwise <mysql-pod> -- \
     mysql -u root -p<root-password> -e "SHOW DATABASES;"
   ```

#### Common MySQL Issues

- **Connection refused**: MySQL pod not ready, check logs
- **Access denied**: Wrong credentials, verify secrets
- **Database doesn't exist**: Init container failed, check init logs
- **Can't connect to host**: Service name incorrect, verify service DNS

### Kafka Connection Issues

#### Diagnostic Steps

1. **Check Kafka pod:**
   ```bash
   kubectl get pods -n logwise -l app=kafka
   kubectl logs -n logwise -l app=kafka
   ```

2. **Verify Kafka is accessible:**
   ```bash
   # Test from Vector pod
   kubectl exec -n logwise <vector-pod> -- \
     nc -zv kafka-bootstrap 9092
   
   # Or use telnet
   kubectl exec -n logwise <vector-pod> -- \
     telnet kafka-bootstrap 9092
   ```

3. **Verify bootstrap servers configuration:**
   ```bash
   # Check ConfigMap
   kubectl get configmap logwise-config -n logwise -o yaml
   
   # Check environment variables in pods
   kubectl exec -n logwise <vector-pod> -- env | grep KAFKA
   kubectl exec -n logwise <orchestrator-pod> -- env | grep KAFKA
   ```

4. **Check Kafka topics:**
   ```bash
   # List topics (if kafka CLI available)
   kubectl exec -n logwise <kafka-pod> -- \
     kafka-topics --bootstrap-server localhost:9092 --list
   ```

#### Common Kafka Issues

- **Connection timeout**: Kafka not ready, check pod status
- **Bootstrap server not found**: DNS issue, verify service name
- **Topic doesn't exist**: Auto-create disabled or permissions issue
- **Broker not available**: Kafka pod crashed, check logs

### Helm Deployment Issues

**Namespace Ownership Errors:**

If you see: `invalid ownership metadata; label validation error: missing key "app.kubernetes.io/managed-by"`

**Solution:**
```bash
# Adopt the namespace for Helm management
kubectl label namespace logwise app.kubernetes.io/managed-by=Helm --overwrite
kubectl annotate namespace logwise meta.helm.sh/release-name=logwise --overwrite
kubectl annotate namespace logwise meta.helm.sh/release-namespace=logwise --overwrite
```

**Release Not Found Errors:**

If you see: `"logwise" has no deployed releases`

**Solution:**
Use `--upgrade --install` instead of just `upgrade`:
```bash
helm upgrade --install logwise . \
  --namespace logwise \
  --values values-nonprod.yaml \
  # ... other flags
```

**Deployment Conflict Errors:**

If you see: `the object has been modified; please apply your changes to the latest version`

**Solution:**
```bash
# Option 1: Use --force flag
helm upgrade --install logwise . --force # ... other flags

# Option 2: Delete conflicting resources
kubectl delete deployment orchestrator -n logwise --wait=false
# Then retry helm upgrade

# Option 3: Clean reinstall (if safe)
helm uninstall logwise -n logwise
# Wait a moment, then reinstall
helm install logwise . # ... flags
```

### Resource Constraints

If pods are being evicted or not scheduling:

#### Diagnostic Steps

1. **Check node resources:**
   ```bash
   # Check node capacity and allocatable
   kubectl describe node
   
   # Check resource usage
   kubectl top nodes
   kubectl top pods -n logwise
   ```

2. **Check pod resource requests:**
   ```bash
   # Check what resources pods are requesting
   kubectl describe pod <pod-name> -n logwise | grep -A 5 "Requests:"
   ```

3. **Check for evicted pods:**
   ```bash
   # List all pods including evicted
   kubectl get pods -n logwise --field-selector=status.phase!=Running
   
   # Check eviction events
   kubectl get events -n logwise --field-selector=reason=Evicted
   ```

#### Solutions

1. **Reduce resource requests/limits:**
   - **Helm**: Edit `values.yaml` or use `--set` flags
   - **Kustomize**: Update resource patches in overlay

2. **Scale down other workloads** or add more nodes to cluster

3. **Check for resource quotas:**
   ```bash
   kubectl get resourcequota -n logwise
   kubectl describe resourcequota -n logwise
   ```

4. **Increase cluster capacity** (add nodes or upgrade node types)

### Docker Hub Rate Limit Issues

**Symptoms:**
- `429 Too Many Requests` errors when pulling images
- `toomanyrequests: You have reached your unauthenticated pull rate limit`
- `Unable to retrieve some image pull secrets`

**Solutions:**

1. **Verify Docker Hub secret exists and is correct:**
   ```bash
   kubectl get secret dockerhub-secret -n logwise
   kubectl describe secret dockerhub-secret -n logwise
   ```

2. **Recreate the secret with correct format:**
   ```bash
   kubectl delete secret dockerhub-secret -n logwise
   kubectl create secret docker-registry dockerhub-secret \
     --docker-server=docker.io \
     --docker-username=your-username \
     --docker-password=your-password-or-token \
     --docker-email=your-email@example.com \
     --namespace=logwise
   ```

3. **Verify Helm is using the secret name (not password):**
   ```bash
   # Check current Helm values
   helm get values logwise -n logwise | grep imagePullSecrets
   # Should show: dockerhub-secret (not your password)
   ```

4. **Upgrade Helm release to ensure secret is applied:**
   ```bash
   helm upgrade --install logwise . \
     --namespace logwise \
     --values values-nonprod.yaml \
     --set 'global.imagePullSecrets[0]=dockerhub-secret' \
     --force
   ```

5. **Restart pods to use the secret:**
   ```bash
   kubectl delete pod -n logwise --all
   # Or restart specific deployments
   kubectl rollout restart deployment orchestrator -n logwise
   kubectl rollout restart deployment vector-logs -n logwise
   ```

6. **Verify pods are using the secret:**
   ```bash
   kubectl get pod -n logwise -l app=orchestrator -o jsonpath='{.items[0].spec.imagePullSecrets[*].name}'
   # Should output: dockerhub-secret
   ```

**Prevention:**
- Always use Docker Hub access tokens instead of passwords
- Create the secret before deploying
- Use `--set 'global.imagePullSecrets[0]=dockerhub-secret'` (secret name, not password)

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

1. **Vector**: HPA enabled by default (Helm) or can be added (Kustomize)
2. **Spark Workers**: Increase replicas
3. **Kafka**: Increase replicas (requires proper configuration)
4. **Orchestrator**: Increase replicas (ensure shared state handling)

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
