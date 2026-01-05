# Troubleshooting Guide

Common issues and solutions for LogWise deployments.

## Docker Compose Issues

### Services won't start

**Symptoms**: Containers exit immediately or fail to start

**Solutions**:
1. Check Docker resources:
   ```bash
   docker system df
   docker stats
   ```
2. Increase Docker memory/CPU limits in Docker Desktop
3. Check logs:
   ```bash
   docker compose -f docker-compose.yml logs <service-name>
   ```
4. Verify `.env` file exists and has correct values
5. Check port conflicts:
   ```bash
   lsof -i :8080
   ```

### Database connection errors

**Symptoms**: Orchestrator can't connect to database

**Solutions**:
1. Wait for MySQL to be healthy:
   ```bash
   docker compose -f docker-compose.yml ps db
   ```
2. Verify database credentials in `.env`
3. Check database logs:
   ```bash
   docker compose -f docker-compose.yml logs db
   ```
4. Restart database:
   ```bash
   docker compose -f docker-compose.yml restart db
   ```

### Kafka connection issues

**Symptoms**: Services can't connect to Kafka

**Solutions**:
1. Wait for Kafka to be healthy:
   ```bash
   docker compose -f docker-compose.yml ps kafka
   ```
2. Check Kafka logs:
   ```bash
   docker compose -f docker-compose.yml logs kafka
   ```
3. Verify `KAFKA_BROKERS` in `.env`
4. Test Kafka connectivity:
   ```bash
   docker compose -f docker-compose.yml exec kafka \
     kafka-topics --bootstrap-server localhost:9092 --list
   ```

### AWS connection errors

**Symptoms**: Services can't access S3 or Athena

**Solutions**:
1. Verify AWS credentials in `.env`:
   ```bash
   grep AWS .env
   ```
2. Test AWS connectivity:
   ```bash
   docker compose -f docker-compose.yml exec orchestrator \
     aws s3 ls
   ```
3. Check IAM permissions
4. Verify AWS region is correct
5. Check S3 bucket exists and is accessible

## Kubernetes Issues

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

### Image pull errors

**Symptoms**: `ErrImagePull` or `ImagePullBackOff`

**Solutions**:
1. Verify image exists:
   ```bash
   docker pull <image-name>
   ```
2. Check registry credentials:
   ```bash
   kubectl get secrets -n logwise
   ```
3. For kind clusters, load images:
   ```bash
   kind load docker-image <image-name>
   ```
4. Verify image pull policy in deployment
5. Check network connectivity to registry

### Services not accessible

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

### Configuration issues

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

### Resource constraints

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

## Common Application Issues

### Logs not appearing

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

### Spark jobs failing

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

### Orchestrator errors

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

## Getting Help

1. Check logs first
2. Review this troubleshooting guide
3. Check documentation
4. Review GitHub issues
5. Contact support team

## Useful Commands

### Docker Compose

```bash
# View all logs
docker compose -f docker-compose.yml logs

# View specific service logs
docker compose -f docker-compose.yml logs <service>

# Restart service
docker compose -f docker-compose.yml restart <service>

# Check service status
docker compose -f docker-compose.yml ps

# Execute command in container
docker compose -f docker-compose.yml exec <service> <command>
```

### Kubernetes

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

