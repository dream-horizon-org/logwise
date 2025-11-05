---
title: Vector
---

# Vector

Vector is the **log ingestion and transformation** component in LogWise. It receives logs from OpenTelemetry collectors, transforms them, and forwards them to Kafka for downstream processing.

## Overview

Vector handles log ingestion via OTLP (OpenTelemetry Protocol), validates and normalizes log data, and routes logs to Kafka topics organized by type, environment, and service.

## Architecture in LogWise

```
OpenTelemetry Collector → Vector → Kafka → Spark Jobs
```

Vector handles:
- **Ingestion**: Receives OTLP logs via gRPC (4317) and HTTP (4318)
- **Transformation**: Validates, normalizes, and enriches log data
- **Routing**: Publishes logs to Kafka topics organized by type, environment, and service

## Key Features

- **OTLP ingestion** - Receives logs via HTTP (4318) and gRPC (4317)
- **Transformation** - Validates, normalizes, and enriches log data
- **Dynamic routing** - Publishes logs to Kafka topics based on metadata tags
- **Protobuf encoding** - Optimized network usage with compression (requires Vector v0.45.0+)

## Topic Routing

Vector automatically creates Kafka topics using the naming convention: `{type}_{env}_{service_name}` based on log metadata tags.

**Example:** `application_prod_order-service`

## Configuration

Key configuration points in `vector.toml`:
- `bootstrap_servers` - Kafka broker address
- `topic` - Dynamic topic routing based on log metadata
- `encoding.codec` - Set to `protobuf` (requires Vector v0.45.0+) to optimize network usage
- `compression` - Set to `zstd` - optimal compression algorithm for protobuf data
- `linger.ms` - Set to `100` - batches logs for up to 100ms before sending to improve throughput
- `batch.size` - Set to `2500000` bytes (~2.5 MB) - sends batch when size limit is reached

> **Note:** gRPC endpoint (4317) is a required configuration in Vector but not actively used in the LogWise setup. All logs are sent via HTTP (4318).

## Common Issues

**Protobuf Codec Not Recognized:**
- Error: `Unknown encoding codec: protobuf`
- Solution: Vector v0.45.0+ required for Protobuf support. Upgrade Vector or use JSON codec as workaround.

**Kafka Connection Failures:**
- Common cause: Kafka's `advertised.listeners` misconfigured
- Kafka must advertise its reachable hostname/IP, not localhost
- Verify `bootstrap_servers` in `vector.toml` matches Kafka's network address

## Integration with Other Components

- **OpenTelemetry Collector** - Receives logs via OTLP HTTP
- **Kafka** - Publishes transformed logs to topics

## Requirements and Setup

See the [Vector Setup Guide](/docs/setup-guides/self-host/vector-setup) for installation and configuration.
