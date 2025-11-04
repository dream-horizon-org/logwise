# Kafka Setup and Configuration

Kafka setup guide for the Logwise log-central system. In our architecture, Vector automatically collects logs and sends them to Kafka, creating topics automatically using our naming convention.

## Overview

Kafka serves as the message streaming platform for high-throughput log data. Vector automatically creates topics when sending logs to Kafka. This guide covers the essential configuration needed.

**Important**: We require **Zookeeper-based Kafka** because we use Kafka Manager APIs for metrics, which require Zookeeper.

## Installation

### Prerequisites

- Java 8 or higher
- Zookeeper

### Installation Steps

1. **Download Kafka**
   - Download a Zookeeper-compatible Kafka version
   - Extract the archive

2. **Install Zookeeper**
   - Download and install Zookeeper
   - Configure `conf/zoo.cfg`:
   ```properties
   dataDir=/var/lib/zookeeper
   clientPort=2181
   ```

3. **Start Services**
   - Start Zookeeper first: `bin/zkServer.sh start`
   - Start Kafka: `bin/kafka-server-start.sh config/server.properties`

## Required Configuration

Edit `config/server.properties` and configure these required settings:

### Auto Topic Creation

Vector automatically creates topics when it sends logs. Auto topic creation **must be enabled**:

```properties
# REQUIRED: Enable auto topic creation for Vector
auto.create.topics.enable=true
```

**Why required**: Vector discovers log sources and creates corresponding Kafka topics automatically. Without this setting, Vector will fail when attempting to send logs to non-existent topics.

### Base Partitions

Set the base number of partitions for topics that Vector creates:

```properties
# REQUIRED: Base partitions for Vector-created topics
num.partitions=3
```

**Base: 3 partitions** - All topics Vector creates start with 3 partitions. The orchestrator service automatically scales partitions based on message throughput, so you don't need to manually adjust them.

### Zookeeper Connection

Configure Zookeeper connection (required for Kafka Manager):

```properties
# REQUIRED: Zookeeper connection (for Kafka Manager)
zookeeper.connect=localhost:2181
zookeeper.session.timeout.ms=18000
```

## Complete Configuration Example

Here's a complete `config/server.properties` example:

```properties
# Broker identity
broker.id=1
listeners=PLAINTEXT://0.0.0.0:9092
log.dirs=/var/kafka-logs

# REQUIRED: Auto topic creation for Vector
auto.create.topics.enable=true

# REQUIRED: Base partitions for Vector-created topics
num.partitions=3

# Replication and high availability
default.replication.factor=3
min.insync.replicas=2

# Retention (default: 1 hour)
log.retention.hours=1
log.retention.check.interval.ms=300000

# REQUIRED: Zookeeper connection (for Kafka Manager)
zookeeper.connect=localhost:2181
zookeeper.session.timeout.ms=18000
zookeeper.connection.timeout.ms=18000
```

## Topic Naming Convention

Vector automatically creates topics using the naming convention: `{type}_{env}_{service_name}`

### Format

- **`type`**: Type of data (e.g., `application`,`kafka`,`mysql`,`nginx`)
- **`env`**: Environment identifier (e.g., `prod`, `staging`, `dev`, `test`)
- **`service_name`**: Service or application name generating the logs

### Examples
- `nginx_prod_order-service` - nginx logs from production service order-service
- `application_prod_order-service` - application logs from production service order-service

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

## Partition Management

- Topics start with **3 partitions** (base count from `num.partitions`)
- The **orchestrator service** automatically monitors message throughput per topic
- Partitions are automatically increased based on defined rate thresholds
- No manual partition adjustment needed - orchestrator handles scaling

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

## Troubleshooting

**Vector cannot send logs**: Ensure `auto.create.topics.enable=true` is set in Kafka configuration bad restart Kafka.

**Topics have wrong partition count**: Check `num.partitions` setting and restart Kafka. Note that partitions can only be increased, never decreased.

**Kafka Manager cannot connect**: Verify Kafka is in Zookeeper mode and Zookeeper is running and accessible.
