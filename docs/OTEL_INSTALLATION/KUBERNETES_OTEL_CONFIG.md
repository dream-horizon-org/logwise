# Kubernetes OpenTelemetry Collector Configuration Guide

Kubernetes setup guide for the OpenTelemetry Collector log shipping system. In our architecture, the OTEL Collector automatically collects container logs and ships them to remote log backends with Kubernetes metadata enrichment.

## Overview

The OpenTelemetry Collector serves as the log collection and processing platform for Kubernetes workloads. The collector automatically discovers and processes container logs, enriching them with Kubernetes metadata. This guide covers the essential configuration needed.

**Important**: This configuration requires **Kubernetes RBAC permissions** to read pod and node metadata for log enrichment.

## Installation

### Prerequisites

- Kubernetes cluster (1.19+)
- kubectl configured to access your cluster
- Log endpoint or backend service

### Installation Steps

1. **Prepare Configuration**
   - Update the log endpoint URL in the ConfigMap
   - Set your API key in the Secret
   - Customize environment variables as needed

2. **Deploy to Cluster**
   - Apply the complete manifest: `kubectl apply -f otel-testing.yaml`
   - Verify deployment: `kubectl get pods -n observability`

3. **Verify Log Collection**
   - Check collector logs: `kubectl logs -n observability -l app.kubernetes.io/name=otel-collector`
   - Monitor health endpoint: `kubectl port-forward -n observability svc/otel-collector 13133:13133`

## Required Configuration

The following settings are essential for proper log collection and processing:

### Log Endpoint Configuration

**REQUIRED**: Update the endpoint URL in the ConfigMap:

```yaml
exporters:
  otlphttp:
    endpoint: "https://your-log-endpoint.com"
```

**Why required**: The collector needs to know where to send processed logs. Without a valid endpoint, logs will only be stored locally or in debug output.

### API Key Authentication

**REQUIRED**: Set your API key in the Secret:

```yaml
stringData:
  MY_LOGS_API_KEY: "your-actual-api-key-here"
```

**Why required**: Most log backends require authentication. The collector uses bearer token authentication to securely send logs.

### Kubernetes Metadata Enrichment

**REQUIRED**: RBAC permissions for metadata enrichment:

```yaml
rules:
  - apiGroups: [""]
    resources: ["pods", "namespaces", "nodes"]
    verbs: ["get", "list", "watch"]
```

**Why required**: The collector enriches logs with Kubernetes metadata (pod name, namespace, node name, labels) by querying the Kubernetes API. Without these permissions, logs will lack important context.

## Production-Ready Configuration

This is a complete, production-ready Kubernetes manifest (otel-testing.yaml) for log shipping:

