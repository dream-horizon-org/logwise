#!/bin/bash

set -x

i=1
HEALTH_CHECK_LOG_FILE=/var/log/healthcheck/healthcheck.log

# Create log directory if it doesn't exist
mkdir -p /var/log/healthcheck

generateMultilineLog() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - This is a healthcheck log entry $i" >> "$HEALTH_CHECK_LOG_FILE"
    echo "This is a second line of the log entry" >> "$HEALTH_CHECK_LOG_FILE"
    echo "This is a third line of the log entry" >> "$HEALTH_CHECK_LOG_FILE"
}

while true; do
    # Every 100th log, generate a multiline stack trace
    if (( i % 100 == 0 )); then
            generateMultilineLog
    else
        echo "$(date '+%Y-%m-%d %H:%M:%S') - This is a healthcheck log entry $i" >> "$HEALTH_CHECK_LOG_FILE"
    fi
    sleep 0.01
    ((i++))
done

