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

Access Grafana at http://localhost:3000 (default login: `admin` / `admin`).

Alternative installs: refer to Grafana docs for Linux packages or Kubernetes Helm charts.

## 2) Add your logs datasource

1. In Grafana → Connections → Data sources → Add data source
2. Choose your backend:
   - Loki: set URL to your Loki endpoint
   - Elasticsearch: set the HTTP URL and index pattern
3. Save & test

## 3) Create variables for tag-based filters

Create dashboard variables matching Orchestrator tags (e.g., `source`, `team`, `env`).

- Loki example: Type = Query, Query = `label_values(source)`
- Elasticsearch example: Terms on `source.keyword`

Enable Multi-value and Include All if helpful.

## 4) Build a logs dashboard

1. Dashboards → New → New dashboard → Add a panel
2. Use your variables in queries, e.g. `$source`, `$team`, `$env`
3. Pick a Logs or Table panel to display entries

## 5) Secure and persist

- Change admin password via Configuration → Users
- Persist Grafana data by mounting a volume to `/var/lib/grafana` in Docker
- Restrict access (reverse proxy, network rules) for production


