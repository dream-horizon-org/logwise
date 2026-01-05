# Kubernetes Setup Guide

This guide covers deploying LogWise to Kubernetes clusters.

## Prerequisites

- Kubernetes cluster (1.24+)
- `kubectl` configured to access your cluster
- `kustomize` (included with kubectl 1.14+)
- Docker (for building images)
- Container registry access (for production)

## Quick Start

### Local Development (kind)

For local testing with kind:

```bash
./kubernetes/scripts/setup-k8s.sh local
```

This will:
1. Create a kind cluster
2. Build images
3. Deploy to the cluster
4. Set up port forwarding

### Non-Production

```bash
# Build and push images
ENV=nonprod REGISTRY=your-registry TAG=1.0.0 \
  ./kubernetes/scripts/build-and-push.sh

# Deploy
ENV=nonprod ./kubernetes/scripts/deploy.sh
```

### Production

```bash
# Build and push images
ENV=prod REGISTRY=your-registry TAG=1.0.0 \
  ./kubernetes/scripts/build-and-push.sh

# Deploy (with validation)
ENV=prod ./kubernetes/scripts/deploy.sh
```

## Directory Structure

```
kubernetes/
├── base/              # Base Kubernetes manifests
├── overlays/          # Environment-specific overlays
│   ├── local/        # Local development
│   ├── nonprod/      # Non-production
│   └── prod/         # Production
├── scripts/          # Deployment scripts
└── config/           # Configuration files
```

## Configuration

### Environment Variables

Configuration is managed via ConfigMaps and Secrets. See `shared/config/` for default values.

### Secrets Management

**IMPORTANT**: Never commit actual secrets.

For local development, generate secrets from `.env`:

```bash
./shared/scripts/sync-config.sh docker/.env
```

For production, use:
- External Secrets Operator
- HashiCorp Vault
- AWS Secrets Manager
- Sealed Secrets

See `kubernetes/config/secrets.example.yaml` for examples.

### Image Registry

Configure image registry in `shared/config/image-registry.yaml` or via environment:

```bash
export REGISTRY=ghcr.io/your-org
export TAG=1.0.0
```

## Deployment Process

### 1. Build Images

```bash
./kubernetes/scripts/build-and-push.sh
```

Options:
- `REGISTRY`: Container registry (e.g., `ghcr.io/your-org`, `dockerhub`, `local`, or `docker` for local Docker)
- `TAG`: Image tag (default: `1.0.0`)
- `CLUSTER_TYPE`: `docker-desktop`, `kind`, or `other`
- `PARALLEL_BUILD`: `true` or `false` (default: `true`)

**Note**: When `REGISTRY` is set to `local` or `docker`, images will be built but not pushed to a remote registry. This is useful for local development with Docker Desktop or kind clusters.

### 2. Deploy

```bash
./kubernetes/scripts/deploy.sh
```

Options:
- `ENV`: Environment (`local`, `nonprod`, `prod`)
- `NAMESPACE`: Kubernetes namespace (default: `logwise`)
- `DRY_RUN`: `true` or `false` (default: `false`)
- `WAIT_TIMEOUT`: Timeout in seconds (default: `600`)

### 3. Verify

```bash
kubectl get pods -n logwise
kubectl get services -n logwise
kubectl logs -n logwise deployment/orchestrator
```

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

### Port Forwarding

```bash
# Orchestrator
kubectl port-forward -n logwise service/orchestrator 8080:8080

# Grafana
kubectl port-forward -n logwise service/grafana 3000:3000

# Spark Master UI
kubectl port-forward -n logwise service/spark-master 8080:8080
```

### Ingress

For nonprod/prod, configure Ingress in the overlay:

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

### Pods not starting

1. Check events: `kubectl describe pod -n logwise <pod-name>`
2. Check logs: `kubectl logs -n logwise <pod-name>`
3. Verify secrets: `kubectl get secrets -n logwise`
4. Check resource limits: `kubectl describe node`

### Image pull errors

1. Verify image exists: `docker pull <image>`
2. Check registry credentials
3. For kind: `kind load docker-image <image>`

### Configuration issues

1. Verify ConfigMap: `kubectl get configmap -n logwise logwise-config -o yaml`
2. Check environment variables: `kubectl exec -n logwise <pod> -- env`

## Rollback

Rollback to previous deployment:

```bash
kubectl rollout undo deployment/orchestrator -n logwise
kubectl rollout history deployment/orchestrator -n logwise
```

## Cleanup

Remove deployment:

```bash
# Delete namespace (removes everything)
kubectl delete namespace logwise

# Or use destroy script
./kubernetes/scripts/destroy-k8s.sh nonprod
```

## Next Steps

- Review [Production Checklist](production-checklist.md)
- Set up monitoring and alerting
- Configure backup procedures
- Review security best practices

