#!/bin/bash

# Script to get Spark driver logs - the driver runs as a separate process
NAMESPACE="logwise"
WORKER_POD="spark-worker-7c547454d-st5mc"

echo "=== Checking Driver Process ==="
echo ""

# Check if driver process is running
echo "1. Checking for driver process..."
kubectl exec -n $NAMESPACE $WORKER_POD -- ps aux | grep -E "(DriverWrapper|MainApplication)" | grep -v grep
echo ""

# Check driver logs file (according to log4j.properties, logs go to /opt/spark/logs/spark.log)
echo "2. Checking driver log file (/opt/spark/logs/spark.log)..."
kubectl exec -n $NAMESPACE $WORKER_POD -- ls -la /opt/spark/logs/ 2>/dev/null || echo "Log directory not accessible"
echo ""

# Try to read the log file
echo "3. Reading driver log file (last 200 lines)..."
kubectl exec -n $NAMESPACE $WORKER_POD -- tail -200 /opt/spark/logs/spark.log 2>/dev/null || echo "Could not read log file"
echo ""

# Check driver work directory
echo "4. Checking driver work directory..."
kubectl exec -n $NAMESPACE $WORKER_POD -- ls -la /opt/spark/work/ 2>/dev/null | head -20
echo ""

# Check if driver stdout/stderr files exist
echo "5. Checking for driver stdout/stderr files..."
kubectl exec -n $NAMESPACE $WORKER_POD -- find /opt/spark/work -name "*.out" -o -name "*.err" 2>/dev/null | head -10
echo ""

# Get all logs from worker (driver output should be here)
echo "6. Getting ALL logs from worker pod (unfiltered)..."
echo "This will show driver application logs if they're being written to stdout..."
kubectl logs -n $NAMESPACE $WORKER_POD --tail=1000 | tail -100
echo ""

echo "=== Alternative: Check Spark UI ==="
echo "Open http://localhost:30082/"
echo "1. Find application app-20260114083517-0000"
echo "2. Click on it"
echo "3. Go to 'Executors' tab - check if driver is listed"
echo "4. Go to 'Streaming' tab - check query status"
echo "5. Click on 'Driver' link to see driver logs in UI"

