# Spark Standalone Cluster Setup (Spark 3.1.2 + Java 11 + S3 Support)

This guide explains how to set up a **Spark Standalone Cluster** with one **Master** and one or more **Workers**.  
It also includes optional steps to run Workers in an **Auto Scaling Group (ASG)** and automatically discover the Master
via EC2 instance tags.

---

## ✅ Versions Used

| Component | Version                                  |
|-----------|------------------------------------------|
| Java      | Corretto 11 / OpenJDK 11                 |
| Spark     | **3.1.2** (Hadoop 3.2 build)             |
| OS        | Amazon Linux 2 / Ubuntu                  |
| Storage   | S3 Supported (Hadoop AWS + AWS SDK Jars) |

# Common Setup (Run on Both Master & Worker)

## Install Java 11

```bash
sudo yum install java-11-amazon-corretto -y
# Or for Ubuntu:
# sudo apt-get install -y openjdk-11-jdk
```

##  Download & Install Spark

```bash 
cd /root
wget https://archive.apache.org/dist/spark/spark-3.1.2/spark-3.1.2-bin-hadoop3.2.tgz
tar -xvzf spark-3.1.2-bin-hadoop3.2.tgz
mv spark-3.1.2-bin-hadoop3.2 spark
```

### Add to PATH (~/.bashrc) and Apply:

```bash 
export SPARK_HOME=/root/spark
export PATH=$PATH:$SPARK_HOME/bin:$SPARK_HOME/sbin
source ~/.bashrc
```

## Add S3 Support Jars 

```bash
mkdir /root/spark-jars
wget -P /root/spark-jars https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/3.2.0/hadoop-aws-3.2.0.jar
wget -P /root/spark-jars https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/1.11.901/aws-java-sdk-bundle-1.11.901.jar

cp /root/spark-jars/*.jar $SPARK_HOME/jars/

```

# Master Setup (Run only on Master node)

### Enable REST API on Spark Master

### Edit:

```bash
nano $SPARK_HOME/conf/spark-defaults.conf
```

### Add:

```bash
spark.master.rest.enabled true
```

### Start Master:

```bash
$SPARK_HOME/sbin/start-master.sh
```

### Spark UI:

```bash
http://<MASTER_PUBLIC_IP>:8080
```

### Note the master URL (example):

```bash 
spark://<MASTER_PUBLIC_IP>:7077
```

# Worker Setup (Run only on Worker nodes)

### Edit Worker environment file:

```bash
nano $SPARK_HOME/conf/spark-env.sh
```

### Add:

```bash
SPARK_WORKER_OPTS="$SPARK_WORKER_OPTS -Dspark.shuffle.service.enabled=true"
```


### Start Worker and attach to Master:

```bash
$SPARK_HOME/sbin/start-worker.sh spark://<MASTER_PRIVATE_IP>:7077
```

## Verify Cluster Status

### Open:

```bash
http://<MASTER_PUBLIC_IP>:8080
```

You should see all Workers under Workers section.


##  Running a Spark Job on the Cluster (REST API Submit)

Once the Spark cluster (Master + Workers) is up and running, you can submit jobs using the Spark REST Submission API (
port 6066).

This repository includes a folder named `spark/`, which contains the complete Spark streaming application source code.

## Building the Spark Job JAR

Before submitting the job, you need to build the JAR file from the source code.

### Prerequisites

- Java 11 (JDK) installed
- Maven 3.6+ installed

### Build Steps

1. Navigate to the spark directory:
```bash
cd spark/
```

2. Build the JAR using Maven:
```bash
mvn clean package -DskipTests
```

This will create a JAR file in the `target/` directory. The JAR file will be named something like:
- `logwise-spark-<VERSION>-SNAPSHOT.jar`

3. (Optional) If you want to include tests:
```bash
mvn clean package
```

4. Verify the JAR was created:
```bash
ls -lh target/*.jar
```

### Host the JAR

Once built, you need to host the JAR file in a location accessible by the Spark cluster.

You may store the JAR in S3 or any reachable artifact location. Then reference it when submitting the job as mentioned
below

### Submit the job to Spark Master

```bash
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
    "spark.app.name": "logWise",
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



