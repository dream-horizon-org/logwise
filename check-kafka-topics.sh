#!/bin/bash

# Script to check Kafka topics - handles different Kafka distributions
NAMESPACE="logwise"

KAFKA_POD=$(kubectl get pods -n $NAMESPACE -l app=kafka -o jsonpath='{.items[0].metadata.name}')

if [ -z "$KAFKA_POD" ]; then
    echo "Kafka pod not found!"
    exit 1
fi

echo "Kafka Pod: $KAFKA_POD"
echo ""

# Try different methods to list topics
echo "=== Method 1: Using kafka-topics.sh (standard location) ==="
kubectl exec -n $NAMESPACE $KAFKA_POD -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null | grep -E "^logs" || echo "Method 1 failed or no topics found"
echo ""

echo "=== Method 2: Using kafka-topics.sh (alternative location) ==="
kubectl exec -n $NAMESPACE $KAFKA_POD -- find / -name kafka-topics.sh 2>/dev/null | head -1
KAFKA_BIN=$(kubectl exec -n $NAMESPACE $KAFKA_POD -- find / -name kafka-topics.sh 2>/dev/null | head -1)
if [ -n "$KAFKA_BIN" ]; then
    kubectl exec -n $NAMESPACE $KAFKA_POD -- $KAFKA_BIN --bootstrap-server localhost:9092 --list 2>/dev/null | grep -E "^logs" || echo "No topics found matching pattern"
fi
echo ""

echo "=== Method 3: Using kafka-console-consumer to check topics ==="
# This is a workaround - we can check if topics exist by trying to describe them
kubectl exec -n $NAMESPACE $KAFKA_POD -- sh -c 'for topic in logs-application logs-vector logs-system; do echo "Checking topic: $topic"; /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic $topic 2>/dev/null && echo "Topic exists: $topic" || echo "Topic does not exist: $topic"; done' 2>/dev/null || echo "Could not check topics"
echo ""

echo "=== Method 4: Check Kafka container details ==="
kubectl describe pod -n $NAMESPACE $KAFKA_POD | grep -A 5 "Image:"
echo ""

echo "=== Method 5: List all topics (no filter) ==="
kubectl exec -n $NAMESPACE $KAFKA_POD -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list 2>/dev/null || echo "Could not list topics"
echo ""

echo "=== Check Vector logs to see what topics it's writing to ==="
kubectl logs -n $NAMESPACE -l app=vector-logs --tail=50 | grep -i kafka | tail -10

