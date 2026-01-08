# Local Overlay

This overlay is configured for local development with kind clusters.

## Resource Constraints

Local kind clusters typically have limited resources. Some pods may remain in `Pending` state due to insufficient memory/CPU. This is expected and does not affect core functionality.

### Expected Pending Pods

- **otel-collector**: DaemonSet that collects logs from host. May be pending if cluster is resource-constrained.
- **grafana**: May be pending if cluster doesn't have enough memory.

### Core Services

The following services are required and should be running:
- ✅ orchestrator
- ✅ kafka
- ✅ spark-master
- ✅ spark-worker
- ✅ vector-logs
- ✅ MySQL databases

## Resource Optimizations

This overlay includes resource optimizations for local development:
- Reduced otel-collector resources (128Mi memory instead of 256Mi)
- NodePort services for easy access

## Accessing Services

Services are exposed via NodePort:
- Orchestrator: http://localhost:30081
- Grafana: http://localhost:30080 (if running)
- Spark Master: http://localhost:30082
- Spark Worker UI: http://localhost:30083
- Vector OTLP: http://localhost:30418

## Troubleshooting

If pods remain pending:
1. Check cluster resources: `kubectl describe node`
2. Consider increasing Docker Desktop memory allocation
3. Some pods (like otel-collector) are optional for local development


