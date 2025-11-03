# Custom OpenTelemetry Collector Configuration Guide


These are detailed configurations with many optional features. You should customize them according to your specific use cases:
- Remove components you don't need (e.g., OTLP receiver if only reading files, bearertokenauth if no auth required)
- Adjust settings based on your log volume, format, and requirements
- Simplify to match the basic configs in `ec2/otel-collector-config.yaml` and `kubernetes/otel-collector-k8s.yaml` for simpler setups

## Configuration (EC2)

This is a complete, production-ready OTEL Collector configuration (otel-collector-config.yaml) for log shipping with advanced options:

```yaml
extensions:
  file_storage:
    directory: /var/lib/otelcol/storage   # Disk used by exporters' persistent queues (survive restarts/outages)

  # For securely managing API keys used by exporters (re-usable authenticator)
  bearertokenauth:
    token: ${env:MY_LOGS_API_KEY}         # Read from env; attached by exporters that reference "bearertokenauth"

  health_check:
    endpoint: 0.0.0.0:13133               # /healthz endpoint for liveness/readiness checks

  pprof:
    endpoint: 0.0.0.0:1777                # Go pprof server for CPU/memory profiling at /debug/pprof/*

receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317            # Accept OTLP over gRPC (4317)
      http:
        endpoint: 0.0.0.0:4318            # Accept OTLP over HTTP (4318)

  filelog:
    include: [ /var/log/my-app/*.log, /var/log/my-app/*.log.gz ]  # Include the compressed files , Tail all matching files (mount hostPath/volume into the collector)
    start_at: beginning                       # Read from start on first discovery (otherwise "end" starts from new lines only)
    compression: auto                                             # Auto-detects and decompresses .gz files

    operators:
      - type: regex_parser                    # Parse each line with a regex and capture fields (The named groups (time, level, message) become attributes.* by default)
        regex: '^(?P<time>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} IST) (?P<level>\w+) (?P<message>.*)$'
        
        timestamp:
          parse_from: attributes.time             # Use the 'time' captured by regex above
          layout: '%Y-%m-%d %H:%M:%S IST'         # Your format with a literal "IST"
          
          # Recommended:
          # layout_type: strptime          # Use strptime tokens for 'layout' (otherwise default Go layout is expected)
          # location: Asia/Kolkata         # Disambiguate "IST" as India (UTC+05:30). Without this, "IST" can be ambiguous.
          
        severity:
          parse_from: attributes.level
          mapping:
            INFO: INFO
            WARN: WARN
            ERROR: ERROR
        # Note: attributes.message remains as the textual message. You can move it into body if you prefer.

processors:
  memory_limiter:
    limit_mib: 1500                       # Keep collector memory below ~1.5 GiB (tune under your container limit)
    spike_limit_mib: 500                  # Allow brief spikes above the limit
    check_interval: 5s                    # Memory sampling cadence

  resource:
    attributes:
      - key: service.name                 # Service identifier - use dot notation (service.name) or snake_case (service_name) based on your convention
        value: ${env:SERVICE_NAME}        # Reads from SERVICE_NAME environment variable
        action: insert                    # Add only if missing (use 'upsert' to overwrite if already set upstream)
      - key: environment                  # Deployment environment identifier
        value: ${env:ENVIRONMENT}        # Reads from ENVIRONMENT environment variable
        action: insert                    # Add only if missing

  batch:
    timeout: 10s                          # Flush a batch at least every 10s (latency vs throughput)
    send_batch_size: 1024                 # Target batch size (# of records) per exporter call
    send_batch_max_size: 2048             # Hard cap for very bursty periods

exporters:
  otlphttp:
    endpoint: "http://log-endpoint:5000"  # Base URL; appends /v1/logs for the logs signal by default
    compression: gzip                     # Compress payloads
    auth:
      authenticator: bearertokenauth      # Attach Authorization: Bearer <MY_LOGS_API_KEY>
    timeout: 30s                          # Per-request timeout
    retry_on_failure:                     # Exponential backoff on transient failures
      enabled: true
      initial_interval: 1s
      max_interval: 30s
      max_elapsed_time: 300s
      multiplier: 2.0
    sending_queue:                        # Durable, on-disk queue for reliability
      enabled: true
      storage: file_storage               # Uses /var/lib/otelcol/storage
      num_consumers: 10                   # Parallel workers draining the queue
      queue_size: 1000                    # Max batches enqueued (not individual records)

  debug:
    verbosity: detailed                   # only for debugging

service:
  extensions: [file_storage, bearertokenauth, health_check, pprof]  # Load these extensions
  pipelines:
    logs:
      receivers: [otlp, filelog]                        # Accept logs from OTLP and from tailed files
      processors: [memory_limiter, resource, batch]     # Guard memory, enrich with attributes, and batch
      exporters: [otlphttp, debug]                      # Ship to backend
```