```yaml
# ----------------------------------------
# 1. Namespace for Observability Components
# ----------------------------------------
apiVersion: v1
kind: Namespace
metadata:
  name: observability

---
# ----------------------------------------
# 2. Secret: Bearer token for your log backend
# ----------------------------------------
apiVersion: v1
kind: Secret
metadata:
  name: logs-api-key
  namespace: observability
type: Opaque
stringData:
  MY_LOGS_API_KEY: "REPLACE_WITH_REAL_TOKEN"

---
# ----------------------------------------
# 3. ServiceAccount + RBAC for k8sattributes enrichment
# ----------------------------------------
apiVersion: v1
kind: ServiceAccount
metadata:
  name: otel-collector
  namespace: observability

---
# ClusterRole grants permissions cluster-wide to read Pod/Node metadata
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: otel-collector-k8s-read
rules:
  - apiGroups: [""]
    resources: ["pods", "namespaces", "nodes"]
    verbs: ["get", "list", "watch"]

---
# ClusterRoleBinding links the ServiceAccount to the ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRoleBinding
metadata:
  name: otel-collector-k8s-read-binding
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
  name: otel-collector-k8s-read
subjects:
  - kind: ServiceAccount
    name: otel-collector
    namespace: observability

---
# ----------------------------------------
# 4. ConfigMap: OpenTelemetry Collector Configuration
# ----------------------------------------
apiVersion: v1
kind: ConfigMap
metadata:
  name: otel-collector-config
  namespace: observability
data:
  otel-config.yaml: |
    extensions:
      file_storage:
        directory: /var/lib/otelcol/storage
      health_check:
        endpoint: 0.0.0.0:13133
      # Keep pprof on localhost for security
      pprof:
        endpoint: 127.0.0.1:1777
      #  bearer token (read from env variable injected via Secret)
      bearertokenauth:
        token: ${env:MY_LOGS_API_KEY}

    receivers:
      # Tail Kubernetes container logs via stable symlink path
      filelog/containers:
        include: [ /var/log/containers/*.log ]
        # Skip system logs to save cost/reduce noise
        exclude: [ /var/log/containers/*_kube-system_*.log ]
        start_at: end
        storage: file_storage # Used for checkpointing (offset and fingerprint)
        operators:
          - type: container_parser # Parses container runtime JSON (CRI/Docker) and handles multiline logs
          - type: move # Move the final log text into the standard OTel body field
            from: attributes.log
            to: body
          - type: severity_parser # Attempts to map log text (e.g., 'INFO') to OTel severity
            parse_from: body

    processors:
      # Enriches logs with Kubernetes metadata by querying the API
      k8sattributes:
        auth_type: serviceAccount
        passthrough: false
        extract:
          metadata:
            - k8s.namespace.name
            - k8s.pod.name
            - k8s.container.name
            - k8s.node.name
          # Promote common labels to OTel Semantic Conventions
          labels:
            - key: app.kubernetes.io/name
              from: pod
              tag_name: service.name
            - key: app.kubernetes.io/version
              from: pod
              tag_name: service.version
        filter:
          # Uncomment this to scope the enrichment lookup to a specific namespace
          # namespace: ["my-namespace"]
          pass:
            - key: k8s.namespace.name
              values: [ "observability", "default" ] # Example: Only process logs from these namespaces

      # Fallback defaults for enrichment
      resource:
        attributes:
          - { key: service.name, action: upsert, value: ${env:SERVICE_NAME} }
          - { key: environment, action: upsert, value: ${env:ENVIRONMENT} }

      # Memory protection
      memory_limiter:
        limit_mib: 1500
        spike_limit_mib: 500
        check_interval: 5s

      # Performance and efficiency
      batch:
        timeout: 10s 
        send_batch_size: 1024
        send_batch_max_size: 2048

    exporters:
      # Reliable OTLP/HTTP exporter
      otlphttp:
        # !!! CHANGE THIS ENDPOINT !!!
        endpoint: "https://log-endpoint.example.com"
        compression: gzip
        auth:
          authenticator: bearertokenauth
        retry_on_failure:
          enabled: true
          initial_interval: 1s
          max_elapsed_time: 300s
        sending_queue: # Durable disk queue for At-Least-Once delivery
          enabled: true
          storage: file_storage
          num_consumers: 10
          queue_size: 1000

      # Debugging output (view in kubectl logs)
      debug:
        verbosity: basic

    service:
      extensions: [file_storage, health_check, pprof, bearertokenauth]
      pipelines:
        logs:
          receivers: [filelog/containers]
          processors: [k8sattributes, resource, memory_limiter, batch]
          exporters: [otlphttp, debug]

---
# ----------------------------------------
# 5. DaemonSet: Deploys Collector as Agent on Each Node
# ----------------------------------------
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: otel-collector
  namespace: observability
  labels:
    app.kubernetes.io/name: otel-collector
spec:
  selector:
    matchLabels:
      app.kubernetes.io/name: otel-collector
  updateStrategy:
    type: RollingUpdate
  template:
    metadata:
      labels:
        app.kubernetes.io/name: otel-collector
    spec:
      serviceAccountName: otel-collector # Grants RBAC permissions
      # Mount host paths to access logs and state
      volumes:
        - name: varlog
          hostPath: { path: /var/log, type: Directory }
        - name: varlibdockercontainers
          hostPath: { path: /var/lib/docker/containers, type: DirectoryOrCreate }
        - name: varlibkubeletpods
          hostPath: { path: /var/lib/kubelet/pods, type: Directory }
        - name: otel-storage
          hostPath: { path: /var/lib/otelcol, type: DirectoryOrCreate } # Durable storage mount
        - name: otel-config
          configMap:
            name: otel-collector-config
            items: [{ key: otel-config.yaml, path: otel-config.yaml }]
      containers:
        - name: otel-collector
          image: otel/opentelemetry-collector-contrib:latest
          imagePullPolicy: IfNotPresent
          args: ["--config=/conf/otel-config.yaml"]
          # Inject API key from Secret
          env:
            - name: MY_LOGS_API_KEY
              valueFrom:
                secretKeyRef:
                  name: logs-api-key
                  key: MY_LOGS_API_KEY
            - { name: SERVICE_NAME, value: "k8s-workload" }
            - { name: ENVIRONMENT, value: "production" }
          ports:
            - { name: healthz, containerPort: 13133 }
          volumeMounts:
            - { name: otel-config, mountPath: /conf, readOnly: true }
            - { name: varlog, mountPath: /var/log, readOnly: true }
            - { name: varlibdockercontainers, mountPath: /var/lib/docker/containers, readOnly: true }
            - { name: varlibkubeletpods, mountPath: /var/lib/kubelet/pods, readOnly: true }
            - { name: otel-storage, mountPath: /var/lib/otelcol } # Writable mount for persistence
          # Health checks
          livenessProbe:
            httpGet: { path: /healthz, port: healthz }
            initialDelaySeconds: 10
            periodSeconds: 10
          readinessProbe:
            httpGet: { path: /healthz, port: healthz }
            initialDelaySeconds: 5
            periodSeconds: 5
          # Resource constraints
          resources:
            requests: { cpu: 100m, memory: 256Mi }
            limits: { cpu: 1000m, memory: 1Gi }
          # Security Context (necessary for host log access)
          securityContext:
            runAsUser: 0 # Needs root to read host logs
            runAsGroup: 0
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
            capabilities: { drop: ["ALL"] }
```

