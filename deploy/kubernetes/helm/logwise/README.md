# Logwise Helm Chart

A comprehensive Helm chart for deploying the complete Logwise stack on Kubernetes. This chart includes all components needed for log collection, processing, storage, and visualization.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Installation Methods](#installation-methods)
- [Configuration](#configuration)
- [Components](#components)
- [Environment-Specific Values](#environment-specific-values)
- [Scripts](#scripts)
- [Accessing Services](#accessing-services)
- [Upgrading](#upgrading)
- [Uninstalling](#uninstalling)
- [Troubleshooting](#troubleshooting)
- [Production Deployment](#production-deployment)

## Overview

The Logwise Helm chart deploys the following components:

- **Orchestrator**: Service coordination and job management
- **Spark**: Stream processing and data transformation (Master + Workers)
- **Vector**: Log collection and forwarding to Kafka
- **Kafka**: Message broker for log streaming
- **Grafana**: Visualization and dashboards
- **MySQL**: Databases for orchestrator and Grafana
- **OTEL Collector**: OpenTelemetry log collection
- **Healthcheck Dummy**: Test service for generating sample logs
- **Cron Jobs**: Scheduled tasks for orchestrator synchronization

## Prerequisites

Before installing the Logwise Helm chart, ensure you have:

1. **Kubernetes Cluster**: A running Kubernetes cluster (1.20+)
   - For local development: [kind](https://kind.sigs.k8s.io/), [minikube](https://minikube.sigs.k8s.io/), or Docker Desktop Kubernetes
   - For production: EKS, GKE, AKS, or any managed Kubernetes service

2. **Helm**: Version 3.8+ installed
   ```bash
   helm version
   ```

3. **kubectl**: Configured to access your cluster
   ```bash
   kubectl cluster-info
   ```

4. **Docker** (for local development): If building images locally

5. **AWS Credentials** (optional): If using S3/Athena for log storage
   - AWS Access Key ID
   - AWS Secret Access Key
   - S3 Bucket Name
   - S3 Athena Output location

## Quick Start

### Local Development (kind cluster)

1. **Set up a kind cluster** (optional, if you don't have one):
   ```bash
   # Option 1: Use Helm wrapper (recommended)
   cd deploy/kubernetes/helm/logwise
   ./setup-kind-cluster.sh
   
   # Option 2: Use common script directly
   cd deploy/kubernetes
   KIND_CLUSTER_NAME=logwise ./scripts/setup-kind-cluster.sh
   ```
   
   **Note:** Both methods use the same common script. The Helm wrapper sets `logwise` as the default cluster name.

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
     --values values-local.yaml
   ```

3. **Set up port forwarding** (if not using kind with port mappings):
   ```bash
   ./post-install.sh
   ```

4. **Access services**:
   - Orchestrator: http://localhost:30081
   - Grafana: http://localhost:30080 (admin/admin)
   - Spark Master: http://localhost:30082
   - Spark Worker UI: http://localhost:30083

### Production

```bash
helm install logwise . \
  --namespace logwise \
  --create-namespace \
  --values values-prod.yaml \
  --set aws.accessKeyId=YOUR_KEY \
  --set aws.secretAccessKey=YOUR_SECRET \
  --set aws.s3BucketName=your-bucket \
  --set aws.s3AthenaOutput=s3://bucket/athena-output/
```

## Installation Methods

### Method 1: Quick Install Script (Recommended for Local)

The `quick-install.sh` script automatically detects if a release exists and performs install or upgrade:

```bash
./quick-install.sh [AWS_ACCESS_KEY] [AWS_SECRET] [S3_BUCKET] [S3_ATHENA_OUTPUT] [AWS_SESSION_TOKEN]
```

Note: `AWS_SESSION_TOKEN` is optional and only needed for temporary credentials (e.g., STS tokens).

**Features:**
- Automatically creates namespace if needed
- Detects existing release and upgrades if present
- Handles AWS credentials as command-line arguments
- Simple and fast for local development

### Method 2: Install/Upgrade Script (Interactive)

The `install-or-upgrade.sh` script provides an interactive installation experience:

```bash
./install-or-upgrade.sh [AWS_ACCESS_KEY] [AWS_SECRET] [S3_BUCKET] [S3_ATHENA_OUTPUT]
```

**Features:**
- Interactive prompts for AWS credentials if not provided
- Confirmation before installation
- Automatic port forwarding setup option
- Better for first-time installations

### Method 3: Manual Helm Commands

For full control, use Helm commands directly:

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

## Configuration

### Values Files

The chart includes multiple values files for different environments:

- **`values.yaml`**: Base configuration with all default values
- **`values-local.yaml`**: Local development overrides (NodePort services, reduced resources)
- **`values-nonprod.yaml`**: Non-production environment overrides
- **`values-prod.yaml`**: Production environment overrides (higher resources, persistent volumes)

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

You can enable or disable individual components:

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
  autoPortForward: false  # Auto-setup port forwarding after install
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

## Components

### Orchestrator

The orchestrator service coordinates jobs and manages the log processing pipeline.

**Configuration:**
```yaml
orchestrator:
  enabled: true
  image:
    repository: logwise-orchestrator
    tag: latest
  replicas: 1
  resources:
    requests:
      cpu: "250m"
      memory: "256Mi"
    limits:
      cpu: "500m"
      memory: "512Mi"
```

**Access:** http://localhost:30081 (NodePort) or via ingress

### Spark

Apache Spark for stream processing with Master and Worker components.

**Configuration:**
```yaml
spark:
  master:
    enabled: true
    replicas: 1
    resources:
      requests:
        cpu: "500m"
        memory: "1Gi"
  worker:
    enabled: true
    replicas: 2
    memory: "2g"
    cores: "2"
```

**Access:**
- Master UI: http://localhost:30082
- Worker UI: http://localhost:30083

### Vector

High-performance log collection and forwarding agent.

**Configuration:**
```yaml
vector:
  enabled: true
  replicas: 1
  hpa:
    enabled: true
    minReplicas: 1
    maxReplicas: 5
    targetCPUUtilization: 70
```

**Features:**
- Horizontal Pod Autoscaler (HPA) support
- OTLP endpoints for log ingestion
- API endpoint for configuration management

### Kafka

Message broker for log streaming (can use external Kafka or in-cluster).

**Configuration:**
```yaml
kafka:
  enabled: true
  bootstrapServers: "kafka-bootstrap.logwise.svc.cluster.local:9092"
  replicas: 1
  storage:
    type: emptyDir  # or persistentVolumeClaim for production
    size: "10Gi"
```

**External Kafka:** Set `kafka.bootstrapServers` to your external Kafka cluster.

### Grafana

Visualization and dashboards for log analysis.

**Configuration:**
```yaml
grafana:
  enabled: true
  admin:
    user: admin
    password: admin  # Change in production!
  plugins: "grafana-athena-datasource,yesoreyeram-infinity-datasource"
```

**Access:** http://localhost:30080 (default credentials: admin/admin)

**Pre-configured:**
- Athena datasource for querying S3 logs
- Infinity datasource for various data sources
- Application logs dashboard

### MySQL

Separate MySQL instances for orchestrator and Grafana.

**Configuration:**
```yaml
mysql:
  orchestrator:
    enabled: true
    username: logwise_orch
    password: change-me
    database: logwise_orch
  grafana:
    enabled: true
    username: grafana
    password: change-me
    database: grafana
```

**Storage:** Use `persistentVolumeClaim` for production to persist data.

### OTEL Collector

OpenTelemetry collector for log collection.

**Configuration:**
```yaml
otel:
  enabled: true
  serviceName: "your-service-name"
  environment: "production"
  storage:
    type: hostPath  # or emptyDir
    hostPath: "/var/lib/otelcol"
```

### Healthcheck Dummy

Test service that generates sample logs for testing the pipeline.

**Configuration:**
```yaml
healthcheck:
  enabled: true
  serviceName: "healthcheck-dummy"
  environment: "local"
```

### Cron Jobs

Scheduled tasks for orchestrator synchronization.

**Configuration:**
```yaml
cron:
  enabled: true
  orchestratorSync:
    schedule: "*/1 * * * *"  # Every minute
    tenant: "ABC"
```

## Environment-Specific Values

### Local Development (`values-local.yaml`)

Optimized for local development with kind or minikube:

- **Services**: NodePort type for easy access
- **Resources**: Reduced CPU/memory limits
- **Storage**: emptyDir (ephemeral)
- **Ingress**: Disabled
- **Image Pull Policy**: IfNotPresent (uses local images)
- **Auto Port Forward**: Optional automatic port forwarding

**Usage:**
```bash
helm install logwise . --values values-local.yaml
```

### Non-Production (`values-nonprod.yaml`)

Configuration for staging/development environments:

- **Services**: ClusterIP (use ingress or port-forward)
- **Ingress**: Enabled with example hosts
- **Resources**: Standard configuration
- **Image Pull Policy**: Always (pulls from registry)

**Usage:**
```bash
helm install logwise . --values values-nonprod.yaml
```

### Production (`values-prod.yaml`)

Production-ready configuration:

- **Resources**: Higher CPU/memory limits
- **Replicas**: Increased for high availability
- **Storage**: Persistent volumes for data persistence
- **Kafka**: 3 replicas, 7-day log retention
- **Ingress**: Enabled with production hosts
- **Image Pull Policy**: Always

**Usage:**
```bash
helm install logwise . \
  --values values-prod.yaml \
  --set aws.accessKeyId=KEY \
  --set aws.secretAccessKey=SECRET
```

## Scripts

### `quick-install.sh`

Fast installation script for local development.

```bash
./quick-install.sh [AWS_KEY] [AWS_SECRET] [S3_BUCKET] [S3_ATHENA_OUTPUT] [AWS_SESSION_TOKEN]
```

Note: `AWS_SESSION_TOKEN` is optional and only needed for temporary credentials (e.g., STS tokens).

**Features:**
- Auto-detects install vs upgrade
- Creates namespace if needed
- Simple command-line interface

### `install-or-upgrade.sh`

Interactive installation script with prompts.

```bash
./install-or-upgrade.sh [AWS_KEY] [AWS_SECRET] [S3_BUCKET] [S3_ATHENA_OUTPUT]
```

**Features:**
- Interactive prompts for missing values
- Confirmation before installation
- Optional post-install port forwarding

### `post-install.sh`

Sets up port forwarding for local access.

```bash
./post-install.sh
```

**Ports forwarded:**
- Orchestrator: 30081
- Grafana: 30080
- Spark Master: 30082
- Spark Worker: 30083
- Vector OTLP: 30418

**Note:** Port forwards run in background. PIDs saved to `/tmp/logwise-pf-*.pid`

### `setup-kind-cluster.sh`

**Note:** This is a wrapper script that calls the common `scripts/setup-kind-cluster.sh` used by both Kustomize and Helm deployments.

Creates a kind cluster and loads Docker images.

```bash
./setup-kind-cluster.sh
```

**Features:**
- Creates kind cluster with port mappings (uses `kind-config.yaml` if available)
- Builds and loads Docker images
- Configures cluster for NodePort access
- Default cluster name: `logwise` (can be overridden with `KIND_CLUSTER_NAME`)

**Options:**
- `--no-build`: Skip building images (only load existing ones)
- `--no-load`: Skip loading images into kind
- `--skip-existing`: Don't prompt if cluster exists, just reuse it
- `-n, --name NAME`: Set cluster name
- `-c, --config PATH`: Use custom kind config file

The common script is located at `../../scripts/setup-kind-cluster.sh` and can be used directly for more control.

## Accessing Services

### Local Development (kind/minikube)

**Option 1: NodePort (with kind-config.yaml)**
If you created the kind cluster with `kind-config.yaml`, NodePort services are accessible directly:
- http://localhost:30080 (Grafana)
- http://localhost:30081 (Orchestrator)
- etc.

**Option 2: Port Forwarding**
```bash
./post-install.sh
# Or manually:
kubectl port-forward -n logwise svc/grafana 30080:3000
kubectl port-forward -n logwise svc/orchestrator 30081:8080
```

### Production/Remote Clusters

**Option 1: Ingress**
Configure ingress hosts in values file and access via domain names:
- https://grafana.prod.example.com
- https://orchestrator.prod.example.com

**Option 2: LoadBalancer**
Set `services.type: LoadBalancer` and use the external IPs provided.

**Option 3: Port Forwarding**
```bash
kubectl port-forward -n logwise svc/grafana 30080:3000
```

## Upgrading

### Upgrade Existing Release

```bash
helm upgrade logwise . \
  --namespace logwise \
  --values values-local.yaml \
  --set aws.accessKeyId=NEW_KEY
```

### Upgrade with New Values File

```bash
helm upgrade logwise . \
  --namespace logwise \
  --values values-prod.yaml \
  --reuse-values  # Keep existing values not in new file
```

### Check Upgrade Status

```bash
helm status logwise -n logwise
kubectl get pods -n logwise
```

## Uninstalling

### Uninstall Release

```bash
helm uninstall logwise -n logwise
```

### Uninstall and Delete Namespace

```bash
helm uninstall logwise -n logwise
kubectl delete namespace logwise
```

### Clean Up Port Forwards

```bash
pkill -f 'kubectl port-forward'
rm -f /tmp/logwise-pf-*.pid
```

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

2. **Reduce resources in values file:**
   ```yaml
   orchestrator:
     resources:
       requests:
         cpu: "100m"
         memory: "128Mi"
   ```

### AWS Credentials Issues

1. **Verify secrets:**
   ```bash
   kubectl get secret aws-credentials -n logwise -o jsonpath='{.data}' | base64 -d
   ```

2. **Check IAM permissions:**
   - S3 read/write access
   - Athena query execution
   - Glue catalog access (if using)

## Production Deployment

### Pre-Deployment Checklist

- [ ] Review and update `values-prod.yaml` with production values
- [ ] Set up persistent volumes for Kafka and MySQL
- [ ] Configure ingress with proper TLS certificates
- [ ] Set up AWS credentials using secrets management (not --set flags)
- [ ] Configure resource limits based on expected load
- [ ] Set up monitoring and alerting
- [ ] Configure backup strategy for databases
- [ ] Review security settings (RBAC, network policies)
- [ ] Test in non-production environment first

### Production Values Overrides

Create a custom values file for production:

```yaml
# values-production.yaml
global:
  imageRegistry: "your-registry.io/logwise"
  imagePullPolicy: Always

aws:
  region: us-east-1
  # Use secrets management, don't set here

services:
  type: ClusterIP

ingress:
  enabled: true
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
  orchestrator:
    host: orchestrator.yourdomain.com
  grafana:
    host: grafana.yourdomain.com

# Increase replicas for HA
spark:
  worker:
    replicas: 5

vector:
  replicas: 3
  hpa:
    enabled: true
    minReplicas: 3
    maxReplicas: 10

# Use persistent storage
kafka:
  storage:
    type: persistentVolumeClaim
    size: "100Gi"
  replicas: 3

mysql:
  orchestrator:
    storage:
      type: persistentVolumeClaim
      size: "50Gi"
  grafana:
    storage:
      type: persistentVolumeClaim
      size: "50Gi"
```

### Secrets Management

**DO NOT** use `--set` flags for secrets in production. Use one of:

1. **External Secrets Operator:**
   ```yaml
   # See secrets.example.yaml for configuration
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

### Monitoring

Set up monitoring for production:

1. **Prometheus metrics:** Components expose metrics endpoints
2. **Grafana dashboards:** Pre-configured dashboards available
3. **Log aggregation:** Use Vector to forward logs to your SIEM
4. **Alerting:** Configure alerts for:
   - Pod failures
   - High resource usage
   - Kafka lag
   - Database connection issues

### Backup Strategy

1. **MySQL backups:**
   ```bash
   # Use Velero or similar for PVC backups
   # Or set up MySQL dump cron jobs
   ```

2. **Kafka data:** Consider replication factor > 1 for production

3. **Configuration:** Version control all values files

### Scaling

The chart supports horizontal scaling:

1. **Vector:** HPA enabled by default
2. **Spark Workers:** Increase `spark.worker.replicas`
3. **Kafka:** Increase `kafka.replicas` (requires proper configuration)
4. **Orchestrator:** Increase `orchestrator.replicas` (ensure shared state handling)

### High Availability

For HA deployments:

1. **Multiple replicas:** Set replicas > 1 for critical components
2. **Pod Disruption Budgets:** Consider adding PDBs
3. **Anti-affinity rules:** Spread pods across nodes
4. **Persistent storage:** Use network-attached storage (EBS, EFS, etc.)
5. **Load balancers:** Use LoadBalancer or Ingress with multiple replicas

## Additional Resources

- [Main Deployment README](../../README.md)
- [Kubernetes Setup Guide](../../../docs/setup-guides/production-setup.md)
- [Component Documentation](../../../docs/components/)
- [Architecture Overview](../../../docs/architecture-overview.md)

## Support

For issues and questions:
1. Check the troubleshooting section above
2. Review component-specific documentation
3. Check Kubernetes and Helm logs
4. Open an issue in the repository

## License

See the main project LICENSE file.
