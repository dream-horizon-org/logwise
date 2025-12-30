# OpenTelemetry Log Shipping Configuration Guide

This guide covers OpenTelemetry (OTEL) collector configurations for log shipping, including OTLP receiver setup, file log tailing, and exporter configurations with detailed explanations of each component.

## Overview

OpenTelemetry Collector reads from log files on the filesystem

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


**2. Prepare Configuration:**

  (a). Download or copy the configuration file. The configuration file should be in the same directory as the collector binary
     
  (b). Customize the configuration as needed (see [`what-you-need-to-provide`](#what-you-need-to-provide) section below)

**3. Run collector:**
```bash
# Run with your config (see otel-collector-config.yaml for configuration details)
./otelcol-contrib --config=otel-collector-config.yaml
```

**Note**: The configuration file [`otel-collector-config.yaml`](https://github.com/dream-horizon-org/logwise/blob/main/docs/send-logs/collectors/otel/ec2/otel-collector-config.yaml) contains the production-ready setup. Make sure to customize it according to your requirements before running.

### Verify Installation

**Check if collector is running:**
```bash
# Check health endpoint
curl http://localhost:13133

# Check processes
ps aux | grep otelcol
```


The configuration includes:
- **Extensions**: File storage for persistent queues and health check endpoint
- **Receivers**: Filelog for tailing log files from the filesystem
- **Processors**: Memory limiter, resource processor (for adding service metadata), and batch processor for efficient log processing
- **Exporters**: OTLP HTTP exporter with retry logic and persistent queues

## What You Need to Provide

### 1. Log Endpoint Configuration

Update the endpoint in `otel-collector-config.yaml`:

```yaml
exporters:
  otlphttp:
    endpoint: "http://your-log-endpoint:5000"
```

### 2. Environment Variables

Set these environment variables before running the collector:

```bash
export SERVICE_NAME="your-service-name"
export ENVIRONMENT="production"
```

These environment variables are used by the resource processor to add metadata to your logs:
- `SERVICE_NAME` - Identifies your service

**Note**: The configuration also automatically adds `type: application` to all logs as a resource attribute.

### 3. File Paths

Update the file paths in `otel-collector-config.yaml` to match your log locations:

```yaml
receivers:
  filelog:
    include: [ /var/log/your-app/*.log, /var/log/your-app/*.log.gz ]
```

## Related Documentation

- [OpenTelemetry Documentation](https://opentelemetry.io/docs/)
- [Collector Configuration Reference](https://opentelemetry.io/docs/collector/configuration/)
- [File Log Receiver Documentation](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/filelogreceiver)
