#!/bin/bash

set -x

i=1
HEALTH_CHECK_LOG_FILE=/var/log/healthcheck/healthcheck.log
BATCH_SIZE=10  # Number of log entries to write per batch
WAIT_TIME=0.1  # Time to wait between batches

# Create log directory if it doesn't exist
mkdir -p /var/log/healthcheck

while true; do
    # Get timestamp once per batch (more efficient)
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')
    
    # Generate batch of log entries in memory using efficient string building
    batch_logs=""
    for ((j=0; j<BATCH_SIZE; j++)); do
        batch_logs+="${timestamp} - This is a healthcheck log entry $i"$'\n'
        ((i++))
    done
    
    # Write entire batch at once (single system call, buffered I/O)
    printf "%s" "$batch_logs" >> "$HEALTH_CHECK_LOG_FILE"
    
    # Small sleep to prevent excessive CPU usage while maintaining high throughput
    # Reduced sleep time since we're batching more efficiently
    sleep $WAIT_TIME
done

