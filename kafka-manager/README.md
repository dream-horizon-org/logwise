# Kafka Manager Setup and Configuration

Kafka Manager setup guide for the Logwise log-central system. Kafka Manager provides management and monitoring capabilities for Kafka clusters, including APIs to retrieve message rates per topic that our orchestrator service uses for automatic partition scaling.

## Overview

Kafka Manager is a web-based tool for managing Apache Kafka clusters. We use Kafka Manager APIs to retrieve topic metrics, particularly message rates per topic, which our orchestrator service uses to automatically scale partitions based on throughput.

**Important**: Kafka Manager requires **Zookeeper-based Kafka** (not KRaft mode) to function properly.

## Installation

### Prerequisites

- Java 8 or higher
- Scala 2.13 or compatible version
- sbt (Scala Build Tool)
- Kafka cluster with Zookeeper running

### Installation Steps

1. **Clone Kafka Manager Repository**

```bash
git clone https://github.com/yahoo/kafka-manager.git
cd kafka-manager
```

2. **Build the Project**

```bash
./sbt clean dist
```

3. **Extract and Setup**

```bash
cd target/universal
unzip kafka-manager-*.zip
cd kafka-manager-*
```

4. **Start Kafka Manager**

```bash
bin/kafka-manager
```

By default, Kafka Manager runs on port **9000**. Access the web interface at `http://localhost:9000`.

### Configuration

Configure Kafka Manager by editing `conf/application.conf`:

```conf
kafka-manager.zkhosts="localhost:2181"
application.secret="changeme"
basicAuthentication.enabled=false
basicAuthentication.username="admin"
basicAuthentication.password="password"
```

## Adding Kafka Cluster

1. **Access Web Interface**
   - Navigate to `http://localhost:9000`

2. **Add Cluster**
   - Click "Cluster" → "Add Cluster"
   - Provide cluster details:
     - **Cluster Name**: Descriptive name (e.g., "Logwise Cluster")
     - **Cluster Zookeeper Hosts**: Comma-separated Zookeeper hosts (e.g., `localhost:2181`)
     - **Kafka Version**: Select your Kafka version
   - **Enable JMX Polling**: Check this box (required for metrics)

3. **Save Configuration**
   - Click "Save" to add the cluster

## API Used for Message Rate Metrics

Our orchestrator service uses the Kafka Manager API to retrieve message rates per topic for automatic partition scaling decisions.

### All Topics Metrics API

**Endpoint**: `/api/clusters/{clusterName}/topics`

**Method**: GET

**Description**: Retrieves metrics for all topics in the cluster, including message rates per topic. This is the only API used by the orchestrator service.

**Parameters**:
- `{clusterName}`: Name of the Kafka cluster (URL-encoded if it contains spaces)

**Response Example**:
```json
{
  "topics": [
    {
      "name": "logs_prod_nginx",
      "messagesPerSec": 45000,
      "partitions": 3,
      "replicationFactor": 3
    },
    {
      "name": "logs_prod_apache",
      "messagesPerSec": 32000,
      "partitions": 3,
      "replicationFactor": 3
    },
    {
      "name": "logs_staging_user-service",
      "messagesPerSec": 15000,
      "partitions": 3,
      "replicationFactor": 3
    }
  ]
}
```

**Response Fields**:
- `name`: Topic name
- `messagesPerSec`: Message throughput rate (messages per second)
- `partitions`: Current number of partitions
- `replicationFactor`: Replication factor for the topic

## How Orchestrator Uses Message Rates

Our orchestrator service periodically queries the `/api/clusters/{clusterName}/topics` API to:

1. **Monitor Message Rates**: Fetches current messages/sec for all topics in one API call
2. **Compare Against Thresholds**: Evaluates if rate exceeds defined thresholds for each topic
3. **Scale Partitions**: Automatically increases partitions when throughput requires it
4. **Track Performance**: Monitors partition utilization and performance metrics


## JMX Configuration (Required for Metrics)

To enable Kafka Manager to collect metrics, JMX must be enabled on Kafka brokers:

### Enable JMX on Kafka Brokers

Set `JMX_PORT` environment variable before starting Kafka:

```bash
export JMX_PORT=9999
bin/kafka-server-start.sh config/server.properties
```

Or add to Kafka startup script:

```bash
#!/bin/bash
export JMX_PORT=9999
export KAFKA_JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.port=9999"
bin/kafka-server-start.sh config/server.properties
```

### Verify JMX is Accessible

```bash
# Test JMX connection
jconsole localhost:9999
```

## API Usage Example

### Get All Topics Metrics

The orchestrator service uses this API to fetch message rates for all topics:

```bash
# Get metrics for all topics (cluster name URL-encoded)
curl http://localhost:9000/api/clusters/Logwise%20Cluster/topics

# Response includes message rates for all topics
{
  "topics": [
    {
      "name": "logs_prod_nginx",
      "messagesPerSec": 45000,
      "partitions": 3,
      "replicationFactor": 3
    },
    {
      "name": "logs_prod_apache",
      "messagesPerSec": 32000,
      "partitions": 3,
      "replicationFactor": 3
    }
  ]
}
```

**Note**: Replace `Logwise%20Cluster` with your actual cluster name (URL-encoded). Spaces should be encoded as `%20`.

## Configuration Example

Complete `conf/application.conf` example:

```conf
# Zookeeper hosts (comma-separated)
kafka-manager.zkhosts="localhost:2181"

# Application secret (change in production)
application.secret="your-secret-key-here"

# Authentication (enable in production)
basicAuthentication.enabled=false
basicAuthentication.username="admin"
basicAuthentication.password="changeme"

# Pinned dispatcher configuration
kafka-manager.update-period=10 seconds

# Feature flags
kafka-manager.consumer.properties.file="conf/consumer.properties"
```

## Verification

After starting Kafka Manager, verify it's working:

1. **Access Web UI**: Navigate to `http://localhost:9000`
2. **Verify Cluster Connection**: Check that your Kafka cluster appears and is online
3. **Check Topics**: View topics list and verify metrics are displayed
4. **Test API**: Use curl command above to test the API endpoint

## Integration with Orchestrator Service

The orchestrator service integrates with Kafka Manager as follows:

1. **Periodic Polling**: Orchestrator polls `/api/clusters/{clusterName}/topics` every configured interval (e.g., every 30 seconds)
2. **Rate Collection**: Retrieves message rates for all topics in a single API call
3. **Decision Making**: Compares rates against thresholds for each topic
4. **Partition Scaling**: Automatically adjusts partitions via Kafka admin APIs when needed based on message rate thresholds

## Troubleshooting

**Kafka Manager cannot connect to Zookeeper**: Verify Zookeeper is running and accessible at the configured host/port.

**No metrics displayed**: Ensure JMX is enabled on Kafka brokers and JMX_PORT is set correctly.

**API returns 404**: Check cluster name is correct and URL-encoded properly in the API call. Ensure the cluster name matches exactly what you configured when adding the cluster in Kafka Manager.

**Message rates not updating**: Verify "Enable JMX Polling" is checked when adding the cluster, and Kafka brokers have JMX enabled.

## Related Documentation

- [Kafka Setup](../kafka/README.md) - Kafka configuration and setup
- [Main Logwise README](../README.md) - Complete system documentation