## Configuration (Kubernetes)

This is a detailed Kubernetes manifest (otel-testing.yaml) for log shipping. This configuration supports both container logs and application log files:

```yaml
# ----------------------------------------
# Full Kubernetes Manifest: OTel Collector Logging DaemonSet (Test Mode)
# ----------------------------------------

# 1. Namespace for Observability Components
apiVersion: v1
kind: Namespace
metadata:
  name: observability

---
# 2. Secret: Bearer token (Token is ignored for this local test)
apiVersion: v1
kind: Secret
metadata:
  name: logs-api-key
  namespace: observability
type: Opaque
stringData:
  MY_LOGS_API_KEY: ${env:MY_LOGS_API_KEY}

---
# 3. ServiceAccount + RBAC for k8sattributes enrichment
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
        directory: /var/lib/otelcol
      health_check:
        endpoint: 0.0.0.0:13133
      pprof:
        endpoint: 127.0.0.1:1777
      bearertokenauth:
        token: ${env:MY_LOGS_API_KEY}

    receivers:
      # Container logs from Kubernetes (optional - remove if only reading file logs)
      filelog/containers:
        include: [ /var/log/containers/*.log ]                    # Kubernetes container log symlinks
        exclude: [ /var/log/containers/*_kube-system_*.log ]      # Exclude system namespace logs
        start_at: end                                              # Start from end (only new logs)
        storage: file_storage                                      # Persist read position across restarts
        operators:
          - type: regex_parser                                     # Parse Kubernetes container log format
            regex: '^((?P<time>\S+)\s+(?P<stream>stdout|stderr)\s+(?P<logtag>[^ ]*)\s+(?P<log>.*))$'
            timestamp:
              parse_from: attributes.time                          # Extract timestamp from container log format
              layout: "%Y-%m-%dT%H:%M:%S.%9fZ"                    # Container timestamp format (nanoseconds + Z)
          
          # Parse the content of the 'log' attribute as JSON (if your app logs JSON)
          - type: json_parser
            parse_from: attributes.log                             # Parse the actual log content
            parse_to: body                                         # Overwrite body with parsed JSON
            
          # Extract Application Fields: Move original JSON keys to OTel standard fields
          - type: move
            from: body."log.level"                                 # Extract log level from JSON body
            to: attributes.app.level                               # Store as custom attribute

          # Clean up: Move message field to body
          - type: move
            from: body.message
            to: body                                               # Set message as the main body

          # Set Severity from stream (stdout/stderr)
          - type: severity_parser
            parse_from: attributes.stream                           # Use container stream for severity
            mapping:
              stderr: ERROR                                        # stderr maps to ERROR
              stdout: INFO                                         # stdout maps to INFO

      # File logs from mounted volumes (optional - remove if only reading container logs)
      filelog:
        include: [ /var/log/my-app/*.log, /var/log/my-app/*.log.gz ]  # Application log files from mounted volumes
        start_at: beginning                                        # Read from start on first discovery
        compression: auto                                         # Auto-detect and decompress .gz files
        storage: file_storage                                      # Persist read position across restarts
        operators:
          - type: regex_parser                                    # Parse custom log format
            regex: '^(?P<time>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} IST) (?P<level>\w+) (?P<message>.*)$'
            timestamp:
              parse_from: attributes.time                          # Extract timestamp from parsed fields
              layout: '%Y-%m-%d %H:%M:%S IST'                      # Your custom timestamp format
            severity:
              parse_from: attributes.level                         # Extract log level
              mapping:
                INFO: INFO
                WARN: WARN
                ERROR: ERROR

    processors:
      # Kubernetes attributes processor (optional - remove if not reading container logs)
      k8sattributes:
        auth_type: serviceAccount                                 # Use ServiceAccount for K8s API access
        passthrough: false                                        # Don't pass through if enrichment fails
        extract:
          metadata:                                              # Extract Kubernetes metadata
            - k8s.namespace.name                                 # Pod namespace
            - k8s.pod.name                                       # Pod name
            - k8s.container.name                                 # Container name
            - k8s.node.name                                     # Node name
          labels:                                                # Extract labels from pods
            - key: app.kubernetes.io/name                       # Service name from label
              from: pod
              tag_name: service.name                            # Map to service.name attribute
            - key: app.kubernetes.io/version                    # Service version from label
              from: pod
              tag_name: service.version                         # Map to service.version attribute
    
      # Resource processor: Add or update resource attributes
      resource:
        attributes:
          - key: service.name                                    # Service identifier (can override k8sattributes)
            action: upsert                                       # Upsert: add or overwrite (use 'insert' to only add if missing)
            value: "demo-service"                                # Hardcoded value - replace with ${env:SERVICE_NAME} for dynamic
          - key: environment                                     # Deployment environment
            action: upsert                                       # Upsert: add or overwrite
            value: "test"                                        # Hardcoded value - replace with ${env:ENVIRONMENT} for dynamic

      # Memory limiter: Prevent OOM kills by limiting memory usage
      memory_limiter:
        limit_mib: 1500                                         # Hard limit (tune based on pod memory limit)
        spike_limit_mib: 500                                    # Allow temporary spikes
        check_interval: 5s                                      # How often to check memory

      # Batch processor: Batch logs for efficient transmission
      batch:
        timeout: 10s                                            # Flush after timeout even if batch not full
        send_batch_size: 1024                                  # Target batch size
        send_batch_max_size: 2048                              # Maximum batch size for bursts

    exporters:
      # OTLP HTTP exporter: Send logs to remote backend
      otlphttp:
        endpoint: "https://log-endpoint.example.com"            # Your log backend URL
        compression: gzip                                       # Compress payloads for efficiency
        auth:
          authenticator: bearertokenauth                        # Use bearer token authentication
        timeout: 30s                                            # Request timeout
        retry_on_failure:                                       # Exponential backoff retry (add if needed)
          enabled: true
          initial_interval: 1s
          max_interval: 30s
          max_elapsed_time: 300s
          multiplier: 2.0
        sending_queue:                                         # Persistent queue for reliability
          enabled: true                                         # Enable for production (set to false for testing)
          storage: file_storage                                 # Use file storage for queue persistence
          num_consumers: 10                                     # Parallel workers
          queue_size: 1000                                      # Max batches in queue
        
      # Debug exporter: Output logs to collector's stdout (for debugging only)
      debug:
        verbosity: detailed                                     # Detailed debug output

    service:
      extensions: [file_storage, health_check, pprof, bearertokenauth]  # Load all extensions
      pipelines:
        logs:
          receivers: [filelog/containers, filelog]              # Accept from both container and file logs
          processors: [k8sattributes, resource, memory_limiter, batch]  # Process in order: enrich -> limit -> batch
          exporters: [otlphttp, debug]                    # Export to backend, file, and debug output

---
# 5. DaemonSet: Deploys Collector as Agent on Each Node
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
      serviceAccountName: otel-collector                         # Required for k8sattributes processor
      volumes:
        - name: varlog
          hostPath: { path: /var/log, type: Directory }          # Container logs and file logs directory
        - name: varlibdockercontainers                          # Required for k8sattributes to access container metadata
          hostPath: { path: /var/lib/docker/containers, type: DirectoryOrCreate }
        - name: varlibkubeletpods                                # Required for k8sattributes to access pod metadata
          hostPath: { path: /var/lib/kubelet/pods, type: Directory }
        - name: otel-storage                                     # Persistent storage for queues and state
          hostPath: { path: /var/lib/otelcol, type: DirectoryOrCreate }
        - name: otel-config                                      # ConfigMap mounted as file
          configMap:
            name: otel-collector-config
            items: [{ key: otel-config.yaml, path: otel-config.yaml }]
      containers:
        - name: otel-collector
          image: otel/opentelemetry-collector-contrib:0.90.1     # Use stable, versioned image
          imagePullPolicy: IfNotPresent
          args: ["--config=/conf/otel-config.yaml"]              # Path to config file in container
          env:
            - name: MY_LOGS_API_KEY                              # API key from Secret (for bearertokenauth)
              valueFrom:
                secretKeyRef:
                  name: logs-api-key
                  key: MY_LOGS_API_KEY
          ports:
            - { name: healthz, containerPort: 13133 }           # Health check endpoint
          volumeMounts:
            - { name: otel-config, mountPath: /conf, readOnly: true }          # Config file
            - { name: varlog, mountPath: /var/log, readOnly: true }            # Log directory (for container and file logs)
            - { name: varlibdockercontainers, mountPath: /var/lib/docker/containers, readOnly: true }  # For k8sattributes
            - { name: varlibkubeletpods, mountPath: /var/lib/kubelet/pods, readOnly: true }            # For k8sattributes
            - { name: otel-storage, mountPath: /var/lib/otelcol }              # Persistent storage
          livenessProbe:
            httpGet: { path: /healthz, port: healthz }
            initialDelaySeconds: 10
            periodSeconds: 10
          readinessProbe:
            httpGet: { path: /healthz, port: healthz }
            initialDelaySeconds: 5
            periodSeconds: 5
          resources:
            requests: { cpu: 100m, memory: 256Mi }
            limits: { cpu: 1000m, memory: 1Gi }
          securityContext:
            runAsUser: 0
            runAsGroup: 0
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
            capabilities: { drop: ["ALL"] }
```

