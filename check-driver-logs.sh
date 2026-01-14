#!/bin/bash

# Script to get Spark driver application logs
# The driver runs as a separate process, so we need to check its logs differently

NAMESPACE="logwise"
APP_ID="app-20260114083517-0000"

echo "=== Finding Driver Process ==="
echo ""

# The driver runs on one of the workers
WORKER_PODS=($(kubectl get pods -n $NAMESPACE -l app=spark-worker -o jsonpath='{.items[*].metadata.name}'))

for worker_pod in "${WORKER_PODS[@]}"; do
    echo "Checking worker: $worker_pod"
    
    # Check if driver process is running in this worker
    DRIVER_PID=$(kubectl exec -n $NAMESPACE $worker_pod -- ps aux | grep "DriverWrapper" | grep -v grep | awk '{print $2}' | head -1)
    
    if [ -n "$DRIVER_PID" ]; then
        echo "Found driver process (PID: $DRIVER_PID) in worker: $worker_pod"
        echo ""
        echo "=== Driver Application Logs (last 200 lines) ==="
        echo "Note: Driver logs are written to stdout/stderr of the driver process"
        echo ""
        
        # The driver logs should be in the worker's stdout, but filtered for the driver
        # Let's get all logs and filter for application-specific messages
        kubectl logs -n $NAMESPACE $worker_pod --tail=2000 | grep -i -E "(MainApplication|com\.logwise|Starting|Push|S3|Kafka|Stream|Query|Error|Exception)" | tail -100
        
        echo ""
        echo "=== Full Driver Logs (if needed) ==="
        echo "Run: kubectl logs -n $NAMESPACE $worker_pod --tail=5000 | grep -A 10 -B 10 'MainApplication'"
        break
    fi
done

echo ""
echo "=== Alternative: Check Spark UI ==="
echo "1. Open http://localhost:30082/"
echo "2. Click on application: $APP_ID"
echo "3. Go to 'Streaming' tab"
echo "4. Check status of 'Export Application Logs To S3' query"
echo "5. Look for Input Rate, Processing Rate, and any errors"
echo ""

echo "=== Check Executor Logs ==="
echo "Executor logs might also contain useful information:"
for worker_pod in "${WORKER_PODS[@]}"; do
    echo "Worker: $worker_pod"
    kubectl logs -n $NAMESPACE $worker_pod --tail=500 | grep -i -E "(executor|error|exception)" | tail -20
    echo ""
done

