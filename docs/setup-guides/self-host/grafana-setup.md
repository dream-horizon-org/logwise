---
title: Grafana (Self-Host)
---

# Grafana — Self-Hosted Setup

Follow these steps to run Grafana for Logwise and connect it to your logs datasource.

## Prerequisites

- A running logs datasource (e.g., Loki, Elasticsearch) that stores logs enriched with Orchestrator tags
- Docker installed (recommended path below) or a compatible package manager

## 1) Run Grafana

Using Docker:

```bash
docker run -d \
  --name grafana \
  -p 3000:3000 \
  -e GF_SECURITY_ADMIN_USER=admin \
  -e GF_SECURITY_ADMIN_PASSWORD=admin \
  grafana/grafana:latest
```

Access Grafana at `http://localhost:3000` (default login: `admin` / `admin`).

::: warning Important
The default Grafana Docker image uses **SQLite** as its embedded database. The database file is stored at `/var/lib/grafana/grafana.db` inside the container. For production deployments or multi-instance setups, configure an external database (PostgreSQL, MySQL) using environment variables like `GF_DATABASE_TYPE`, `GF_DATABASE_HOST`, etc.
:::

Alternative installs: refer to Grafana docs for Linux packages or Kubernetes Helm charts.

## 2) Add the Athena datasource

1. In Grafana → Connections → Data sources → Add data source
2. Choose Amazon Athena and configure it for your AWS account
3. Name the datasource exactly `athena`
4. Save & test

## 3) Configure Orchestrator URL

1. Update the Orchestrator backend service URL used by the dashboard:
   - Option A: Edit the JSON file [application-logs.json](../../grafana/provisioning/dashboards/application-logs.json) before importing to set your service URL
   - Option B: After import, update dashboard links/variables that reference the Orchestrator backend
2. Ensure the `athena` datasource will be used for panels/variables

## 4) Import the provided dashboard JSON and start querying

1. In Grafana: Dashboards → Import → Upload JSON → select the file and import
2. Use the dropdowns to filter by `service_name` and start querying logs

 


