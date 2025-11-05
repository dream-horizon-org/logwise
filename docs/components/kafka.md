---
title: Kafka
---

# Kafka

Kafka provides a **high-throughput**, **fault-tolerant**, and **scalable** log ingestion layer for the Logwise system.

## Overview

Kafka acts as the message broker that receives processed logs from Vector and buffers them for downstream consumers like Apache Spark.

## Architecture in LogWise

```
Vector → Kafka → Spark Jobs
```

Kafka enables:
- **Decoupled processing** - Producers and consumers operate independently
- **Delayed processing** - Logs can be processed later without data loss
- **Batched processing** - Efficient batch consumption by Spark jobs

## Key Features

- **High throughput** - Handles massive log volumes efficiently with parallel processing across partitions
- **Fault tolerance** - Replicated data across brokers with automatic failover
- **Scalability** - Horizontal scaling by adding brokers with partition-based parallelism
- **Dynamic partition scaling** - Automatically increases partitions via Orchestrator Service based on throughput

## Topic Management

Vector dynamically creates Kafka topics using tags: `type`, `env`, and `service_name`. Topics follow the naming convention: `{type}_{env}_{service_name}`.

**Format:**
- `type` - Type of data (e.g., `application`, `kafka`, `mysql`, `nginx`)
- `env` - Environment identifier (e.g., `prod`, `staging`, `dev`, `test`)
- `service_name` - Service or application name generating the logs

**Examples:**
- `nginx_prod_order-service` - nginx logs from production order-service
- `application_prod_order-service` - application logs from production order-service

This automatic topic creation enables organized log routing and processing.

## Partition Management

- Topics start with **3 partitions** (base count from `num.partitions`)
- The orchestrator service automatically monitors message throughput per topic
- Partitions are automatically increased based on defined rate thresholds
- No manual partition adjustment needed - orchestrator handles scaling

## Message Retention

By default, topics have **1 hour retention**. Messages are automatically deleted after 1 hour.

Increase retention beyond 1 hour for:
- Compliance or audit requirements (7 days, 30 days, etc.)
- Batch processing with longer intervals
- Recovery scenarios requiring historical data

## Kafka Manager

Kafka Manager provides management and monitoring for Kafka clusters. Logwise uses Kafka Manager APIs to retrieve per-topic message rates that drive the orchestrator's automatic partition scaling.

**Features:**
- Central UI and API for Kafka operations
- Exposes per-topic metrics (messages/sec) consumed by the orchestrator
- Requires JMX enabled on Kafka brokers

**API Used:**
- Endpoint: `/api/clusters/{clusterName}/topics`
- Method: GET
- Returns metrics for all topics including `messagesPerSec`, `partitions`, and `replicationFactor`

**How the Orchestrator Uses It:**
1. Periodically queries the topics API
2. Compares messages/sec with configured thresholds
3. Increases partitions via Kafka admin APIs when needed
4. Monitors utilization and performance

## Integration with Other Components

- **Vector** - Publishes logs to Kafka topics
- **Spark** - Consumes logs from Kafka topics for processing
- **Orchestrator Service** - Monitors topic metrics and scales partitions automatically

## Requirements and Setup

See the [Kafka Setup Guide](/setup-guides/self-host/kafka-setup) for installation and configuration.

::: warning Important
Use Zookeeper-based Kafka (required for Kafka Manager metrics integration).
:::
