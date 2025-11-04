---
title: Orchestrator Service
---

# Orchestrator Service

The Orchestrator Service standardizes tags, builds metadata for Grafana dropdowns, and automates Kafka partition scaling based on throughput.

## Responsibilities

- Aggregate and standardize source tags: `type`, `env`, `service_name`
- Periodically fetch S3/Athena partition keys and persist metadata in the database
- Expose APIs used by Grafana to populate dashboard variables
- Monitor Kafka topic message rates and scale partitions when thresholds are exceeded

## Data flow

1. OTEL Collector adds tags to logs
2. Vector converts tags to fields and sends to Kafka
3. Spark writes partitioned datasets to S3
4. Orchestrator derives partition metadata and serves it via APIs


