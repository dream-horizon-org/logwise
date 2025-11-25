# Spark 3.1.2 Dockerfile optimized for Java JAR applications
# Using multi-stage build for better caching and build time optimization
# syntax=docker/dockerfile:1.4

# Base stage with Java 11
FROM eclipse-temurin:11-jdk AS base

# Set Spark version
ARG SPARK_VERSION=3.1.2
ARG HADOOP_VERSION=3.2

# Set environment variables
ENV SPARK_HOME=/opt/spark
ENV PATH=$PATH:${SPARK_HOME}/bin:${SPARK_HOME}/sbin
ENV SPARK_VERSION=${SPARK_VERSION}
# JAVA_HOME is already set in eclipse-temurin:11-jdk base image

# Install minimal dependencies for Java JAR applications only
# Using cache mount for apt packages to speed up rebuilds
RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update --allow-releaseinfo-change && \
    apt-get install -y --no-install-recommends \
    bash \
    ca-certificates \
    curl \
    file \
    aria2 \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Create spark user and app directory (cached layer)
RUN groupadd -r spark && useradd -r -g spark spark && \
    mkdir -p /opt/app && \
    chown -R spark:spark /opt/app

# Download Spark in separate layer for better caching
FROM base AS spark-downloader
ARG SPARK_VERSION=3.1.2
ARG HADOOP_VERSION=3.2
WORKDIR /tmp
RUN SPARK_FILE="spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz" && \
    echo "Downloading Spark ${SPARK_VERSION} with aria2 (multi-connection + mirrors)..." && \
    aria2c -c -x 16 -s 16 -k 1M --timeout=60 --max-tries=5 --retry-wait=3 \
      -o "${SPARK_FILE}" \
      "https://archive.apache.org/dist/spark/spark-${SPARK_VERSION}/${SPARK_FILE}" \
      "https://dlcdn.apache.org/spark/spark-${SPARK_VERSION}/${SPARK_FILE}" \
      "https://downloads.apache.org/spark/spark-${SPARK_VERSION}/${SPARK_FILE}" \
      "https://mirrors.ocf.berkeley.edu/apache/spark/spark-${SPARK_VERSION}/${SPARK_FILE}" && \
    echo "Verifying download..." && \
    file "${SPARK_FILE}" && \
    (file "${SPARK_FILE}" | grep -q "gzip\\|POSIX tar" || \
     (echo "ERROR: Invalid archive" && head -c 200 "${SPARK_FILE}" && exit 1))

# Download JARs in a separate stage for better caching
# This stage is cached separately, so JAR downloads only happen when versions change
FROM base AS jar-downloader
ARG HADOOP_AWS_VERSION=3.2.0
ARG AWS_SDK_VERSION=1.12.767
ARG SPARK_KAFKA_VERSION=3.1.2
ARG KAFKA_CLIENTS_VERSION=2.6.0
ARG SPARK_TOKEN_PROVIDER_VERSION=3.1.2
ARG COMMONS_POOL2_VERSION=2.6.2
ARG LZ4_VERSION=1.7.1
ARG ZSTD_JNI_VERSION=1.4.8-1
ARG SNAPPY_JAVA_VERSION=1.1.8.4
WORKDIR /tmp/jars
# Download all JARs sequentially (more reliable than parallel in Docker)
# Each download is cached separately if the version doesn't change
RUN echo "Downloading required JARs..." && \
    curl -L -f --connect-timeout 30 --max-time 300 --retry 2 --retry-delay 3 --progress-bar \
    -o "hadoop-aws-${HADOOP_AWS_VERSION}.jar" \
    "https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/${HADOOP_AWS_VERSION}/hadoop-aws-${HADOOP_AWS_VERSION}.jar" && \
    curl -L -f --connect-timeout 30 --max-time 300 --retry 2 --retry-delay 3 --progress-bar \
    -o "aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar" \
    "https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/${AWS_SDK_VERSION}/aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar" && \
    curl -L -f --connect-timeout 30 --max-time 300 --retry 2 --retry-delay 3 --progress-bar \
    -o "spark-sql-kafka-0-10_2.12-${SPARK_KAFKA_VERSION}.jar" \
    "https://repo1.maven.org/maven2/org/apache/spark/spark-sql-kafka-0-10_2.12/${SPARK_KAFKA_VERSION}/spark-sql-kafka-0-10_2.12-${SPARK_KAFKA_VERSION}.jar" && \
    curl -L -f --connect-timeout 30 --max-time 300 --retry 2 --retry-delay 3 --progress-bar \
    -o "spark-token-provider-kafka-0-10_2.12-${SPARK_TOKEN_PROVIDER_VERSION}.jar" \
    "https://repo1.maven.org/maven2/org/apache/spark/spark-token-provider-kafka-0-10_2.12/${SPARK_TOKEN_PROVIDER_VERSION}/spark-token-provider-kafka-0-10_2.12-${SPARK_TOKEN_PROVIDER_VERSION}.jar" && \
    curl -L -f --connect-timeout 30 --max-time 300 --retry 2 --retry-delay 3 --progress-bar \
    -o "kafka-clients-${KAFKA_CLIENTS_VERSION}.jar" \
    "https://repo1.maven.org/maven2/org/apache/kafka/kafka-clients/${KAFKA_CLIENTS_VERSION}/kafka-clients-${KAFKA_CLIENTS_VERSION}.jar" && \
    # Kafka runtime deps: commons-pool2 (for consumer caching), compression codecs
    curl -L -f --connect-timeout 30 --max-time 300 --retry 2 --retry-delay 3 --progress-bar \
    -o "commons-pool2-${COMMONS_POOL2_VERSION}.jar" \
    "https://repo1.maven.org/maven2/org/apache/commons/commons-pool2/${COMMONS_POOL2_VERSION}/commons-pool2-${COMMONS_POOL2_VERSION}.jar" && \
    curl -L -f --connect-timeout 30 --max-time 300 --retry 2 --retry-delay 3 --progress-bar \
    -o "lz4-java-${LZ4_VERSION}.jar" \
    "https://repo1.maven.org/maven2/org/lz4/lz4-java/${LZ4_VERSION}/lz4-java-${LZ4_VERSION}.jar" && \
    curl -L -f --connect-timeout 30 --max-time 300 --retry 2 --retry-delay 3 --progress-bar \
    -o "zstd-jni-${ZSTD_JNI_VERSION}.jar" \
    "https://repo1.maven.org/maven2/com/github/luben/zstd-jni/${ZSTD_JNI_VERSION}/zstd-jni-${ZSTD_JNI_VERSION}.jar" && \
    curl -L -f --connect-timeout 30 --max-time 300 --retry 2 --retry-delay 3 --progress-bar \
    -o "snappy-java-${SNAPPY_JAVA_VERSION}.jar" \
    "https://repo1.maven.org/maven2/org/xerial/snappy/snappy-java/${SNAPPY_JAVA_VERSION}/snappy-java-${SNAPPY_JAVA_VERSION}.jar" && \
    echo "All JARs downloaded successfully"

