# Kafka — Self-Host Quick Setup

Minimal steps required to run Kafka for LogWise.

## Prerequisites

- Java 8+
- Linux host (systemd recommended)

## 1) Install Kafka

```bash
# Download Kafka
# https://kafka.apache.org/downloads (choose binary with Scala 2.13)
tar -xzf kafka_2.13-<VERSION>.tgz && cd kafka_2.13-<VERSION>
```

Edit `config/server.properties` — REQUIRED settings for LogWise:

```properties
# Enable Vector to create topics automatically
auto.create.topics.enable=true

# Base partitions for Vector-created topics
num.partitions=3

# Network listeners
listeners=PLAINTEXT://0.0.0.0:9092
# Optional: set an externally reachable address if needed
# advertised.listeners=PLAINTEXT://<YOUR_KAFKA_ADDRESS>:9092
```

Start Kafka:

```bash
bin/kafka-server-start.sh -daemon config/server.properties
```

## 2) Verify Kafka

```bash
# Broker is up
bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092
```

## Notes for LogWise

- `auto.create.topics.enable=true` is required so Vector can publish to new topics.
- `num.partitions=3` sets the default number of partitions for new topics.
- If Vector cannot connect, verify `advertised.listeners` is reachable from the Vector host.


