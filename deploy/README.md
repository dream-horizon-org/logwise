# LogWise Deployment Guide

This directory contains all deployment configurations and scripts for LogWise, supporting both Docker Compose (local development) and Kubernetes (production) deployments.

## Directory Structure

```
deploy/
├── docker/              # Docker Compose configurations for local development
├── kubernetes/          # Kubernetes manifests and overlays
├── shared/              # Shared configuration and utilities
├── ci-cd/               # CI/CD workflows and scripts
└── docs/                # Additional documentation
```

## Quick Start

### Docker (Local Development)

For local development and testing:

```bash
cd deploy/docker
cp ../shared/templates/env.template .env
# Edit .env with your AWS credentials
make setup
```

For Docker Compose setup, see the Docker Compose configuration in `docker/` directory.

### Kubernetes (Production)

For Kubernetes deployments:

```bash
# Local (kind cluster)
./kubernetes/scripts/setup-k8s.sh local

# Non-production
ENV=nonprod REGISTRY=your-registry TAG=1.0.0 \
  ./kubernetes/scripts/build-and-push.sh
ENV=nonprod ./kubernetes/scripts/deploy.sh

# Production
ENV=prod REGISTRY=your-registry TAG=1.0.0 \
  ./kubernetes/scripts/build-and-push.sh
ENV=prod ./kubernetes/scripts/deploy.sh
```

See [Kubernetes Setup Guide](docs/kubernetes-setup.md) for detailed instructions.

## Architecture Overview

LogWise consists of the following components:

- **Vector**: Log collection and forwarding to Kafka
- **Kafka**: Message broker for log streaming
- **Spark**: Stream processing and data transformation
- **Orchestrator**: Service coordination and job management
- **Grafana**: Visualization and dashboards
- **MySQL**: Database for orchestrator and Grafana

## Deployment Options

### Docker Compose

Best for:
- Local development
- Quick testing
- Single-machine deployments

For Docker Compose setup, see the Docker Compose configuration in `docker/` directory.

### Kubernetes

Best for:
- Production deployments
- High availability
- Scalability
- Multi-environment management

See [Kubernetes Setup Guide](docs/kubernetes-setup.md).

## Configuration

### Shared Configuration

Common configuration values are defined in `shared/config/`:

- `defaults.yaml`: Default values for all services
- `env-mapping.yaml`: Environment variable mappings
- `image-registry.yaml`: Image registry configurations

### Environment Variables

**For Docker**: Create `.env` in `deploy/docker/`:
```bash
cd deploy/docker
cp ../shared/templates/env.template .env
# Edit .env with your configuration
```

**For Kubernetes**: Create a separate `.env` file (Docker and Kubernetes configurations are independent):
```bash
# Create Kubernetes-specific .env file
cp shared/templates/env.template kubernetes/.env
# Edit kubernetes/.env with your configuration

# Sync to Kubernetes manifests
./kubernetes/scripts/sync-config.sh kubernetes/.env

# Or use helper script
./scripts/create-env.sh --kubernetes
```

See the Configuration section in [Kubernetes Setup Guide](docs/kubernetes-setup.md) for environment file configuration.

Required variables:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_REGION`
- `S3_BUCKET_NAME`
- `S3_ATHENA_OUTPUT`
- `ATHENA_WORKGROUP`
- `ATHENA_DATABASE`

## Secrets Management

**IMPORTANT**: Never commit secrets to version control.

### Docker

Secrets are managed via `.env` file (not committed).

### Kubernetes

For production, use one of these approaches:

1. **External Secrets Operator** with AWS Secrets Manager
2. **HashiCorp Vault**
3. **Sealed Secrets**
4. **Kubernetes Secrets** (encrypted at rest)

See `kubernetes/config/secrets.example.yaml` for examples.

## CI/CD

GitHub Actions workflows are available in `ci-cd/.github/workflows/`:

- `docker-build.yml`: Builds and pushes Docker images
- `k8s-deploy.yml`: Deploys to Kubernetes environments

See the workflows for configuration details.

## Validation

Validate configurations before deployment:

```bash
# Validate environment file
./shared/scripts/validation.sh docker/.env

# Validate Kubernetes manifests
./shared/scripts/validation.sh "" "" kubernetes/base

# Validate Docker Compose
./shared/scripts/validation.sh docker/.env docker/docker-compose.yml
```

## Troubleshooting

See the Troubleshooting section in [Kubernetes Setup Guide](docs/kubernetes-setup.md) for common issues and solutions.

## Production Checklist

Before deploying to production, review the Production Checklist section in [Kubernetes Setup Guide](docs/kubernetes-setup.md).

## Additional Documentation

- [Kubernetes Setup Guide](docs/kubernetes-setup.md) - Complete Kubernetes deployment guide with troubleshooting, production checklist, and configuration

## Support

For issues and questions:
1. Check the troubleshooting guide
2. Review the documentation
3. Open an issue in the repository
