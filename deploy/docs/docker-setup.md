# Docker Setup Guide

This guide covers setting up LogWise using Docker Compose for local development.

## Prerequisites

- Docker Desktop or Docker Engine with Docker Compose v2
- Make (optional, for convenience commands)
- AWS credentials (for S3 and Athena)

## Quick Start

1. **Navigate to docker directory**:
   ```bash
   cd deploy/docker
   ```

2. **Create environment file**:
   ```bash
   cp ../shared/templates/env.template .env
   ```

3. **Edit `.env` file** with your AWS credentials and configuration:
   ```bash
   # Required: AWS credentials
   AWS_ACCESS_KEY_ID=your-access-key
   AWS_SECRET_ACCESS_KEY=your-secret-key
   AWS_REGION=us-east-1
   
   # Required: S3 and Athena configuration
   S3_BUCKET_NAME=your-bucket-name
   S3_ATHENA_OUTPUT=s3://your-bucket/athena-output/
   ATHENA_WORKGROUP=logwise
   ATHENA_DATABASE=logwise
   ```

4. **Run setup**:
   ```bash
   make setup
   ```

   Or manually:
   ```bash
   ./scripts/setup.sh
   ```

## Available Commands

Using Make (recommended):

```bash
make setup          # One-click setup (bootstrap, .env, start services)
make up             # Start all services
make down           # Stop all services
make logs           # View logs from all services
make ps             # Show service status
make teardown       # Remove all containers and volumes
```

Using Docker Compose directly:

```bash
docker compose -f docker-compose.yml up -d    # Start services
docker compose -f docker-compose.yml down     # Stop services
docker compose -f docker-compose.yml logs -f  # View logs
docker compose -f docker-compose.yml ps       # Show status
```

## Service Ports

After startup, services are available at:

- **Grafana**: http://localhost:3000 (admin/admin)
- **Orchestrator**: http://localhost:8080
- **Spark Master UI**: http://localhost:18080
- **Vector API**: http://localhost:8686
- **Vector OTLP gRPC**: localhost:4317
- **Vector OTLP HTTP**: http://localhost:4318

## Configuration

### Environment Variables

All configuration is done via the `.env` file. See `shared/templates/env.template` for all available options.

### Port Conflicts

If ports are already in use, you can override them in `.env`:

```bash
GRAFANA_PORT=3001
ORCH_PORT=8081
VECTOR_API_PORT=8687
```

### Resource Limits

Default resource limits are set in `docker-compose.yml`. Adjust if needed:

```yaml
services:
  orchestrator:
    mem_limit: 1g
    cpus: "1.00"
```

## Health Checks

Services include health checks. Check status:

```bash
docker compose -f docker-compose.yml ps
```

All services should show "healthy" status.

## Troubleshooting

### Services won't start

1. Check Docker resources (memory/CPU)
2. Verify ports are not in use
3. Check logs: `docker compose -f docker-compose.yml logs <service-name>`

### Database connection issues

1. Wait for MySQL to be healthy
2. Check database credentials in `.env`
3. Verify network connectivity

### AWS connection issues

1. Verify AWS credentials in `.env`
2. Check AWS region is correct
3. Verify S3 bucket exists and is accessible

### Port conflicts

1. Identify what's using the port: `lsof -i :8080`
2. Stop the conflicting service or change the port in `.env`

## Data Persistence

Data is persisted in Docker volumes:

- `kafka_data`: Kafka logs and data
- `mysql_data`: Orchestrator database
- `grafana_data`: Grafana data
- `spark_checkpoint`: Spark checkpoints
- `db_data`: Database files

To remove all data:

```bash
make teardown
```

## Development Workflow

1. Make changes to code
2. Rebuild specific service: `docker compose -f docker-compose.yml build <service>`
3. Restart service: `docker compose -f docker-compose.yml up -d <service>`
4. View logs: `docker compose -f docker-compose.yml logs -f <service>`

## Next Steps

- Configure Grafana dashboards
- Set up log ingestion via Vector
- Monitor Spark jobs
- Review [Production Checklist](../docs/production-checklist.md) for production deployment

