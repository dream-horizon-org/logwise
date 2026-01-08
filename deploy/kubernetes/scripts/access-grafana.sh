#!/bin/bash
# Script to access Grafana via port-forwarding
# Usage: ./access-grafana.sh

echo "Setting up port-forwarding to Grafana..."
echo "Grafana will be available at: http://localhost:3000"
echo "Default credentials: admin/admin"
echo ""
echo "Press Ctrl+C to stop port-forwarding"
echo ""

kubectl port-forward -n logwise svc/grafana 3000:3000


