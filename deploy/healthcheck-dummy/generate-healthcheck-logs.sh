#!/bin/bash

set -x

i=1
HEALTH_CHECK_LOG_FILE=/var/log/healthcheck/healthcheck.log

# Create log directory if it doesn't exist
mkdir -p /var/log/healthcheck

while true; do
    echo "$(date '+%Y-%m-%d %H:%M:%S') - This is a healthcheck log entry $i" >> "$HEALTH_CHECK_LOG_FILE"
    sleep 1
    ((i++))
done

