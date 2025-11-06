# Kafka + Kafka Manager — Self-Host Quick Setup

Minimal steps required to run Kafka (Zookeeper mode) and Kafka Manager for LogWise.

## Prerequisites

- Java 8+
- Linux host (systemd recommended)

## 1) Install and start Zookeeper

```bash
# Install Zookeeper (use your OS package or tarball)
# Configure conf/zoo.cfg with at least:
# dataDir=/var/lib/zookeeper
# clientPort=2181

# Start Zookeeper
zkServer.sh start
# or
systemctl start zookeeper
```

## 2) Install Kafka (Zookeeper-based)

```bash
# Download a Zookeeper-compatible Kafka (2.x classic)
# https://kafka.apache.org/downloads (choose binary with Scala 2.13)
tar -xzf kafka_2.13-<VERSION>.tgz && cd kafka_2.13-<VERSION>
```

Edit `config/server.properties` — REQUIRED settings for LogWise:

```properties
# Enable Vector to create topics automatically
auto.create.topics.enable=true

# Base partitions for Vector-created topics
num.partitions=3

# Zookeeper connection (required for Kafka Manager)
zookeeper.connect=localhost:2181

# Network listeners
listeners=PLAINTEXT://0.0.0.0:9092
# Optional: set an externally reachable address if needed
# advertised.listeners=PLAINTEXT://<YOUR_KAFKA_ADDRESS>:9092
```

Start Kafka:

```bash
# Start after Zookeeper is up
bin/kafka-server-start.sh -daemon config/server.properties
```

## 3) Verify Kafka

```bash
# Broker is up
bin/kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# Zookeeper has broker registered
zkCli.sh -server localhost:2181 ls /brokers/ids
```

## 4) Install Kafka Manager (CMAK)

```bash
# Get CMAK (Kafka Manager)
# Releases: https://github.com/yahoo/CMAK/releases

# Example using packaged start (adjust for your install method)
export ZK_HOSTS="localhost:2181"

# In conf/application.conf ensure:
# cmak.zkhosts="localhost:2181"

# Start CMAK (default port 9000)
bin/cmak -Dconfig.file=conf/application.conf -Dhttp.port=9000 &
```

Then open `http://<server>:9000`, add a cluster:
- Cluster Zookeeper hosts: `localhost:2181`
- Enable JMX polling if needed

## Notes for LogWise

- `auto.create.topics.enable=true` is required so Vector can publish to new topics.
- `num.partitions=3` sets the default number of partitions for new topics.
- If Vector cannot connect, verify `advertised.listeners` is reachable from the Vector host.


