# Vector Setup for LogWise

Vector configuration guide for the LogWise log-central system. Vector receives logs from OpenTelemetry collectors, transforms them, and forwards them to Kafka for downstream processing.

## Architecture

```
OpenTelemetry Collector → Vector → Kafka → Log Storage/Processing
```

Vector handles:
- **Ingestion**: Receives OTLP logs via gRPC (4317) and HTTP (4318)
- **Transformation**: Validates, normalizes, and enriches log data
- **Routing**: Publishes logs to Kafka topics organized by type, environment, and service
- **Topic Naming:** `logs.{type}_{environment}_{service}`  
Example: `logs.application_production_api-gateway`

##

## Configuration Overview

| Component | Port | Purpose |
|-----------|------|---------|
| OTLP gRPC | 4317 | Receives logs from OpenTelemetry collectors* |
| OTLP HTTP | 4318 | Alternative HTTP endpoint for OTLP logs |
| Vector API | 8686 | Health checks and monitoring |

> **Note:** *gRPC endpoint (4317) is a required configuration in Vector but not actively used in the LogWise setup. All logs are sent via HTTP (4318).

**Key Configuration Points** (in `vector.toml`):
- `bootstrap_servers`: Kafka broker address 
- `topic`: Dynamic topic routing based on log metadata
- `encoding.codec`: Set to `protobuf` (requires Vector v0.45.0+) to optimize network usage
- `compression`: Set to `zstd` - optimal compression algorithm for protobuf data
- `linger.ms`: Set to `100` - batches logs for up to 100ms before sending to improve throughput
- `batch.size`: Set to `2500000` bytes (~2.5 MB) - sends batch when size limit is reached

## Troubleshooting

### Protobuf Codec Not Recognized

**Error:**
```
ERROR vector::sinks::kafka: Unknown encoding codec: protobuf
```

**Solution:** Vector v0.45.0+ required for Protobuf support.

```bash
# Check version
vector --version

# Upgrade if needed
# Docker: timberio/vector:0.45.0-distroless-libc or later
# Homebrew: brew upgrade vector
# Apt: sudo apt update && sudo apt upgrade vector

# Workaround for older versions: use JSON codec
# encoding.codec = "json"
```

### Kafka Connection Failures

**Error:**
```
ERROR vector::sinks::kafka: Failed to produce message: BrokerTransportFailure
```

**Common cause:** Kafka's `advertised.listeners` misconfigured.

```bash
# Kafka must advertise its reachable hostname/IP, not localhost
# ❌ WRONG: advertised.listeners=PLAINTEXT://localhost:9092
# ✅ CORRECT: advertised.listeners=PLAINTEXT://<YOUR_KAFKA_ADDRESS>

# Verify Kafka's advertised address
kcat -b <YOUR_KAFKA_ADDRESS> -L

# Look for broker metadata:
# broker 1 at localhost:9092         ← Problem: clients outside container can't connect
# broker 1 at <YOUR_KAFKA_ADDRESS> ← Good: resolvable hostname
```

**Other checks:**
- Verify `bootstrap_servers` in `vector.toml` matches Kafka's network address
- Ensure firewall allows connections to Kafka ports
- Test connectivity: `telnet <YOUR_KAFKA_ADDRESS> <KAFKA_PORT>`

## References 

### Files
- **[`vector.toml`](https://github.com/ds-horizon/logwise/blob/main/vector/vector.toml)** - Production ready Vector pipeline configuration
- **[`logwise-vector.proto`](https://github.com/ds-horizon/logwise/blob/main/vector/logwise-vector.proto)** - Production ready Protobuf schema defining log message structure

### Vector Components

- [**OpenTelemetry Receiver**](https://vector.dev/docs/reference/configuration/sources/opentelemetry/)
- [**Remap Transformer**](https://vector.dev/docs/reference/configuration/transforms/remap/)
- [**Kafka Sink**](https://vector.dev/docs/reference/configuration/sinks/kafka/)

