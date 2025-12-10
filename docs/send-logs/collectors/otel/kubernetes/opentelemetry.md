# Kubernetes OpenTelemetry Collector Configuration Guide

Kubernetes setup guide for the OpenTelemetry Collector log shipping system. In our architecture, the OTEL Collector automatically collects application log files and ships them to remote log backends with resource attribute enrichment.

**Important**: This configuration reads application logs from files on the filesystem. Ensure your application writes logs to accessible file paths.

## Installation

### Prerequisites

- Kubernetes cluster (1.19+)
- kubectl configured to access your cluster
- Log endpoint or backend service

### Installation Steps

1. **Prepare Configuration**

    (a). Download or copy the manifest file:

    (b). Customize the configuration as needed (see [`what-you-need-to-provide`](#what-you-need-to-provide) section below)

2. **Deploy to Cluster**
   - Apply the complete manifest: `kubectl apply -f otel-collector-config.yaml`
     The configuration file is available at [`otel-collector-config.yaml`](https://github.com/dream-horizon-org/logwise/blob/main/docs/send-logs/kubernetes/otel-collector-config.yaml).
   - Verify deployment: `kubectl get pods -n observability`

3. **Verify Log Collection**
   - Check collector logs: `kubectl logs -n observability -l app.kubernetes.io/name=otel-collector`
   - Monitor health endpoint: `kubectl port-forward -n observability svc/otel-collector 13133:13133`

**To use this configuration:**

The configuration includes:
- **Extensions**: File storage for persistent queues and health check endpoint
- **Receivers**: Filelog for tailing application log files from the filesystem
- **Processors**: Resource processor (for adding service metadata), memory limiter, and batch processor
- **Exporters**: OTLP HTTP exporter with retry logic and persistent queues, plus debug exporter

## What You Need to Provide

### 1. Log Endpoint Configuration

Update the endpoint in `otel-collector-config.yaml`:

```yaml
exporters:
  otlphttp:
    endpoint: "https://your-log-endpoint.com"
```

### 2. Environment Variables

Set these environment variables in the DaemonSet section of `otel-collector-config.yaml`:

```yaml
env:
  - name: SERVICE_NAME
    value: "your-service-name"
```

These environment variables are used by the resource processor to add metadata to your logs:
- `SERVICE_NAME` - Identifies your service

**Note**: The configuration also automatically adds `type: application` to all logs as a resource attribute.

### 3. File Paths

Update the file paths in the ConfigMap section of `otel-collector-config.yaml` to match your application log locations:

```yaml
receivers:
  filelog/app:
    include: [ /var/log/your-app/*.log, /var/log/your-app/*.log.gz ]
```

**Note**: Ensure the log directory is mounted as a volume in the DaemonSet and that the collector has read access to the log files.

## Log Processing Features

The collector automatically processes logs with the following capabilities:

### Application Log File Discovery

- **Automatic Discovery**: Reads from configured log file paths (e.g., `/var/log/app/*.log`)
- **Compression Support**: Automatically detects and decompresses `.gz` files
- **File Rotation**: Handles log rotation and compressed log files
- **Start at Beginning**: Reads from the start of log files on first discovery

### Log Parsing and Enrichment

- **Resource Attributes**: Adds service name, environment, and type information to all logs

### Export Capabilities

- **OTLP HTTP**: Sends logs to remote backends
- **Retry Logic**: Automatic retry with exponential backoff
- **Durable Queues**: Persistent storage for reliable delivery
- **Compression**: Gzip compression for efficient transmission

## Log Format and Metadata

The collector enriches logs with the following metadata structure:

### Resource Attributes

- **`service_name`**: Service identifier (from `SERVICE_NAME` environment variable)

### Log Body

- **`body`**: The actual log message content
- **`severity`**: Log level (INFO, ERROR, etc.)

## Resource Management

### Memory Limits

- **Memory Limit**: 1.5 GiB with 500 MiB spike allowance
- **Check Interval**: 5 seconds for memory monitoring
- **Protection**: Prevents OOM kills in high-volume environments

### CPU and Memory Requests

- **CPU Request**: 100m (guaranteed)
- **CPU Limit**: 1000m (maximum)
- **Memory Request**: 256Mi (guaranteed)
- **Memory Limit**: 1Gi (maximum)

### Batch Processing

- **Batch Timeout**: 10 seconds (latency vs throughput)
- **Batch Size**: 1024 records per batch
- **Max Batch Size**: 2048 records (burst handling)

## Verification

After deploying the collector, verify it's working correctly:

```bash
# Check collector pods are running
kubectl get pods -n observability

# Check DaemonSet status
kubectl get daemonset -n observability

# View collector logs
kubectl logs -n observability -l app.kubernetes.io/name=otel-collector

# Check health endpoint
kubectl port-forward -n observability svc/otel-collector 13133:13133
curl http://localhost:13133/healthz

# Check if logs are being processed
kubectl logs -n observability -l app.kubernetes.io/name=otel-collector | grep "filelog/app"
```


**No logs being collected**: Verify the collector has access to your configured log file paths and check file permissions. Ensure the log directory is properly mounted as a volume.

**Export failures**: Check the endpoint URL and network connectivity to your log backend.

## Related Documentation

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Collector Configuration Reference](https://opentelemetry.io/docs/collector/configuration/)
- [File Log Receiver Documentation](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/filelogreceiver)
