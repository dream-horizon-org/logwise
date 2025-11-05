# Spark image for local submit, based on Bitnami Spark
FROM apache/spark-py:3.5.1

USER root
RUN apt-get update -y && apt-get install -y --no-install-recommends \
      curl ca-certificates wget bash && \
    rm -rf /var/lib/apt/lists/*

# Build-time versions (match .env.example). These are for jar downloads.
ARG SPARK_VERSION_MATCH=3.5.1
ARG HADOOP_AWS_VERSION=3.3.4
ARG AWS_SDK_VERSION=1.12.772

ENV SPARK_HOME=/opt/spark

# Download required jars for Kafka and S3 (S3A)
RUN mkdir -p ${SPARK_HOME}/jars && \
    cd ${SPARK_HOME}/jars && \
    wget -q https://repo1.maven.org/maven2/org/apache/spark/spark-sql-kafka-0-10_2.12/${SPARK_VERSION_MATCH}/spark-sql-kafka-0-10_2.12-${SPARK_VERSION_MATCH}.jar && \
    wget -q https://repo1.maven.org/maven2/org/apache/hadoop/hadoop-aws/${HADOOP_AWS_VERSION}/hadoop-aws-${HADOOP_AWS_VERSION}.jar && \
    wget -q https://repo1.maven.org/maven2/com/amazonaws/aws-java-sdk-bundle/${AWS_SDK_VERSION}/aws-java-sdk-bundle-${AWS_SDK_VERSION}.jar

# Submit helper script
COPY submit.sh /opt/submit.sh
RUN chmod +x /opt/submit.sh

USER root

# Default to idle; submit via docker compose exec spark /opt/submit.sh
CMD ["bash", "-lc", "sleep infinity"]


