#!/usr/bin/env bash
set -euo pipefail

# Env inputs
: "${KAFKA_BROKERS:?KAFKA_BROKERS is required}"
: "${KAFKA_TOPIC:?KAFKA_TOPIC is required}"
: "${S3_BUCKET:?S3_BUCKET is required}"
: "${S3_PREFIX:?S3_PREFIX is required}"
SPARK_STREAMING=${SPARK_STREAMING:-true}
CHECKPOINT_DIR=${CHECKPOINT_DIR:-/opt/checkpoints}
AWS_REGION=${AWS_REGION:-us-east-1}

SPARK_HOME=${SPARK_HOME:-/opt/spark}
APP_FILE=${APP_FILE:-/opt/app/app.py}
APP_JAR=${APP_JAR:-}
MAIN_CLASS=${MAIN_CLASS:-}
mkdir -p "$(dirname "$APP_FILE")" "$CHECKPOINT_DIR"

# If no app file is provided, generate a minimal PySpark job
if [ -z "$APP_JAR" ] && [ ! -f "$APP_FILE" ]; then
  cat > "$APP_FILE" << 'PYAPP'
from pyspark.sql import SparkSession
from pyspark.sql.functions import col, from_json, to_timestamp
from pyspark.sql.types import StructType, StructField, StringType
import os

spark = (
    SparkSession.builder.appName("KafkaToS3Parquet")
    .config("spark.sql.shuffle.partitions", "4")
    .getOrCreate()
)

spark.sparkContext.setLogLevel("WARN")

brokers = os.environ["KAFKA_BROKERS"]
topic = os.environ["KAFKA_TOPIC"]
s3_bucket = os.environ["S3_BUCKET"]
s3_prefix = os.environ["S3_PREFIX"].rstrip("/")
checkpoint_dir = os.environ.get("CHECKPOINT_DIR", "/opt/checkpoints")
is_streaming = os.environ.get("SPARK_STREAMING", "true").lower() == "true"

schema = StructType([
    StructField("level", StringType()),
    StructField("message", StringType()),
    StructField("app", StringType()),
    StructField("ts", StringType()),
])

if is_streaming:
    df = (
        spark.readStream.format("kafka")
        .option("kafka.bootstrap.servers", brokers)
        .option("subscribe", topic)
        .option("startingOffsets", "latest")
        .load()
    )
    parsed = (
        df.selectExpr("CAST(value AS STRING) AS raw")
        .select(from_json(col("raw"), schema).alias("j"))
        .select("j.*")
        .withColumn("timestamp", to_timestamp(col("ts")))
    )
    out_path = f"s3a://{s3_bucket}/{s3_prefix}/"
    q = (
        parsed.writeStream
        .format("parquet")
        .option("path", out_path)
        .option("checkpointLocation", checkpoint_dir)
        .outputMode("append")
        .start()
    )
    q.awaitTermination()
else:
    df = (
        spark.read.format("kafka")
        .option("kafka.bootstrap.servers", brokers)
        .option("subscribe", topic)
        .option("startingOffsets", "earliest")
        .load()
    )
    parsed = (
        df.selectExpr("CAST(value AS STRING) AS raw")
        .select(from_json(col("raw"), schema).alias("j"))
        .select("j.*")
        .withColumn("timestamp", to_timestamp(col("ts")))
    )
    out_path = f"s3a://{s3_bucket}/{s3_prefix}/"
    parsed.write.mode("append").parquet(out_path)

spark.stop()
PYAPP
fi

# Ensure required jars exist (Kafka + S3A) aligned to Spark 3.1.2
SPARK_VERSION_MATCH=${SPARK_VERSION_MATCH:-3.1.2}
HADOOP_AWS_VERSION=${HADOOP_AWS_VERSION:-3.2.0}
AWS_SDK_VERSION=${AWS_SDK_VERSION:-1.11.375}
JARS_DIR="${SPARK_HOME}/jars"
mkdir -p "$JARS_DIR"
if [ ! -f "$JARS_DIR/spark-sql-kafka-0-10_2.12-${SPARK_VERSION_MATCH}.jar" ]; then
  wget -q -O "$JARS_DIR/spark-sql-kafka-0-10_2.12-${SPARK_VERSION_MATCH}.jar" \
    "https://repo1.maven.org/maven2/org/apache/spark/spark-sql-kafka-0-10_2.12/${SPARK_VERSION_MATCH}/spark-sql-kafka-0-10_2.12-${SPARK_VERSION_MATCH}.jar"
fi
# Spark 3.1.x requires token provider jar for Kafka
if [ ! -f "$JARS_DIR/spark-token-provider-kafka-0-10_2.12-${SPARK_VERSION_MATCH}.jar" ]; then
  wget -q -O "$JARS_DIR/spark-token-provider-kafka-0-10_2.12-${SPARK_VERSION_MATCH}.jar" \
    "https://repo1.maven.org/maven2/org/apache/spark/spark-token-provider-kafka-0-10_2.12/${SPARK_VERSION_MATCH}/spark-token-provider-kafka-0-10_2.12-${SPARK_VERSION_MATCH}.jar"
fi
# Kafka connector caches consumers via commons-pool2
if ! ls "$JARS_DIR"/commons-pool2-*.jar >/dev/null 2>&1; then
  wget -q -O "$JARS_DIR/commons-pool2-2.6.2.jar" \
    "https://repo1.maven.org/maven2/org/apache/commons/commons-pool2/2.6.2/commons-pool2-2.6.2.jar"
fi
# Compression codec runtime libs for Kafka (lz4, zstd, snappy)
if ! ls "$JARS_DIR"/lz4-java-*.jar >/dev/null 2>&1; then
  wget -q -O "$JARS_DIR/lz4-java-1.7.1.jar" \
    "https://repo1.maven.org/maven2/org/lz4/lz4-java/1.7.1/lz4-java-1.7.1.jar"
