# Diagnosis Steps for Spark S3 Push Issue

## Critical Finding: Driver Logs Not Visible

The driver logs are **not appearing** in the worker pod stdout. This is because:
- The driver runs as a separate Java process (`DriverWrapper`)
- Its stdout/stderr may not be captured by `kubectl logs`
- Logs should be in `/opt/spark/logs/spark.log` according to log4j.properties

## Immediate Actions:

### 1. Check Driver Logs File Directly

```bash
# Check if driver log file exists
kubectl exec -n logwise spark-worker-7c547454d-st5mc -- ls -la /opt/spark/logs/

# Read driver logs from file
kubectl exec -n logwise spark-worker-7c547454d-st5mc -- tail -500 /opt/spark/logs/spark.log | grep -i -E "(MainApplication|Starting|Push|S3|Kafka|Stream|Query|Error|Exception|com\.logwise)"
```

### 2. Check Spark UI (MOST RELIABLE)

Open **http://localhost:30082/** and:

1. **Find your application**: `app-20260114083517-0000`
2. **Click on the application name**
3. **Go to "Executors" tab**:
   - Check if "driver" executor is listed
   - Click on "stdout" or "stderr" links to see driver logs
4. **Go to "Streaming" tab**:
   - Look for query: "Export Application Logs To S3"
   - Check status (ACTIVE, FAILED, etc.)
   - Check Input Rate (should be > 0 if reading from Kafka)
   - Check Processing Rate
   - Check Output Rate (should be > 0 if writing to S3)
   - Click on query name to see detailed logs

### 3. Check Kafka Topics (Confluent Kafka)

Since you're using Confluent Kafka (`confluentinc/cp-kafka:7.6.0`), use:

```bash
KAFKA_POD=$(kubectl get pods -n logwise -l app=kafka -o jsonpath='{.items[0].metadata.name}')

# List topics (Confluent Kafka path)
kubectl exec -n logwise $KAFKA_POD -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# List topics matching pattern
kubectl exec -n logwise $KAFKA_POD -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list | grep -E "^logs"

# Check offsets (to see if topics have messages)
kubectl exec -n logwise $KAFKA_POD -- /opt/kafka/bin/kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic <topic-name>
```

### 4. Check if Driver Process is Running

```bash
# Check if driver process exists
kubectl exec -n logwise spark-worker-7c547454d-st5mc -- ps aux | grep DriverWrapper
```

### 5. Check Vector is Producing Logs

```bash
# Check Vector logs
kubectl logs -n logwise -l app=vector-logs --tail=100 | grep -i kafka

# Check Vector pod status
kubectl get pods -n logwise -l app=vector-logs
```

## Most Likely Issues:

### Issue 1: No Kafka Data (Most Likely)
**Symptom**: Spark UI shows Input Rate = 0

**Reason**: `kafka.startingOffsets=latest` means Spark only reads NEW messages after it starts. If no new messages are produced, nothing will be written to S3.

**Solution**:
- Produce new log messages to Kafka after Spark starts
- Or change `kafka.startingOffsets` to `earliest` in orchestrator config

### Issue 2: Driver Not Starting Properly
**Symptom**: No driver logs, no application in Spark UI

**Check**: Spark UI → Applications → See if app is listed

### Issue 3: S3 Write Failure
**Symptom**: Input Rate > 0, Processing Rate > 0, but Output Rate = 0

**Check**: Spark UI → Streaming → Query details → Look for errors

## Quick Diagnostic Commands:

```bash
# 1. Get driver logs from file
kubectl exec -n logwise spark-worker-7c547454d-st5mc -- tail -1000 /opt/spark/logs/spark.log 2>/dev/null | grep -i -E "(error|exception|starting|push|s3|kafka)" | tail -50

# 2. Check Kafka topics (Confluent)
KAFKA_POD=$(kubectl get pods -n logwise -l app=kafka -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n logwise $KAFKA_POD -- /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# 3. Check Vector
kubectl logs -n logwise -l app=vector-logs --tail=50

# 4. Check Spark UI at http://localhost:30082/
#    - Go to application
#    - Check Streaming tab
#    - Check Executors tab → Driver → stdout/stderr
```

## Next Steps:

1. **FIRST**: Check Spark UI at http://localhost:30082/ - this is the most reliable way to see what's happening
2. **SECOND**: Get driver logs from `/opt/spark/logs/spark.log` file
3. **THIRD**: Check Kafka topics to see if data exists
4. **FOURTH**: Check if Vector is producing logs to Kafka

