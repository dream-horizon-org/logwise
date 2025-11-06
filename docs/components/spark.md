---
title: Apache Spark
---

# Apache Spark

Apache Spark is the **log processing engine** in the LogWise system. It consumes logs from Kafka, transforms them as needed, and writes them to Amazon S3 in partitioned format.

## Overview

Spark handles continuous log processing from Kafka, transforms and enriches log data, and writes it to S3 in a partitioned structure optimized for querying with Athena.

## Architecture in LogWise

```
Kafka Topics → Spark Jobs → S3 (Parquet, Partitioned)
```

Spark handles:
- **Ingestion**: Reads logs from Kafka topics in near real-time
- **Partitioned Storage**: Writes logs to S3 in a hierarchical, time-based partition format
- **Schema Management**: Ensures consistent schema across logs using predefined formats

## Key Features

- **Real-time processing** - Consumes logs from Kafka topics continuously in micro-batch and streaming modes
- **Partitioned storage** - Writes logs to S3 in hierarchical partition format
- **Fault tolerance** - Checkpointing ensures no data loss with exactly-once processing

## Partitioned Storage in S3

Spark writes logs in partitioned directories for efficient query and retrieval. Partition format:
```
/env=<env>/service_name=<service_name>/year=<YYYY>/month=<MM>/day=<DD>/hour=<HH>/minute=<mm>/
```

This structure allows fast filtering based on environment, service, or time ranges when querying with Athena.

## Kafka Integration

- Consumes logs from Kafka topics created by Vector
- Supports automatic topic discovery using regular expressions
- Tracks Kafka offsets for reliable exactly-once processing

## Integration with Other Components

- **Kafka** - Consumes logs from topics
- **S3** - Writes processed logs in Parquet format with partition structure
- **Orchestrator Service** - Monitors job health and manages Spark drivers

## Requirements and Setup

See the Spark setup documentation for installation and configuration.