## Log Processing Features

The collector automatically processes logs with the following capabilities:

### Container Log Discovery

- **Automatic Discovery**: Reads from `/var/log/containers/*.log` (Kubernetes symlinks)
- **System Log Exclusion**: Skips kube-system logs to reduce noise
- **Multi-line Support**: Handles multi-line log entries properly

### Log Parsing and Enrichment

- **Container Parser**: Extracts timestamp, stream, and log content from container format
- **Severity Mapping**: Maps stderr/stdout to ERROR/INFO levels
- **Kubernetes Metadata**: Adds pod name, namespace, node name, and labels
- **Resource Attributes**: Adds service name and environment information

### Export Capabilities

- **OTLP HTTP**: Sends logs to remote backends with authentication
- **Retry Logic**: Automatic retry with exponential backoff
- **Durable Queues**: Persistent storage for reliable delivery
- **Compression**: Gzip compression for efficient transmission

## Log Format and Metadata

The collector enriches logs with the following metadata structure:

### Kubernetes Metadata

- **`k8s.namespace.name`**: Pod namespace
- **`k8s.pod.name`**: Pod name
- **`k8s.container.name`**: Container name
- **`k8s.node.name`**: Node name
- **`service.name`**: From `app.kubernetes.io/name` label
- **`service.version`**: From `app.kubernetes.io/version` label

### Resource Attributes

- **`service.name`**: Service identifier (from environment variable)
- **`environment`**: Environment identifier (from environment variable)

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
kubectl logs -n observability -l app.kubernetes.io/name=otel-collector | grep "filelog/containers"
```

## Troubleshooting

**Collector pods not starting**: Check RBAC permissions and ensure the ServiceAccount has proper ClusterRoleBinding.

**No logs being collected**: Verify the collector has access to `/var/log/containers/` and check file permissions.

**Export failures**: Check the endpoint URL, API key, and network connectivity to your log backend.

**Memory issues**: Monitor resource usage and adjust memory limits if needed. Check for OOM kills in pod events.

**RBAC permission denied**: Ensure the ServiceAccount has the required permissions to read pods, namespaces, and nodes.

## Related Documentation

- [OTEL Agent Basic Installation](OTEL_AGENT_BASIC_INSTALLATION.md)
- [OTEL Log Configuration](OTEL_LOG_CONFIGURATION.md)
- [OpenTelemetry Collector Documentation](https://opentelemetry.io/docs/collector/)