fi
if ! ls "$JARS_DIR"/zstd-jni-*.jar >/dev/null 2>&1; then
  wget -q -O "$JARS_DIR/zstd-jni-1.4.8-1.jar" \
    "https://repo1.maven.org/maven2/com/github/luben/zstd-jni/1.4.8-1/zstd-jni-1.4.8-1.jar"
fi
if ! ls "$JARS_DIR"/snappy-java-*.jar >/dev/null 2>&1; then
  wget -q -O "$JARS_DIR/snappy-java-1.1.8.4.jar" \
    "https://repo1.maven.org/maven2/org/xerial/snappy/snappy-java/1.1.8.4/snappy-java-1.1.8.4.jar"
fi
if [ ! -f "$JARS_DIR/hadoop-aws-${HADOOP_AWS_VERSION}.jar" ]; then
  wget -q -O "$JARS_DIR/hadoop-aws-${HADOOP_AWS_VERSION}.jar" \
    "https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/${HADOOP_AWS_VERSION}/hadoop-aws-${HADOOP_AWS_VERSION}.jar"
fi
if [ ! -f "$JARS_DIR/aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar" ]; then
  wget -q -O "$JARS_DIR/aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar" \
    "https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/${AWS_SDK_VERSION}/aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar"
fi

# Spark settings: S3A + master URL
# For us-east-1, use s3.amazonaws.com (no region prefix)
# For other regions, use s3-<region>.amazonaws.com
if [[ "${AWS_REGION}" == "us-east-1" ]]; then
  S3A_ENDPOINT="s3.amazonaws.com"
else
  S3A_ENDPOINT="s3-${AWS_REGION}.amazonaws.com"
fi

S3A_OPTS=(--conf spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
          --conf spark.hadoop.fs.s3a.path.style.access=false
          --conf spark.hadoop.fs.s3a.endpoint=${S3A_ENDPOINT})

# Pass credentials explicitly so executors don't rely on inheriting env
if [[ -n "${AWS_SESSION_TOKEN:-}" ]]; then
  S3A_OPTS+=(
    --conf spark.hadoop.fs.s3a.aws.credentials.provider=org.apache.hadoop.fs.s3a.TemporaryAWSCredentialsProvider
    --conf spark.hadoop.fs.s3a.access.key="${AWS_ACCESS_KEY_ID}"
    --conf spark.hadoop.fs.s3a.secret.key="${AWS_SECRET_ACCESS_KEY}"
    --conf spark.hadoop.fs.s3a.session.token="${AWS_SESSION_TOKEN}"
    --conf spark.driverEnv.AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}"
    --conf spark.driverEnv.AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}"
    --conf spark.driverEnv.AWS_SESSION_TOKEN="${AWS_SESSION_TOKEN}"
    --conf spark.driverEnv.AWS_REGION="${AWS_REGION}"
    --conf spark.executorEnv.AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}"
    --conf spark.executorEnv.AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}"
    --conf spark.executorEnv.AWS_SESSION_TOKEN="${AWS_SESSION_TOKEN}"
    --conf spark.executorEnv.AWS_REGION="${AWS_REGION}"
  )
else
  S3A_OPTS+=(
    --conf spark.hadoop.fs.s3a.aws.credentials.provider=org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider
    --conf spark.hadoop.fs.s3a.access.key="${AWS_ACCESS_KEY_ID}"
    --conf spark.hadoop.fs.s3a.secret.key="${AWS_SECRET_ACCESS_KEY}"
    --conf spark.driverEnv.AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}"
    --conf spark.driverEnv.AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}"
    --conf spark.driverEnv.AWS_REGION="${AWS_REGION}"
    --conf spark.executorEnv.AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}"
    --conf spark.executorEnv.AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}"
    --conf spark.executorEnv.AWS_REGION="${AWS_REGION}"
  )
fi
MASTER_URL=${SPARK_MASTER_URL:-spark://spark-master:7077}

if [ -n "$APP_JAR" ]; then
  # Try to auto-detect Main-Class from JAR manifest if not provided
  if [ -z "$MAIN_CLASS" ] && command -v unzip >/dev/null 2>&1; then
    DETECTED=$(unzip -p "$APP_JAR" META-INF/MANIFEST.MF 2>/dev/null | awk -F": " 'tolower($1)=="main-class"{print $2; exit}')
    if [ -n "$DETECTED" ]; then
      MAIN_CLASS="$DETECTED"
      echo "Detected MAIN_CLASS from manifest: $MAIN_CLASS"
    fi
  fi
  echo "Submitting Spark JAR: $APP_JAR (class=${MAIN_CLASS:-<none>}) to ${MASTER_URL}"
  if [ -n "$MAIN_CLASS" ]; then
    "${SPARK_HOME}/bin/spark-submit" \
      --name kafka-to-s3-parquet-jar \
      --master "${MASTER_URL}" \
      "${S3A_OPTS[@]}" \
      --class "$MAIN_CLASS" \
      "$APP_JAR"
  else
    echo "ERROR: No MAIN_CLASS provided and none found in JAR manifest. Set MAIN_CLASS in .env or via make spark-run-jar." >&2
    exit 2
  fi
else
  echo "Submitting PySpark job (streaming=${SPARK_STREAMING}) to write parquet to s3://${S3_BUCKET}/${S3_PREFIX}"
  "${SPARK_HOME}/bin/spark-submit" \
    --name kafka-to-s3-parquet \
    --master "${MASTER_URL}" \
    "${S3A_OPTS[@]}" \
    "$APP_FILE"
fi

# Touch ready file if desired for health checks
touch /opt/spark-ready || true


