package com.logwise.spark.constants;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
  public final String APP_NAME = "log-management";

  public final String APPLICATION_CONFIG_DIR = "application-config";
  public final String X_TENANT_NAME = "X-Tenant-Name";

  public final String APPLICATION_LOGS_TO_S3_QUERY_NAME = "Export Application Logs To S3";

  public final String APPLICATION_LOGS_KAFKA_GROUP_ID = "app-spark-" + randomAlphanumeric(5);

  public final String WRITE_STREAM_PARQUET_FORMAT = "parquet";
  public final String WRITE_STREAM_BQ_FORMAT = "com.google.cloud.spark.bigquery";
  public final String WRITE_STREAM_GZIP_COMPRESSION = "gzip";

  // Config Keys
  public final String CONFIG_KEY_SPARK_CONFIG = "sparkConfig";
  public final String CONFIG_KEY_SPARK_HADOOP_CONFIG = "sparkHadoopConfig";

  // Column Names
  public final String APPLICATION_LOG_COLUMN_DDSOURCE = "ddsource";
  public final String APPLICATION_LOG_COLUMN_DDTAGS = "ddtags";
  public final String APPLICATION_LOG_COLUMN_HOSTNAME = "hostname";
  public final String APPLICATION_LOG_COLUMN_MESSAGE = "message";
  public final String APPLICATION_LOG_COLUMN_SERVICE_NAME = "service_name";
  public final String APPLICATION_LOG_COLUMN_SOURCE_TYPE = "source_type";
  public final String APPLICATION_LOG_COLUMN_STATUS = "status";
  public final String APPLICATION_LOG_COLUMN_TIMESTAMP = "timestamp";
  public final String APPLICATION_LOG_COLUMN_YEAR = "year";
  public final String APPLICATION_LOG_COLUMN_MONTH = "month";
  public final String APPLICATION_LOG_COLUMN_DAY = "day";
  public final String APPLICATION_LOG_COLUMN_HOUR = "hour";
  public final String APPLICATION_LOG_COLUMN_MINUTE = "minute";

  // Partition Columns
  public final String[] APPLICATION_LOG_S3_PARTITION_COLUMNS = {
    Constants.APPLICATION_LOG_COLUMN_SERVICE_NAME,
    Constants.APPLICATION_LOG_COLUMN_YEAR,
    Constants.APPLICATION_LOG_COLUMN_MONTH,
    Constants.APPLICATION_LOG_COLUMN_DAY,
    Constants.APPLICATION_LOG_COLUMN_HOUR,
    Constants.APPLICATION_LOG_COLUMN_MINUTE
  };

  // Kafka Consumer Config
  public final String KEY_DESERIALIZER_CLASS_CONFIG_VALUE =
      "org.apache.kafka.common.serialization.StringDeserializer";
  public final String VALUE_DESERIALIZER_CLASS_CONFIG_VALUE =
      "org.apache.kafka.common.serialization.StringDeserializer";
  public final String GROUP_ID_CONFIG_VALUE = "offsetFinderGroup";
  public final String AUTO_OFFSET_RESET_CONFIG_VALUE = "earliest";
  public final Duration KAFKA_CONSUMER_TIMEOUT = Duration.ofSeconds(10);

  public final Map<String, String> QUERY_NAME_TO_STAGE_MAP =
      ImmutableMap.of(
          APPLICATION_LOGS_TO_S3_QUERY_NAME, "start at ApplicationLogsStreamToS3.java:57");

  public final int FEIGN_DEFAULT_CONNECTION_TIMEOUT_IN_SECONDS = 5;
  public final int FEIGN_DEFAULT_READ_TIMEOUT_IN_SECONDS = 10;
  public final int FEIGN_DEFAULT_RETRY_COUNT = 3;
  public final int FEIGN_DEFAULT_RETRY_MAX_PERIOD_IN_MILLIS = 3000;
  public final int FEIGN_DEFAULT_RETRY_PERIOD_IN_MILLIS = 1000;
}
