---
title: S3 & Athena
---

# S3 & Athena

Amazon S3 and Athena provide the **long-term storage** and **query layer** for LogWise, enabling cost-effective log retention and serverless SQL querying of application logs.

## Overview

S3 serves as the durable object storage backend where processed logs are persisted in Parquet format, while Athena provides a serverless SQL query engine that allows you to query logs directly from S3 without managing infrastructure.

## Architecture in LogWise

```
Spark Jobs → S3 (Parquet) → AWS Glue Data Catalog → Athena → Grafana
```

The data flow:
1. Spark processes logs from Kafka and writes them to S3 in Parquet format
2. S3 stores logs partitioned by metadata tags for efficient querying
3. AWS Glue Data Catalog maintains the schema and partition metadata
4. Athena queries logs directly from S3 using the Glue catalog
5. Grafana visualizes query results from Athena

## Key Features

- **Cost-effective storage** - Pay-per-use pricing with lifecycle policies for archival (S3 Standard → S3 IA → Glacier)
- **Serverless querying** - SQL queries without managing infrastructure, pay-per-query pricing
- **Partitioned storage** - Logs organized by `service_name` and `time` for optimized querying
- **Parquet format** - Columnar storage with high compression (70-90% reduction) and schema evolution support

## Partitioning Strategy

Logs are partitioned in S3 by metadata tags to optimize query performance and reduce costs. The table schema includes two partition keys:
- `service_name` - Service identifier
- `time` - Time-based partition (typically date/hour)

### S3 Path Structure

Logs are organized in S3 with partition keys in the path:
```
s3://bucket-name/logs/
  service_name=order-service/
    time=2024-01-15/
      part-00000.parquet
```

When querying with filters like `WHERE service_name = 'order-service' AND time >= '2024-01-15'`, Athena only scans the relevant partition directories, dramatically reducing scan time and costs.

## Data Format: Parquet

Logs are stored in **Parquet format**, which offers:
- **Columnar storage** - Efficient for analytical queries (only read needed columns)
- **Compression** - Reduces storage costs (typically 70-90% compression)
- **Schema evolution** - Supports adding/modifying columns over time
- **Type safety** - Strong typing for better query performance

## Schema Management

The AWS Glue Data Catalog serves as the metadata store:
- **Database**: `logs` - Contains all log-related tables
- **Table**: `application-logs` - Defines the schema and partition structure
- **Schema fields**: `ddtags`, `hostname`, `message`, `source_type`, `status`, `timestamp` (core fields) plus partition fields

## Query Optimization

Always include partition keys in your WHERE clauses to reduce scan time and costs:
```sql
-- ✅ Good: Scans only prod/order-service partitions
SELECT * FROM logs.application-logs 
WHERE env = 'prod' AND service_name = 'order-service' AND time >= '2024-01-15'

-- ❌ Bad: Scans all partitions (expensive!)
SELECT * FROM logs.application-logs WHERE message LIKE '%error%'
```

## Integration with Other Components

- **Spark** - Writes processed logs to S3 in Parquet format with partition structure
- **Grafana** - Queries Athena as a datasource for log visualization
- **Orchestrator Service** - Monitors S3 partition structure and exposes metadata for Grafana dropdowns

## Requirements and Setup

See the [S3 & Athena Setup Guide](/setup-guides/self-host/s3-athena-setup) for AWS configuration steps.

::: tip Best Practice
Regularly run `MSCK REPAIR TABLE logs.application-logs` in Athena to update partition metadata after Spark writes new partitions to S3.
:::
