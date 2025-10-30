# OpenTelemetry Log Shipping Configuration Guide

This guide covers OpenTelemetry (OTEL) collector configurations for log shipping, including OTLP receiver setup, file log tailing, and exporter configurations with detailed explanations of each component.

## Overview

OpenTelemetry Collector reads from log files on the filesystem

## OTEL Agent Installation

1. **Install OTEL agent for your application:**
   - Install the OpenTelemetry SDK/agent for your programming language
   - Configure it to send logs via OTLP to the collector endpoint
   - Enable automatic log instrumentation

2. **Run your application:**
   - Start your application with OTEL instrumentation enabled
   - Logs will be automatically collected and sent to the OTEL collector
   - Verify logs are flowing by checking the collector's debug output

## Prerequisites

Before configuring OTEL log shipping, ensure you have:

- **OTEL Collector** installed and running
- **Network access** between applications and collector
- **File system permissions** for log file access (if using filelog receiver)

## How to Install and Run OTEL Collector

### Using Binary

**1. Download binary:**
 
```bash

# Choose the appropriate binary for your system:

# Example for Linux AMD64:

# Download latest release
wget https://github.com/open-telemetry/opentelemetry-collector-releases/releases/latest/download/otelcol-contrib_linux_amd64.tar.gz

# Extract
tar -xzf otelcol-contrib_linux_amd64.tar.gz

# Make executable
chmod +x otelcol-contrib
```

**2. Run collector:**
```bash
# Run with your config
./otelcol-contrib --config=otel-collector-config.yaml
```

### Verify Installation

**Check if collector is running:**
```bash
# Check health endpoint
curl http://localhost:13133

# Check processes
ps aux | grep otelcol
```

## Production-Ready Configuration

This is a complete, production-ready OTEL Collector configuration(otel-collector-config.yaml) for log shipping:

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
      - key: service.name
        value: ${env:SERVICE_NAME}
        action: insert                    # Add only if missing (use 'upsert' to overwrite if already set upstream)
      - key: environment
        value: ${env:ENVIRONMENT}
        action: insert

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

## What You Need to Provide

### 1. Log Endpoint Configuration
endpoint: "http://your-log-endpoint:5000"  

### 2. Environment Variables

Set these environment variables:

```bash
export MY_LOGS_API_KEY="your-api-key-here"
export SERVICE_NAME="your-service-name"
export ENVIRONMENT="production"
```

### 3. File Paths

Update the file paths to match your log locations:

```yaml
receivers:
  filelog:
    include: [ /var/log/your-app/*.log, /var/log/your-app/*.log.gz ]
```

### 4. Log Format

Adjust the regex pattern to match your log format:

```yaml
operators:
  - type: regex_parser
    regex: '^(?P<time>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} IST) (?P<level>\w+) (?P<message>.*)$'
```

## Exporter Sending Queue Configuration

The sending queue provides reliable log delivery with retry logic and persistent storage.

### Queue Configuration Example

```yaml
exporters:
  otlphttp:
    endpoint: "http://log-endpoint:5000"
    sending_queue:
      enabled: true
      storage: file_storage               # Persistent queue on disk
      num_consumers: 10                   # Parallel workers
      queue_size: 1000                    # Max batches enqueued
    retry_on_failure:
      enabled: true
      initial_interval: 1s
      max_interval: 30s
      max_elapsed_time: 300s
      multiplier: 2.0
```


### Capacity Configuration Example

```yaml
receivers:
  filelog:
    include: [ /var/log/my-app/*.log ]
    max_concurrent_files: 256        # Max files to read simultaneously
    poll_interval: 200ms            # Filesystem scan interval
    fingerprint_size: 1024          # Bytes for file identification
    initial_buffer_size: 16384      # Initial read buffer
    start_at: beginning             # Read from start on first discovery
    compression: auto               # Auto-detect .gz files
```

### Rotation Configuration Example

```yaml
receivers:
  filelog:
    include: [ /var/log/my-app/*.log, /var/log/my-app/*.log.* ]
    start_at: beginning
    compression: auto
    max_concurrent_files: 256
    # Handles both live logs and rotated .gz files
```

## Best Practices

### Configuration Recommendations

1. **Memory Management**: Set `memory_limiter` limits below container memory limits
2. **Batch Sizing**: Balance latency vs throughput with appropriate batch sizes
3. **Retry Logic**: Configure retry settings based on backend SLAs
4. **Persistent Queues**: Use `file_storage` for production reliability
5. **Resource Attributes**: Add consistent service identification

### Performance Tuning

- **Increase `num_consumers`** for higher throughput (monitor CPU usage)
- **Adjust `queue_size`** based on available disk space
- **Tune `send_batch_size`** for optimal backend performance
- **Set appropriate `timeout`** values for your network conditions

### Monitoring

- **Health Checks**: Use the health check endpoint for liveness probes
- **Profiling**: Enable pprof for performance analysis
- **Debug Logging**: Use debug exporter for troubleshooting (disable in production)

## Troubleshooting

### Common Issues

1. **Memory Issues**: Reduce `limit_mib` or increase container memory
2. **File Permission Errors**: Ensure collector has read access to log files
3. **Network Timeouts**: Increase `timeout` values for slow networks
4. **Duplicate Logs**: Check rotation configuration and fingerprint settings

### Debug Configuration

```yaml
exporters:
  debug:
    verbosity: detailed                   # Enable detailed debug output

service:
  pipelines:
    logs:
      exporters: [otlphttp, debug]       # Include debug exporter for troubleshooting
```

## Related Documentation

- [OTEL Agent Installation Guide](OTEL_AGENT_INSTALLATION.md)
- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Collector Configuration Reference](https://opentelemetry.io/docs/collector/configuration/)
- [File Log Receiver Documentation](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/filelogreceiver)
