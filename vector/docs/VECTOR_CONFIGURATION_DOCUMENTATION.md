# Vector.dev Configuration Documentation

## Overview

This document provides comprehensive documentation for a [Vector](https://vector.dev) configuration that serves as a high-performance log processing pipeline for OpenTelemetry (OTLP) logs. Vector replaces traditional Flask/HTTP endpoints with a robust, production-ready solution for receiving, transforming, and forwarding telemetry data.

## Architecture Overview

Typically, the OTEL Collector runs as a sidecar or DaemonSet on each node, while Vector and kafka runs as a seperately deployed services.

```
┌─────────────────┐    ┌───────────────────┐      ┌─────────────────┐    ┌─────────────────┐
│   Applications  │───▶│ OTEL Collector    │─────▶│     Vector      │───▶│     Kafka       │
│  (with OTEL     │    │   (Agent sidecar) |      │  (Log Endpoint) │    │   (Topic:       │
│ instrumentation)│    │                   │      │                 │    │ logs.app.*)     │
└─────────────────┘    └───────────────────┘      └─────────────────┘    └─────────────────┘
```

### Data Flow

1. **Applications** emit logs via OpenTelemetry instrumentation
2. **OTEL Collector** receives and processes logs from multiple sources
3. **Vector** receives OTLP logs via gRPC/HTTP, transforms them, and enriches metadata
4. **Kafka** stores processed logs in environment-specific topics

## Configuration File: `vector.toml`

### Global Configuration

```toml
# Vector.dev configuration for log endpoint service
# Replaces the Flask app for receiving OTLP logs from OTEL collector

# Data directory for Vector's internal state (must be at root level)
data_dir = "/var/lib/vector"
```

**Purpose**: The `data_dir` configuration specifies where Vector stores its internal state and persistent data. This is crucial for:
- Buffering data during temporary downstream failures
- Maintaining processing state across restarts
- Ensuring data durability

**Reference**: [Vector Configuration - Global Options](https://vector.dev/docs/reference/configuration/global-options/#data_dir)

### API Configuration

```toml
[api]
enabled = true
address = "0.0.0.0:8686"
playground = false
```

**Purpose**: Enables Vector's built-in API server for:
- Health checks and monitoring
- Runtime configuration management
- Metrics collection
- Troubleshooting and debugging

**Key Options**:
- `enabled = true`: Activates the API server
- `address = "0.0.0.0:8686"`: Binds to all interfaces on port 8686
- `playground = false`: Disables the GraphQL playground for security (production setting)

**Reference**: [Vector API Configuration](https://vector.dev/docs/reference/configuration/api/)

### Sources Configuration

#### OpenTelemetry Source

```toml
# OpenTelemetry source to receive logs from OTEL collector  
[sources.otlp_logs]
type = "opentelemetry"

[sources.otlp_logs.grpc]
address = "0.0.0.0:4317"

[sources.otlp_logs.http]
address = "0.0.0.0:4318"
```

**Purpose**: Configures Vector to receive OpenTelemetry Protocol (OTLP) data from upstream collectors or applications.

**Key Features**:
- **Dual Protocol Support**: Accepts both gRPC (port 4317) and HTTP (port 4318) protocols
- **Standards Compliance**: Fully compatible with [OpenTelemetry Protocol Specification](https://opentelemetry.io/docs/specs/otlp/)
- **Automatic Parsing**: Vector automatically deserializes OTLP payloads into its internal event format

**Protocol Details**:
- **gRPC (4317)**: High-performance binary protocol, preferred for high-throughput scenarios
- **HTTP (4318)**: RESTful interface, easier for debugging and testing

**Reference**: [Vector OpenTelemetry Source](https://vector.dev/docs/reference/configuration/sources/opentelemetry/)

### Transforms Configuration

#### Log Enrichment Transform

```toml
# Transform to enrich OTLP data with additional metadata
[transforms.enrich_otlp]
type = "remap"
inputs = ["otlp_logs.logs"]
drop_on_abort = true
source = '''

if !is_string(.resources."service.name") { abort }
if !is_string(.resources.environment)    { abort }

# Normalize as strings (infallible)
.service_name     = to_string(.resources."service.name") ?? "unknown"
.environment_name = to_string(.resources.environment)    ?? "unknown"

# Handle attributes with fallible assignments and defaults
.message    = to_string(.attributes.message) ?? "no_message"
.log_level  = to_string(.attributes.level) ?? "unknown"
.timestamp  = to_string(.attributes.time) ?? "unknown"

'''
```

**Purpose**: Uses Vector's Remap Language (VRL) to transform and enrich incoming OTLP logs.

**Transform Logic**:

1. **Validation**: 
   - Ensures required fields (`service.name`, `environment`) are present as strings
   - The use of `abort` ensures malformed OTLP logs (missing required resource fields) are dropped early rather than forwarded with incomplete metadata, which simplifies downstream processing.

2. **Field Extraction & Normalization**:
   - Extracts service name from OTLP resources
   - Extracts environment information
   - Normalizes message, log level, and timestamp from OTLP attributes

3. **Error Handling**:
   - `drop_on_abort = true`: Drops events that fail validation
   - Uses fallback values (e.g., "unknown", "no_message") for missing data
   - `??` operator provides safe defaults for null/missing values

**VRL Features Utilized**:
- **Type Checking**: `is_string()` function validates data types
- **Safe Conversion**: `to_string()` safely converts values to strings
- **Null Coalescing**: `??` operator provides fallback values
- **Conditional Logic**: `if` statements for validation

**Reference**: [Vector Remap Transform](https://vector.dev/docs/reference/configuration/transforms/remap/) | [VRL Functions](https://vector.dev/docs/reference/vrl/)

### Sinks Configuration

#### Kafka Sink with Protocol Buffers

```toml
[sinks.app-logs-kafka]
type = "kafka"
inputs = [ "enrich_otlp" ]
bootstrap_servers = "kafka:29092"
topic = "logs.app.{{.environment_name}}_{{.service_name}}"
compression = "zstd"

#protobuf codec
encoding.codec = "protobuf"
encoding.protobuf.message_type = "logwise.vector.logs.VectorLogs"
encoding.protobuf.desc_file = "/etc/vector/logwise-vector.desc"
```

**Purpose**: Outputs enriched logs to Apache Kafka using Protocol Buffers for efficient serialization.

**Key Configuration Options**:

- **Connection**: 
  - `bootstrap_servers = "kafka:29092"`: Connects to Kafka broker
  - Supports Kafka's automatic topic creation

- **Topic Routing**: 
  - `topic = "logs.app.{{.environment_name}}_{{.service_name}}"`: Dynamic topic assignment
  - Note that {{.environment_name}} and {{.service_name}} come from the VRL transform — if they’re missing or empty, the topic could resolve incorrectly.
  - Creates separate topics per environment/service combination
  - Example topics: `logs.app.development_sample-service`, `logs.app.production_api-service`


- **Performance Optimization**:
  - `compression = "zstd"`: Uses Zstandard compression for optimal size/speed ratio
  - Protocol Buffers provide compact binary serialization

- **Schema Management**:
  - `encoding.codec = "protobuf"`: Enables Protocol Buffers encoding
  - `message_type = "logwise.vector.logs.VectorLogs"`: Specifies the protobuf message type
  - `desc_file = "/etc/vector/logwise-vector.desc"`: Path to compiled protobuf descriptor

**Protocol Buffer Schema**:
```protobuf
syntax = "proto3";

package logwise.vector.logs;

message VectorLogs {
  string service_name = 1;
  string environment_name = 2;
  string message = 3;
  string log_level = 4;
  string timestamp = 5;
}
```

**Reference**: [Vector Kafka Sink](https://vector.dev/docs/reference/configuration/sinks/kafka/) | [Protocol Buffers Codec](https://vector.dev/docs/reference/configuration/sinks/kafka/#protobuf)

## Production Considerations

### Performance & Scalability

1. **Buffer Management**: 
   - Vector automatically manages internal buffers for handling traffic spikes
   - Configure `data_dir` on fast storage (SSD recommended)

2. **Compression**:
   - Zstandard compression reduces network bandwidth by 60-80%
   - Consider `lz4` for CPU-constrained environments requiring faster compression

3. **Kafka Optimization**:
   - Partition topics by service/environment for parallel processing
   - Configure appropriate retention policies based on log volume

### Monitoring & Observability

1. **Health Checks**:
   - Vector API endpoint: `http://localhost:8686/health`
   - Built-in metrics available at: `http://localhost:8686/metrics`

2. **Logging**:
   - Vector internal logs provide insight into processing pipeline
   - Configure log level via environment variable: `VECTOR_LOG=info`

### Security Considerations

1. **Network Security**:
   - OTLP endpoints (4317, 4318) should be accessible only from trusted sources
   - API endpoint (8686) should be restricted to monitoring systems

2. **Authentication**:
   - Consider implementing authentication for OTLP endpoints in production
   - Kafka SASL/SSL can be configured for secure broker communication

### Resource Requirements

- **CPU**: Moderate usage, scales with log volume
- **Memory**: ~100MB base + buffer memory (configured via Vector settings)
- **Disk**: Minimal for Vector state, depends on buffer configuration
- **Network**: Bandwidth scales with log throughput

## Deployment with Docker

The configuration is designed for containerized deployment:

```dockerfile
FROM timberio/vector:0.34.0-alpine

# Install dependencies
RUN apt-get update && apt-get install -y \
    protobuf-compiler \
    && rm -rf /var/lib/apt/lists/*

COPY vector.toml /etc/vector/vector.toml
COPY logwise-vector.proto /app/logwise-vector.proto

# Generate protobuf descriptor file
RUN protoc --proto_path=/app --descriptor_set_out=/etc/vector/logwise-vector.desc --include_imports logwise-vector.proto


EXPOSE 4317 4318 8686

CMD ["--config", "/etc/vector/vector.toml"]
```

**Docker Compose Integration**:
- Volumes mount for persistent data directory
- Health checks via Vector API
- Service dependencies ensure proper startup order

## Troubleshooting

### Common Issues

1. **Connection Refused Errors**:
   - Verify Kafka broker is accessible
   - Check network connectivity and DNS resolution

2. **Schema Validation Failures**:
   - Ensure protobuf descriptor file is correctly mounted
   - Verify message type matches schema definition

3. **High Memory Usage**:
   - Adjust buffer settings in Vector configuration
   - Monitor downstream Kafka consumer lag

### Debug Commands

```bash
# Check Vector health
curl http://localhost:8686/health

# View Vector metrics
curl http://localhost:8686/metrics

# Test OTLP HTTP endpoint
curl -X POST http://localhost:4318/v1/logs \
  -H "Content-Type: application/x-protobuf" \
  --data-binary @sample_logs.pb

# Monitor Kafka topics
kafka-topics --bootstrap-server localhost:9092 --list
```

If this config is used in production, add a line about potential debug transform (optional):
```toml
[sinks.console]
type = "console"
inputs = ["enrich_otlp"]
encoding.codec = "json"
```

## References & Further Reading

- [Vector.dev Official Documentation](https://vector.dev/docs/)
- [Vector Remap Language (VRL) Guide](https://vector.dev/docs/reference/vrl/)
- [OpenTelemetry Protocol Specification](https://opentelemetry.io/docs/specs/otlp/)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Protocol Buffers Documentation](https://developers.google.com/protocol-buffers)

## Contributing

This configuration is part of an open-source observability stack. Contributions and improvements are welcome:

1. Fork the repository
2. Create a feature branch
3. Submit a pull request with detailed description

For questions or issues, please refer to the [Vector.dev Community](https://vector.dev/community/) resources.
