---
title: Grafana
---

# Grafana

Grafana is the **visual layer** for log search, filtering, and analysis in Logwise. It presents logs in a table view and supports custom dashboards with dropdown filters powered by metadata from the Orchestrator Service.

## Overview

Grafana visualizes logs from your datasource (Athena) and enables tag-based filtering via dashboard variables. These dropdowns are populated by metadata produced by the Orchestrator Service from source tags.

## Key Features

- **Visualization** - Presents logs in tables and panels
- **Tag-based filters** - Dropdown filters via dashboard variables
- **Reusable dashboards** - Build dashboards for teams with consistent filter options

## How It Works

The Orchestrator Service aggregates metadata from source tags (for example: `environment_name`, `component_type`, `service_name`). That metadata is used to populate dashboard dropdowns (Grafana variables), ensuring consistent filter options across dashboards.

**Data Flow:**
1. All logs pushed by the OTEL Collector include tags: `environment_name`, `component_type`, `service_name`
2. Vector converts these tags into fields, makes them part of the schema, and sends the data to Kafka
3. Spark reads this schema and writes it to S3, partitioned by these tag fields
4. The Orchestrator Service periodically fetches partition keys, creates metadata in the database, and exposes that metadata via APIs
5. Grafana uses this metadata to populate dropdowns (variables)

Users select values from dropdowns; all panels on the dashboard get filtered accordingly.

## Creating Dashboards with Tag-Based Filters

We provide a ready-to-use dashboard JSON that already includes tag-based filters. The process involves:

1. **Configure Orchestrator backend URL** - Update dashboard links or variables that reference the Orchestrator backend service URL to point to your deployment
2. **Add Athena datasource** - In Grafana, add an Athena datasource and name it exactly `athena`. Configure it with your AWS credentials and region
3. **Import dashboard JSON** - Use the JSON at `grafana/provisioning/dashboards/application-logs.json` from this repo and import it into Grafana (Dashboards → Import → Upload JSON)
4. **Start querying** - The imported dashboard comes with dropdowns powered by the Orchestrator metadata. With the `athena` datasource configured and the Orchestrator URL set, you can start filtering and querying logs immediately

## Notes

- If you maintain an API that exposes allowed tag values (e.g., from Orchestrator), you can populate variables via a suitable Grafana datasource that can query HTTP/JSON
- Otherwise, query your logs datasource directly for distinct label/field values
- Keep variable names aligned with tag keys standardized by Orchestrator to ensure portability across dashboards

## Integration with Other Components

- **Athena** - Primary datasource for querying logs
- **Orchestrator Service** - Provides metadata for dashboard dropdown variables

## Requirements and Setup

See the [Grafana Setup Guide](/setup-guides/self-host/grafana-setup) for installation and dashboard configuration.

::: warning Important
By default, Grafana uses **SQLite** as its embedded database (stored at `/var/lib/grafana/grafana.db` in container deployments). For production environments or high-availability setups, configure an external database (PostgreSQL or MySQL).
:::