## What You Need to Provide

### For EC2/Standalone Configuration

**1. Log Endpoint Configuration:**
```yaml
exporters:
  otlphttp:
    endpoint: "http://your-log-endpoint:5000"
```

**2. Environment Variables:**
```bash
export MY_LOGS_API_KEY="your-api-key-here"
export SERVICE_NAME="your-service-name"
export ENVIRONMENT="production"
```

**3. File Paths:**
```yaml
receivers:
  filelog:
    include: [ /var/log/your-app/*.log, /var/log/your-app/*.log.gz ]
```

### For Kubernetes Configuration

**1. Log Endpoint Configuration:**
Update the endpoint in the ConfigMap:
```yaml
exporters:
  otlphttp:
    endpoint: "https://your-log-endpoint.com"
```

**2. API Key:**
Set your API key in the Secret or use environment variable:
```yaml
stringData:
  MY_LOGS_API_KEY: "your-actual-api-key-here"
```

**3. File Paths for filelog:**
Update file paths in the ConfigMap to match your mounted volumes:
```yaml
receivers:
  filelog:
    include: [ /var/log/your-app/*.log, /var/log/your-app/*.log.gz ]
```

## Related Documentation

- [OTEL Agent Basic Installation](OTEL_AGENT_BASIC_INSTALLATION.md)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Collector Configuration Reference](https://opentelemetry.io/docs/collector/configuration/)
