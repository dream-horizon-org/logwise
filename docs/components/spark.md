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
- **Automatic scaling** - Adjusts worker count based on historical stage metrics via Orchestrator Service

## Partitioned Storage in S3

Spark writes logs in partitioned directories for efficient query and retrieval. Partition format:
```
/env=<env>/service_name=<service_name>/year=<YYYY>/month=<MM>/day=<DD>/hour=<HH>/minute=<mm>/
```

This structure allows fast filtering based on environment, service, or time ranges when querying with Athena.

## Autoscaling Logic

Spark automatically adjusts worker count based on historical stage metrics:

1. **Stage History Collection** - After each job, metrics for completed stages are collected:
   - `inputRecords` - number of records processed
   - `outputBytes` - size of output data
   - `coresUsed` - CPU cores utilized
   - Submission & completion timestamps

2. **Input Growth Analysis** - The orchestrator inspects the last N stages to determine if input records are incremental or consistent across stages, and computes an incremental buffer to anticipate growth

3. **Worker Calculation** - Using tenant configuration (`perCoreLogsProcess`), calculates expected executor cores: `expectedExecutorCores = ceil(maxInputRecordsWithBuffer / perCoreLogsProcess)`, then converts to worker count while respecting tenant min/max limits

4. **Scaling Decisions** - Orchestrator decides worker scaling for next job based on metrics. Upscales if workload exceeds capacity, downscales if below thresholds (only if configured conditions are met)

This ensures efficient resource usage and handles variable log volumes without manual intervention.

## Kafka Integration

- Consumes logs from Kafka topics created by Vector
- Supports automatic topic discovery using regular expressions
- Tracks Kafka offsets for reliable exactly-once processing

## Integration with Other Components

- **Kafka** - Consumes logs from topics
- **S3** - Writes processed logs in Parquet format with partition structure
- **Orchestrator Service** - Sends stage metrics and receives scaling decisions

## Requirements and Setup

See the Spark setup documentation for installation and configuration.
