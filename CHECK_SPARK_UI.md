# How to Check Spark UI and Diagnose S3 Push Issues

## Step 1: Check Spark UI Streaming Tab

Since Spark UI is available at **http://localhost:30082/**, follow these steps:

1. **Open Spark UI**: http://localhost:30082/
2. **Find your application**: Look for `app-20260114083517-0000` (or the current running app)
3. **Click on the application** to see details
4. **Go to "Streaming" tab** (or "Streaming Query" tab)
5. **Look for query**: "Export Application Logs To S3"

### What to Check in Spark UI:

#### In the Streaming Tab:
- **Query Status**: Should be "ACTIVE" or "RUNNING"
- **Input Rate**: Should be > 0 if reading from Kafka (if 0, no data is being read)
- **Processing Rate**: Should be > 0 if processing data
- **Output Rate**: Should be > 0 if writing to S3 (if 0, nothing is being written)
- **Last Batch**: Check timestamp - should be recent
- **Errors**: Look for any error messages

#### In the "Executors" Tab:
- Check if executors are running and have tasks
- Look for any failed tasks

#### In the "Stages" Tab:
- Check for failed stages
- Look at stage details for errors

## Step 2: Get Full Driver Logs

The driver logs are in the worker pod stdout. Get them with:

```bash
# Get full logs from the worker that has the driver (spark-worker-7c547454d-st5mc)
kubectl logs -n logwise spark-worker-7c547454d-st5mc --tail=5000 > /tmp/driver-logs.txt

# Then search for application-specific logs
grep -i -E "(MainApplication|Starting|Push|S3|Kafka|Stream|Query|Error|Exception|com\.logwise)" /tmp/driver-logs.txt

# Or view all logs
cat /tmp/driver-logs.txt | tail -500
```

**Look for these key log messages:**
- `<<<************************[Starting Application]************************>>>`
- `Starting PushLogsToS3Runnable...`
- `Starting To Push Application Logs to S3...`
- `Starting kafka stream with kafkaReadStreamOptions`
- `Creating Vector Application Logs DataFrame from Kafka Stream`
- Any `ERROR` or `EXCEPTION` messages

## Step 3: Check Kafka Topics

```bash
# Get Kafka pod
KAFKA_POD=$(kubectl get pods -n logwise -l app=kafka -o jsonpath='{.items[0].metadata.name}')

# List all topics
kubectl exec -n logwise $KAFKA_POD -- kafka-topics.sh --bootstrap-server localhost:9092 --list

# List topics matching pattern
kubectl exec -n logwise $KAFKA_POD -- kafka-topics.sh --bootstrap-server localhost:9092 --list | grep -E "^logs"

# Check message offsets (to see if topics have data)
for topic in $(kubectl exec -n logwise $KAFKA_POD -- kafka-topics.sh --bootstrap-server localhost:9092 --list | grep -E "^logs"); do
    echo "=== Topic: $topic ==="
    kubectl exec -n logwise $KAFKA_POD -- kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list localhost:9092 --topic $topic
done
```

## Step 4: Check if Vector is Producing Logs

```bash
# Check Vector logs
kubectl logs -n logwise -l app=vector-logs --tail=100 | grep -i kafka

# Check if Vector is running
kubectl get pods -n logwise -l app=vector-logs
```

## Common Issues and What Spark UI Will Show:

### Issue 1: No Kafka Data
**Spark UI Shows:**
- Input Rate: 0 records/sec
- Last Batch: Old timestamp or "No data"

**Solution:**
- Check if Kafka topics exist and have messages
- If `kafka.startingOffsets=latest`, produce new messages after Spark starts
- Or change to `kafka.startingOffsets=earliest` to read existing messages

### Issue 2: S3 Write Failure
**Spark UI Shows:**
- Input Rate: > 0 (reading from Kafka)
- Processing Rate: > 0 (processing data)
- Output Rate: 0 (not writing to S3)
- Errors in query details

**Solution:**
- Check AWS credentials
- Verify S3 bucket permissions
- Check driver logs for S3 access errors

### Issue 3: Streaming Query Not Started
**Spark UI Shows:**
- No streaming query visible
- Or query status: FAILED

**Solution:**
- Check driver logs for initialization errors
- Verify all configs are correct
- Check if Spark session initialized properly

## Quick Diagnostic Commands:

```bash
# 1. Get driver logs (full)
kubectl logs -n logwise spark-worker-7c547454d-st5mc --tail=5000 | grep -A 5 -B 5 -i "error\|exception\|starting\|push\|s3\|kafka"

# 2. Check Kafka topics
KAFKA_POD=$(kubectl get pods -n logwise -l app=kafka -o jsonpath='{.items[0].metadata.name}')
kubectl exec -n logwise $KAFKA_POD -- kafka-topics.sh --bootstrap-server localhost:9092 --list

# 3. Check if logs are being produced
kubectl logs -n logwise -l app=vector-logs --tail=50

# 4. Check Spark master for app status
kubectl logs -n logwise -l app=spark-master --tail=100 | grep "app-20260114083517-0000"
```

## Next Steps Based on Spark UI:

1. **If Input Rate = 0**: 
   - Check Kafka topics exist
   - Check if Vector is producing logs
   - Verify Kafka connectivity from Spark

2. **If Input Rate > 0 but Output Rate = 0**:
   - Check S3 credentials and permissions
   - Check driver logs for S3 write errors
   - Verify S3 bucket exists and is accessible

3. **If Query Status = FAILED**:
   - Check driver logs for the error
   - Check Spark UI "Stages" tab for failed stages
   - Verify all configurations are correct