# Build stage - Build Spark application JAR using Maven
FROM maven:3.9-eclipse-temurin-11 AS spark-app-builder
WORKDIR /app

# Copy pom.xml first (for better layer caching)
COPY ./pom.xml .

# Download dependencies with parallel downloads
RUN mvn -B -T 4 dependency:go-offline -Dmaven.artifact.threads=10

# Copy source code
COPY ./src ./src

# Build the application JAR with the specified Maven command
RUN mvn -B -T 4 clean package -Dmaven.test.skip -Dmaven.artifact.threads=10

# Final stage - extract and setup (optimized for Java JAR apps)
FROM base
ARG SPARK_VERSION=3.1.2
ARG HADOOP_VERSION=3.2

# Copy Spark archive from downloader stage
COPY --from=spark-downloader /tmp/spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz /tmp/

# Extract Spark (single RUN command for better caching)
WORKDIR /tmp
RUN SPARK_FILE="spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION}.tgz" && \
    echo "Extracting Spark..." && \
    tar -xzf "${SPARK_FILE}" && \
    mv spark-${SPARK_VERSION}-bin-hadoop${HADOOP_VERSION} ${SPARK_HOME} && \
    rm "${SPARK_FILE}" && \
    rm -rf ${SPARK_HOME}/python ${SPARK_HOME}/pyspark ${SPARK_HOME}/bin/pyspark* ${SPARK_HOME}/bin/spark-submit.cmd 2>/dev/null || true && \
    echo "Spark ${SPARK_VERSION} extracted successfully"

# Copy JARs from jar-downloader stage (faster than downloading again)
COPY --from=jar-downloader /tmp/jars/*.jar ${SPARK_HOME}/jars/

# Copy the built Spark application JAR from the build stage to /opt/app/app.jar
RUN mkdir -p /opt/app
COPY --from=spark-app-builder /app/target/logwise-spark-*.jar /opt/app/app.jar

# Setup directories and permissions in a single layer
RUN echo "Setting up directories and permissions..." && \
    # Ensure proper permissions
    chown -R spark:spark ${SPARK_HOME} && \
    mkdir -p ${SPARK_HOME}/logs ${SPARK_HOME}/work && \
    chown -R spark:spark ${SPARK_HOME}/logs ${SPARK_HOME}/work && \
    # Create Spark worker configuration to set memory explicitly
    mkdir -p ${SPARK_HOME}/conf && \
    echo "spark.worker.memory 2048m" > ${SPARK_HOME}/conf/spark-defaults.conf && \
    echo "spark.worker.cores 2" >> ${SPARK_HOME}/conf/spark-defaults.conf && \
    chown -R spark:spark ${SPARK_HOME}/conf && \
    # Create app directory for JAR files
    mkdir -p /opt/app && \
    chown -R spark:spark /opt/app && \
    echo "Spark ${SPARK_VERSION} installed successfully (Java 11, optimized with S3 support)"

# Set working directory
WORKDIR ${SPARK_HOME}

# Run as root (matching apache/spark:latest behavior)
# This allows docker-compose commands to create directories and files
USER root

# Expose ports
# 7077: Spark master port
# 8080: Spark master web UI
# 8081: Spark worker web UI
# 6066: Spark REST API
EXPOSE 7077 8080 8081 6066

# Default command (can be overridden in docker-compose)
CMD ["/bin/bash"]
