package com.logwise.spark.constants;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ConstantsTest {

  @Test
  public void testConstants_AllFieldsAreNotNull() {
    Assert.assertNotNull(Constants.APP_NAME);
    Assert.assertNotNull(Constants.APPLICATION_CONFIG_DIR);
    Assert.assertNotNull(Constants.X_TENANT_NAME);
    Assert.assertNotNull(Constants.APPLICATION_LOGS_TO_S3_QUERY_NAME);
    Assert.assertNotNull(Constants.APPLICATION_LOGS_KAFKA_GROUP_ID);
    Assert.assertNotNull(Constants.WRITE_STREAM_PARQUET_FORMAT);
    Assert.assertNotNull(Constants.WRITE_STREAM_BQ_FORMAT);
    Assert.assertNotNull(Constants.WRITE_STREAM_GZIP_COMPRESSION);
    Assert.assertNotNull(Constants.CONFIG_KEY_SPARK_CONFIG);
    Assert.assertNotNull(Constants.CONFIG_KEY_SPARK_HADOOP_CONFIG);
  }

  @Test
  public void testConstants_ApplicationLogColumnsAreNotNull() {
    Assert.assertNotNull(Constants.APPLICATION_LOG_COLUMN_DDSOURCE);
    Assert.assertNotNull(Constants.APPLICATION_LOG_COLUMN_DDTAGS);
    Assert.assertNotNull(Constants.APPLICATION_LOG_COLUMN_HOSTNAME);
    Assert.assertNotNull(Constants.APPLICATION_LOG_COLUMN_MESSAGE);
    Assert.assertNotNull(Constants.APPLICATION_LOG_COLUMN_SERVICE_NAME);
    Assert.assertNotNull(Constants.APPLICATION_LOG_COLUMN_SOURCE_TYPE);
    Assert.assertNotNull(Constants.APPLICATION_LOG_COLUMN_STATUS);
    Assert.assertNotNull(Constants.APPLICATION_LOG_COLUMN_TIMESTAMP);
  }

  @Test
  public void testConstants_PartitionColumnsAreNotNull() {
    Assert.assertNotNull(Constants.APPLICATION_LOG_S3_PARTITION_COLUMNS);
    Assert.assertTrue(Constants.APPLICATION_LOG_S3_PARTITION_COLUMNS.length > 0);
  }

  @Test
  public void testConstants_KafkaConfigValuesAreNotNull() {
    Assert.assertNotNull(Constants.KEY_DESERIALIZER_CLASS_CONFIG_VALUE);
    Assert.assertNotNull(Constants.VALUE_DESERIALIZER_CLASS_CONFIG_VALUE);
    Assert.assertNotNull(Constants.GROUP_ID_CONFIG_VALUE);
    Assert.assertNotNull(Constants.AUTO_OFFSET_RESET_CONFIG_VALUE);
    Assert.assertNotNull(Constants.KAFKA_CONSUMER_TIMEOUT);
  }

  @Test
  public void testConstants_FeignConfigValuesAreSet() {
    Assert.assertTrue(Constants.FEIGN_DEFAULT_CONNECTION_TIMEOUT_IN_SECONDS > 0);
    Assert.assertTrue(Constants.FEIGN_DEFAULT_READ_TIMEOUT_IN_SECONDS > 0);
    Assert.assertTrue(Constants.FEIGN_DEFAULT_RETRY_COUNT > 0);
    Assert.assertTrue(Constants.FEIGN_DEFAULT_RETRY_MAX_PERIOD_IN_MILLIS > 0);
    Assert.assertTrue(Constants.FEIGN_DEFAULT_RETRY_PERIOD_IN_MILLIS > 0);
  }

  @Test
  public void testConstants_QueryNameToStageMapIsNotNull() {
    Assert.assertNotNull(Constants.QUERY_NAME_TO_STAGE_MAP);
    Assert.assertFalse(Constants.QUERY_NAME_TO_STAGE_MAP.isEmpty());
  }
}
