---
title: Grafana
---

# Grafana

Grafana is the visual layer for log search, filtering, and analysis in Logwise. It presents logs in a table view and supports custom dashboards with dropdown filters. These dropdowns are driven by metadata produced by the Orchestrator Service from source tags.

## What Grafana does in Logwise

- Visualizes logs in tables and panels
- Enables tag-based filters via dashboard variables
- Lets you build reusable dashboards for teams

## Tag-based filters powered by the Orchestrator Service

The Orchestrator Service aggregates metadata from source tags (for example: `type`, `env`, `service_name`). That metadata is used to populate dashboard dropdowns (Grafana variables), ensuring consistent filter options across dashboards.

How this works at a high level:
- The Orchestrator Service standardizes and exposes available tag values.
- Grafana variables reference those tag values (directly from the logs datasource or via an API, depending on your datasource).
- Users select values from dropdowns; all panels on the dashboard get filtered accordingly.

## How to create dashboards with tag-based filters

We provide a ready-to-use dashboard JSON that already includes tag-based filters. Follow these steps:

1) Configure the Orchestrator backend URL
- Update the dashboard links or variables that reference the Orchestrator backend service URL to point to your deployment.

2) Add the Athena datasource in Grafana
- In Grafana, add an Athena datasource and name it exactly `athena`.

3) Import the provided dashboard JSON
- Use the JSON at `grafana/application-logs-dashboard.json` from this repo and import it into Grafana (Dashboards → Import → Upload JSON).

4) Start querying
- The imported dashboard comes with dropdowns powered by the Orchestrator metadata. With the `athena` datasource configured and the Orchestrator URL set, you can start filtering and querying logs immediately.

## How dropdowns get populated from the Orchestrator Service

- All logs pushed by the OTEL Collector include tags: `type`, `env`, `service_name`.
- Vector converts these tags into fields, makes them part of the schema, and sends the data to Kafka.
- Spark reads this schema and writes it to S3, partitioned by these tag fields.
- The Orchestrator Service periodically fetches partition keys, creates metadata in the database, and exposes that metadata via APIs, which Grafana uses to populate dropdowns.

Notes:
- If you maintain an API that exposes allowed tag values (e.g., from Orchestrator), you can populate variables via a suitable Grafana datasource that can query HTTP/JSON. Otherwise, query your logs datasource directly for distinct label/field values.
- Keep variable names aligned with tag keys standardized by Orchestrator to ensure portability across dashboards.


