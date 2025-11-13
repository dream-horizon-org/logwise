#!/bin/bash
set -e

# Ensure log directory exists and has correct permissions
# Volumes are mounted, but ensure directories exist and are writable
mkdir -p /var/log/healthcheck /var/lib/otelcol/storage

# If running as root, set ownership to otelcol user (UID 10001, GID 10001)
if [ "$(id -u)" = "0" ]; then
    chown -R 10001:10001 /var/log/healthcheck /var/lib/otelcol/storage 2>/dev/null || true
fi

# Start the healthcheck log generator in the background
# Run as root to ensure it can write to the volume
/usr/local/bin/generate-healthcheck-logs.sh &

# Wait a moment for logs to start generating
sleep 2

# Start the OTEL collector as otelcol user
# Use su-exec if available, otherwise use su
if command -v su-exec >/dev/null 2>&1; then
    exec su-exec otelcol /otelcol --config=/etc/otelcol/config.yaml
else
    exec su -s /bin/sh otelcol -c "/otelcol --config=/etc/otelcol/config.yaml"
fi

