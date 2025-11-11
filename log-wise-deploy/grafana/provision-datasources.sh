#!/bin/bash
# Wait for Grafana to be ready
echo "Waiting for Grafana to be ready..."
for i in {1..30}; do
    if curl -sf http://localhost:3000/api/health > /dev/null 2>&1; then
        echo "Grafana is ready!"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

# Wait a bit more for plugins to be fully initialized
sleep 5

# Add Athena datasource
echo "Adding Athena datasource..."
curl -X POST http://admin:${GF_SECURITY_ADMIN_PASSWORD:-admin}@localhost:3000/api/datasources \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Athena",
    "uid": "athena-main",
    "type": "grafana-athena-datasource",
    "access": "proxy",
    "editable": true,
    "jsonData": {
      "region": "'${AWS_REGION}'",
      "catalog": "'${ATHENA_CATALOG}'",
      "database": "'${ATHENA_DATABASE}'",
      "workgroup": "'${ATHENA_WORKGROUP}'",
      "outputLocation": "'${S3_ATHENA_OUTPUT}'"
    }
  }' 2>/dev/null | grep -q "Datasource added" && echo "Athena datasource added" || echo "Athena datasource may already exist"

# Add Infinity datasource
echo "Adding Infinity datasource..."
curl -X POST http://admin:${GF_SECURITY_ADMIN_PASSWORD:-admin}@localhost:3000/api/datasources \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Infinity",
    "uid": "dewe67f9vnv28f",
    "type": "yesoreyeram-infinity-datasource",
    "access": "proxy",
    "editable": true,
    "jsonData": {}
  }' 2>/dev/null | grep -q "Datasource added" && echo "Infinity datasource added" || echo "Infinity datasource may already exist"

echo "Datasource provisioning complete!"

