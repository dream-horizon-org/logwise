# Kafka

Kafka provides a **high-throughput**, **fault-tolerant**, and **scalable** log ingestion layer for the Logwise system.

## Overview

Kafka acts as the message broker that receives processed logs from Vector and buffers them for downstream consumers like Apache Spark.

## Key Features

### High Throughput
- Handles massive log volumes efficiently
- Supports parallel processing across partitions
- Optimized for streaming data

### Fault Tolerance
- Replicated data across brokers
- Automatic failover capabilities
- Data durability guarantees

### Scalability
- Horizontal scaling by adding brokers
- Partition-based parallelism
- Dynamic partition scaling via Orchestrator Service

## Topic Management

Vector dynamically creates **Kafka topics** using tags:
- `type` - Log type classification
- `env` - Environment (dev, staging, prod)
- `service_name` - Service identifier

This automatic topic creation enables organized log routing and processing.

## Topic Naming Convention

Vector automatically creates topics using the naming convention: `{type}_{env}_{service_name}`

**Format:**
- **`type`**: Type of data (e.g., `application`, `kafka`, `mysql`, `nginx`)
- **`env`**: Environment identifier (e.g., `prod`, `staging`, `dev`, `test`)
- **`service_name`**: Service or application name generating the logs

**Examples:**
- `nginx_prod_order-service` - nginx logs from production service order-service
- `application_prod_order-service` - application logs from production service order-service

## Processing Model

Kafka enables:
- **Decoupled processing** - Producers and consumers operate independently
- **Delayed processing** - Logs can be processed later without data loss
- **Batched processing** - Efficient batch consumption by Spark jobs

## Partition Management

- Topics start with **3 partitions** (base count from `num.partitions`)
- The **orchestrator service** automatically monitors message throughput per topic
- Partitions are automatically increased based on defined rate thresholds
- No manual partition adjustment needed - orchestrator handles scaling

## Message Retention

By default, topics have **1 hour retention**. Messages are automatically deleted after 1 hour.

### Configuration

```properties
# Default retention for Vector-created topics
log.retention.hours=1
# OR using milliseconds
log.retention.ms=3600000
```

### When to Increase Retention

Increase retention beyond 1 hour for:
- Compliance or audit requirements (7 days, 30 days, etc.)
- Batch processing with longer intervals
- Recovery scenarios requiring historical data
- Analytics requiring longer historical data

Update retention on existing topics:
```bash
kafka-configs.sh --alter \
  --topic logs_prod_nginx \
  --bootstrap-server localhost:9092 \
  --add-config retention.ms=86400000  # 24 hours
```

## Configuration

See the [Kafka Setup Guide](/setup/kafka) for detailed configuration instructions.

**Important**: We require **Zookeeper-based Kafka** because we use Kafka Manager APIs for metrics, which require Zookeeper.

### Required Settings

- `auto.create.topics.enable=true` - Enable auto topic creation for Vector
- `num.partitions=3` - Base partitions for Vector-created topics
- `zookeeper.connect` - Zookeeper connection (required for Kafka Manager)

## Related Components

- **[Vector](/components/vector)** - Sends logs to Kafka
- **[Kafka Manager](/components/kafka-manager)** - Monitors and manages Kafka cluster
- **[Apache Spark](/components/spark)** - Consumes logs from Kafka
- **[Orchestrator Service](/components/orchestrator)** - Automatically scales Kafka partitions

## Verification

After starting Kafka, verify it's running:

```bash
# Check Kafka is running
kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# List topics (after Vector starts sending logs)
kafka-topics.sh --list --bootstrap-server localhost:9092

# Verify Zookeeper connection
zkCli.sh -server localhost:2181 ls /brokers/ids
```

