# Environment Files Guide

This guide explains where to place `.env` files for Docker and Kubernetes deployments.

## Quick Answer

**For Docker**: Place `.env` in `deploy/docker/.env`  
**For Kubernetes**: Use the same file OR create a separate one, then sync to Kubernetes

## Docker Deployment

### Location
```
deploy/docker/.env
```

### Setup
1. Copy the template:
   ```bash
   cd deploy/docker
   cp ../shared/templates/env.template .env
   ```

2. Edit `.env` with your values:
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

3. The `docker-compose.yml` automatically loads this file via `env_file: - .env`

### Usage
```bash
cd deploy/docker
make setup  # Creates .env if missing
make up     # Uses .env automatically
```

## Kubernetes Deployment

### Option 1: Use Docker's .env (Recommended for Development)

For local development, you can use the same `.env` file:

```bash
# Sync Docker .env to Kubernetes ConfigMaps/Secrets
./deploy/shared/scripts/sync-config.sh deploy/docker/.env
```

This generates:
- `deploy/kubernetes/base/configmap-logwise-config.yaml`
- `deploy/kubernetes/base/secret-aws.yaml`

### Option 2: Separate .env for Kubernetes

For production, create a separate environment file:

```bash
# Create Kubernetes-specific env file
cp deploy/shared/templates/env.template deploy/kubernetes/.env
# Edit with production values
vim deploy/kubernetes/.env

# Sync to Kubernetes manifests
./deploy/shared/scripts/sync-config.sh deploy/kubernetes/.env
```

### Option 3: External Secret Management (Production)

For production, **DO NOT** use `.env` files. Instead:

1. **Use External Secrets Operator** with AWS Secrets Manager
2. **Use HashiCorp Vault**
3. **Use Sealed Secrets**
4. **Manually create Kubernetes Secrets** (encrypted at rest)

See `deploy/kubernetes/config/secrets.example.yaml` for examples.

## File Structure

```
deploy/
├── docker/
│   └── .env                    # Docker Compose environment file
├── kubernetes/
│   └── .env                    # Optional: Kubernetes-specific env file
└── shared/
    └── templates/
        └── env.template        # Template for both
```

## Best Practices

### Development
- ✅ Use `deploy/docker/.env` for Docker Compose
- ✅ Sync same file to Kubernetes for local testing
- ✅ Add `.env` to `.gitignore` (never commit secrets)

### Production
- ❌ Do NOT use `.env` files
- ✅ Use external secret management
- ✅ Use ConfigMaps for non-sensitive config
- ✅ Use Secrets for sensitive data (encrypted at rest)

## Syncing to Kubernetes

The sync script converts `.env` to Kubernetes manifests:

```bash
# Sync Docker .env to Kubernetes
./deploy/shared/scripts/sync-config.sh deploy/docker/.env

# Or specify custom paths
./deploy/shared/scripts/sync-config.sh \
  /path/to/.env \
  deploy/kubernetes/base
```

This generates:
- **ConfigMap**: Non-sensitive configuration
- **Secrets**: Sensitive data (base64 encoded)

**Warning**: The generated secrets are base64 encoded but NOT encrypted. For production, use proper secret management.

## Security Notes

1. **Never commit `.env` files** to version control
2. Add to `.gitignore`:
   ```
   deploy/docker/.env
   deploy/kubernetes/.env
   *.env
   ```
3. For production, use external secret management
4. Rotate secrets regularly
5. Use least-privilege IAM policies

## Troubleshooting

### Docker can't find .env
- Ensure you're in `deploy/docker/` directory
- Check file exists: `ls -la deploy/docker/.env`
- Verify docker-compose.yml references `.env`

### Kubernetes sync fails
- Check .env file exists and is readable
- Verify you have write permissions to kubernetes/base/
- Check for syntax errors in .env file

### Secrets not working in Kubernetes
- Verify secrets exist: `kubectl get secrets -n logwise`
- Check secret keys match what deployments expect
- Verify namespace is correct

