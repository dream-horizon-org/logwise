# log-management-spark

This repository contains a Spark application that streams log data from Kafka and writes it to S3 in Protobuf format.

## Prerequisites

* Maven 3.2+
* Java 11+
* Spark 3.1.2+
* Kafka cluster
* S3-compatible storage

## Setup
 
Main Class: ```com.logwise.spark.MainApplication```

Run Tests: ```mvn clean verify```

Packaging Command: ```mvn clean package -Dmaven.test.skip```

Fat Jar File Path: ```/target/logwise-spark-{artifact-version}.jar```

## Data Format

The application consumes Protobuf messages from Kafka topics matching the pattern `^logs.*` and writes them to S3 in Parquet format with the following schema:

- message: Log message content
- ddtags: JSON string of tags
- timestamp: ISO timestamp string
- env: Environment name
- service_name: Service name
- component_name: Component name
- hostname: Hostname
- ddsource: Data source
- source_type: Source type
- status: Log status

## Submit Spark Job

### Sample Curl Request

```
curl --location '<SPARK_MASTER_HOST>:6066/v1/submissions/create' \
--header 'Cache-Control: no-cache' \
--header 'Content-Type: application/json;charset=UTF-8' \
--data '{
  "action": "CreateSubmissionRequest",
  "appArgs": [
    "kafka.cluster.dns=<KAFKA_BROKER_DNS>",
    "kafka.maxRatePerPartition=4000",
    "kafka.startingOffsets=latest",
    "kafka.topic.prefix.application=\"^logs.*\"",
    "s3.bucket=<S3_BUCKET_NAME>"
  ],
  "appResource": "<SPARK_JAR_URL>",
  "clientSparkVersion": "3.1.2",
  "mainClass": "com.logwise.spark.MainApplication",
  "environmentVariables": {
    "SPARK_ENV_LOADED": "1"
  },
  "sparkProperties": {
    "spark.app.name": ""logWise"",
    "spark.driver.cores": "3",
    "spark.driver.extraJavaOptions": "-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -Dlog4j.configuration=<LOG4J_PROPERTIES_URL>",
    "spark.driver.maxResultSize": "2000G",
    "spark.driver.memory": "12G",
    "spark.driver.supervise": true,
    "spark.executor.cores": "3",
    "spark.executor.extraJavaOptions": "-XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -Dlog4j.configuration=<LOG4J_PROPERTIES_URL>",
    "spark.executor.memory": "12G",
    "spark.master": "spark://<SPARK_MASTER_HOST>:7077",
    "spark.submit.deployMode": "cluster",
    "spark.scheduler.mode": "FAIR",
    "spark.jars": "<SPARK_JAR_URL>",
    "spark.scheduler.pool": "production",
    "spark.dynamicAllocation.enabled": true,
    "spark.shuffle.service.enabled": true,
    "spark.dynamicAllocation.executorIdleTimeout": 15
  }
}'
```
