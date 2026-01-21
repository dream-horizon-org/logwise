# Production Overlay

This overlay contains production-specific configurations for LogWise.

## Features

- **High Availability**: Multiple replicas with pod anti-affinity
- **Resource Limits**: Increased CPU and memory allocations
- **Pod Disruption Budgets**: Ensures minimum availability during updates
- **Managed Services**: Configurations for using managed Kafka (if applicable)

## Secrets Management

**IMPORTANT**: Do not store actual secrets in this repository.

For production, use one of these approaches:

1. **External Secrets Operator** with AWS Secrets Manager
2. **HashiCorp Vault**
3. **Sealed Secrets**
4. **Kubernetes Secrets** (encrypted at rest)

See `deploy/kubernetes/config/secrets.example.yaml` for examples.

## Deployment

```bash
# Build and push images
ENV=prod REGISTRY=your-registry TAG=1.0.0 \
  ./deploy/kubernetes/scripts/build-and-push.sh

# Deploy to production
ENV=prod ./deploy/kubernetes/scripts/deploy.sh
```

## Pre-deployment Checklist

- [ ] Secrets are configured in your secret management system
- [ ] Resource limits are appropriate for your cluster
- [ ] Pod disruption budgets are set correctly
- [ ] Ingress is configured with proper TLS certificates
- [ ] Monitoring and alerting are set up
- [ ] Backup procedures are in place
- [ ] Rollback plan is documented


