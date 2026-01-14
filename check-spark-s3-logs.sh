#!/bin/bash

# Script to diagnose why Spark logs are not being pushed to S3

NAMESPACE="logwise"
APP_ID="app-20260114083517-0000"

echo "=== Checking Spark Application Status ==="
echo ""

# Get Spark master pod
SPARK_MASTER_POD=$(kubectl get pods -n $NAMESPACE -l app=spark-master -o jsonpath='{.items[0].metadata.name}')
echo "Spark Master Pod: $SPARK_MASTER_POD"
echo ""

# Get Spark worker pods
echo "=== Spark Worker Pods ==="
kubectl get pods -n $NAMESPACE -l app=spark-worker -o wide
echo ""

# Check for driver logs in workers (driver runs on one of the workers)
echo "=== Checking Driver Logs (last 100 lines) ==="
echo "Looking for driver logs containing errors, exceptions, or S3/Kafka related messages..."
echo ""

for worker_pod in $(kubectl get pods -n $NAMESPACE -l app=spark-worker -o jsonpath='{.items[*].metadata.name}'); do
    echo "--- Checking worker: $worker_pod ---"
    kubectl logs -n $NAMESPACE $worker_pod --tail=500 | grep -i -E "(driver|error|exception|s3|kafka|stream|query|starting|push)" | tail -30
    echo ""
done

echo "=== Checking Spark Master Logs for Application Status ==="
kubectl logs -n $NAMESPACE $SPARK_MASTER_POD --tail=200 | grep -i "$APP_ID" | tail -20
echo ""

echo "=== Checking Kafka Topics ==="
KAFKA_POD=$(kubectl get pods -n $NAMESPACE -l app=kafka -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
if [ -n "$KAFKA_POD" ]; then
    echo "Kafka Pod: $KAFKA_POD"
    echo "Listing Kafka topics matching '^logs.*':"
    kubectl exec -n $NAMESPACE $KAFKA_POD -- kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null | grep -E "^logs" || echo "No topics found matching pattern"
    echo ""
    
    echo "Checking if there are messages in logs topics:"
    for topic in $(kubectl exec -n $NAMESPACE $KAFKA_POD -- kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null | grep -E "^logs"); do
        echo "Topic: $topic"
        kubectl exec -n $NAMESPACE $KAFKA_POD -- kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic $topic 2>/dev/null || echo "Could not get offsets"
    done
else
    echo "Kafka pod not found or not accessible"
fi
echo ""

echo "=== Checking Orchestrator Logs for Spark Job Submission ==="
ORCH_POD=$(kubectl get pods -n $NAMESPACE -l app=orchestrator -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
if [ -n "$ORCH_POD" ]; then
    echo "Orchestrator Pod: $ORCH_POD"
    kubectl logs -n $NAMESPACE $ORCH_POD --tail=100 | grep -i -E "(spark|s3|bucket|kafka)" | tail -20
else
    echo "Orchestrator pod not found"
fi
echo ""

echo "=== Summary ==="
echo "1. Check if Kafka topics exist and have messages"
echo "2. Check driver logs for errors/exceptions"
echo "3. Verify S3 bucket access and permissions"
echo "4. Check if streaming query is active and processing data"
echo ""
echo "To view full driver logs, check the worker pod that launched the driver"
echo "Driver was launched on worker with IP 10.244.0.15:36391"

