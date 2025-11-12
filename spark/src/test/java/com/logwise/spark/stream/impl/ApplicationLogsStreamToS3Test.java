package com.logwise.spark.stream.impl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;
import static org.testng.Assert.*;

import com.logwise.spark.base.BaseSparkTest;
import com.logwise.spark.constants.Constants;
import com.logwise.spark.services.KafkaService;
import com.logwise.spark.utils.ConfigUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.HashMap;
import java.util.Map;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder;
import org.apache.spark.sql.streaming.DataStreamWriter;
import org.apache.spark.sql.streaming.OutputMode;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.mockito.MockedStatic;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for ApplicationLogsStreamToS3.
 *
 * <p>Tests the S3 streaming implementation including: - Constructor behavior and dependency
 * injection - Inheritance verification - Configuration handling - Protected method coverage
 */
public class ApplicationLogsStreamToS3Test extends BaseSparkTest {

  // Test constants
  private static final int DEFAULT_PROCESSING_TIME_SECONDS = 60;
  private static final String DEFAULT_CHECKPOINT_PATH = "s3://test-bucket/checkpoints/app-logs";
  private static final String DEFAULT_LOGS_PATH = "s3://test-bucket/logs/app-logs";
  private static final String DEFAULT_KAFKA_DNS = "test-kafka.local";
  private static final long DEFAULT_STARTING_OFFSETS_TIMESTAMP = 0L;
  private static final String DEFAULT_STARTING_OFFSETS = "latest";
  private static final String DEFAULT_TOPIC_PREFIX = "app-logs-.*";
  private static final String DEFAULT_MAX_RATE_PER_PARTITION = "1000";
  private static final long DEFAULT_OFFSET_PER_TRIGGER = 10000L;

  private ApplicationLogsStreamToS3 stream;
  private Config config;
  private KafkaService mockKafkaService;

  @BeforeMethod
  @Override
  public void setUp() {
    super.setUp();
    config = createTestConfig();
    mockKafkaService = mock(KafkaService.class);
    stream = new ApplicationLogsStreamToS3(config, mockKafkaService);
  }

  /**
   * Creates a test configuration with default values.
   *
   * @return Config instance with test values
   */
  private Config createTestConfig() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("spark.processing.time.seconds", DEFAULT_PROCESSING_TIME_SECONDS);
    configMap.put("s3.path.checkpoint.application", DEFAULT_CHECKPOINT_PATH);
    configMap.put("s3.path.logs.application", DEFAULT_LOGS_PATH);
    configMap.put("kafka.cluster.dns", DEFAULT_KAFKA_DNS);
    configMap.put("kafka.startingOffsetsTimestamp", DEFAULT_STARTING_OFFSETS_TIMESTAMP);
    configMap.put("kafka.startingOffsets", DEFAULT_STARTING_OFFSETS);
    configMap.put("kafka.topic.prefix.application", DEFAULT_TOPIC_PREFIX);
    configMap.put("kafka.maxRatePerPartition", DEFAULT_MAX_RATE_PER_PARTITION);
    configMap.put("spark.offsetPerTrigger.default", DEFAULT_OFFSET_PER_TRIGGER);
    // Add empty spark.config to ensure ConfigUtils.getSparkConfig() returns empty map
    configMap.put("spark.config", new HashMap<String, Object>());
    return ConfigFactory.parseMap(configMap);
  }

  /**
   * Creates a test configuration with custom values.
   *
   * @param processingTimeSeconds processing time in seconds
   * @param checkpointPath S3 checkpoint path
   * @param logsPath S3 logs path
   * @return Config instance with custom values
   */
  private Config createCustomTestConfig(
      int processingTimeSeconds, String checkpointPath, String logsPath) {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("spark.processing.time.seconds", processingTimeSeconds);
    configMap.put("s3.path.checkpoint.application", checkpointPath);
    configMap.put("s3.path.logs.application", logsPath);
    configMap.put("kafka.cluster.dns", DEFAULT_KAFKA_DNS);
    configMap.put("kafka.startingOffsetsTimestamp", DEFAULT_STARTING_OFFSETS_TIMESTAMP);
    configMap.put("kafka.startingOffsets", DEFAULT_STARTING_OFFSETS);
    configMap.put("kafka.topic.prefix.application", DEFAULT_TOPIC_PREFIX);
    configMap.put("kafka.maxRatePerPartition", DEFAULT_MAX_RATE_PER_PARTITION);
    configMap.put("spark.offsetPerTrigger.default", DEFAULT_OFFSET_PER_TRIGGER);
    configMap.put("spark.config", new HashMap<String, Object>());
    return ConfigFactory.parseMap(configMap);
  }

  /**
   * Sets up mocks for Spark Dataset transformation chain (map, withColumn).
   *
   * @param mockKafkaDataset the Kafka dataset mock
   * @return the mapped dataset mock (after transformations)
   */
  private Dataset<Row> setupDatasetTransformationMocks(Dataset<Row> mockKafkaDataset) {
    Dataset<Row> mockMappedDataset = mock(Dataset.class);
    when(mockKafkaDataset.map(any(MapFunction.class), any(ExpressionEncoder.class)))
        .thenReturn(mockMappedDataset);
    when(mockMappedDataset.withColumn(anyString(), any(Column.class)))
        .thenReturn(mockMappedDataset);
    return mockMappedDataset;
  }

  /**
   * Sets up mocks for Spark DataStreamWriter chain (writeStream, queryName, trigger, etc.).
   *
   * @param mockMappedDataset the dataset mock to write from
   * @return the mock DataStreamWriter (for verification purposes)
   */
  private DataStreamWriter<Row> setupDataStreamWriterMocks(Dataset<Row> mockMappedDataset) {
    DataStreamWriter<Row> mockWriter = mock(DataStreamWriter.class);
    StreamingQuery mockQuery = mock(StreamingQuery.class);

    when(mockMappedDataset.writeStream()).thenReturn(mockWriter);
    doReturn(mockWriter).when(mockWriter).queryName(anyString());
    doReturn(mockWriter).when(mockWriter).trigger(any());
    doReturn(mockWriter).when(mockWriter).outputMode(any(OutputMode.class));
    doReturn(mockWriter).when(mockWriter).format(anyString());
    // partitionBy has two overloads: varargs (String...) and Scala Seq - mock both explicitly
    doReturn(mockWriter)
        .when(mockWriter)
        .partitionBy(Constants.APPLICATION_LOG_S3_PARTITION_COLUMNS);
    doReturn(mockWriter).when(mockWriter).partitionBy(any(scala.collection.Seq.class));
    doReturn(mockWriter).when(mockWriter).option(anyString(), anyString());
    doReturn(mockWriter).when(mockWriter).options(anyMap());
    doReturn(mockQuery).when(mockWriter).start(anyString());

    return mockWriter;
  }

  @Test
  public void testConstructor_WithValidParameters_CreatesInstance() {
    // Act
    ApplicationLogsStreamToS3 newStream = new ApplicationLogsStreamToS3(config, mockKafkaService);

    // Assert
    assertNotNull(newStream);
  }

  @Test
  public void testConstructor_WithNullConfig_CreatesInstance() {
    // Note: Guice will ensure config is not null in production code
    // This test verifies constructor signature works

    // Act
    ApplicationLogsStreamToS3 newStream = new ApplicationLogsStreamToS3(null, mockKafkaService);

    // Assert
    assertNotNull(newStream);
  }

  @Test
  public void testConstructor_WithNullKafkaService_CreatesInstance() {
    // Note: Guice will ensure kafkaService is not null in production code
    // This test verifies constructor signature works

    // Act
    ApplicationLogsStreamToS3 newStream = new ApplicationLogsStreamToS3(config, null);

    // Assert
    assertNotNull(newStream);
  }

  @Test
  public void testConstructor_WithDifferentConfig_CreatesInstance() {
    // Arrange
    Config customConfig =
        createCustomTestConfig(120, "s3://another-bucket/checkpoints", "s3://another-bucket/logs");

    // Act
    ApplicationLogsStreamToS3 newStream =
        new ApplicationLogsStreamToS3(customConfig, mockKafkaService);

    // Assert
    assertNotNull(newStream);
  }

  @Test
  public void testConstructor_ExtendsAbstractApplicationLogsStream() {
    // Assert - Verify inheritance
    assertTrue(
        stream instanceof AbstractApplicationLogsStream,
        "ApplicationLogsStreamToS3 should extend AbstractApplicationLogsStream");
  }

  @Test
  public void testMultipleInstances_CanBeCreated() {
    // Act - Create multiple instances
    ApplicationLogsStreamToS3 stream1 = new ApplicationLogsStreamToS3(config, mockKafkaService);
    ApplicationLogsStreamToS3 stream2 = new ApplicationLogsStreamToS3(config, mockKafkaService);
    ApplicationLogsStreamToS3 stream3 = new ApplicationLogsStreamToS3(config, mockKafkaService);

    // Assert - All should be non-null and independent
    assertNotNull(stream1);
    assertNotNull(stream2);
    assertNotNull(stream3);
    assertNotSame(stream1, stream2);
    assertNotSame(stream2, stream3);
    assertNotSame(stream1, stream3);
  }

  @Test
  public void testGetVectorApplicationLogsStreamQuery_CallsPushApplicationLogsToS3() {
    // Arrange
    Dataset<Row> mockKafkaDataset = mock(Dataset.class);
    Dataset<Row> mockMappedDataset = setupDatasetTransformationMocks(mockKafkaDataset);
    DataStreamWriter<Row> mockWriter = setupDataStreamWriterMocks(mockMappedDataset);

    try (MockedStatic<ConfigUtils> mockedConfigUtils = mockStatic(ConfigUtils.class)) {
      mockedConfigUtils
          .when(() -> ConfigUtils.getSparkConfig(any(Config.class)))
          .thenReturn(new HashMap<>());

      // Act - Call protected method directly (same package, no reflection needed)
      StreamingQuery result = stream.getVectorApplicationLogsStreamQuery(mockKafkaDataset);

      // Assert
      assertNotNull(result, "Should return a StreamingQuery");

      // Verify that pushApplicationLogsToS3 was called (indirectly through writeStream chain)
      verify(mockMappedDataset, times(1)).writeStream();
      verify(mockWriter, times(1)).option("compression", Constants.WRITE_STREAM_GZIP_COMPRESSION);
      verify(mockWriter, times(1))
          .option("checkpointLocation", config.getString("s3.path.checkpoint.application"));
      verify(mockWriter, times(1)).start(config.getString("s3.path.logs.application"));
      verify(mockWriter, times(1)).queryName(Constants.APPLICATION_LOGS_TO_S3_QUERY_NAME);
      verify(mockWriter, times(1)).format(Constants.WRITE_STREAM_PARQUET_FORMAT);
      verify(mockWriter, times(1)).partitionBy(Constants.APPLICATION_LOG_S3_PARTITION_COLUMNS);
    }
  }

  @Test
  public void testGetVectorApplicationLogsStreamQuery_ProcessesKafkaDataset() {
    // Arrange
    Dataset<Row> mockKafkaDataset = mock(Dataset.class);
    Dataset<Row> mockMappedDataset = setupDatasetTransformationMocks(mockKafkaDataset);
    setupDataStreamWriterMocks(mockMappedDataset);

    try (MockedStatic<ConfigUtils> mockedConfigUtils = mockStatic(ConfigUtils.class)) {
      mockedConfigUtils
          .when(() -> ConfigUtils.getSparkConfig(any(Config.class)))
          .thenReturn(new HashMap<>());

      // Act
      StreamingQuery result = stream.getVectorApplicationLogsStreamQuery(mockKafkaDataset);

      // Assert
      assertNotNull(result);
      // Verify map() was called (transforms Kafka data to application logs format)
      verify(mockKafkaDataset, times(1)).map(any(MapFunction.class), any(ExpressionEncoder.class));
      // Verify withColumn was called 5 times (year, month, day, hour, minute)
      verify(mockMappedDataset, times(5)).withColumn(anyString(), any(Column.class));
    }
  }

  @Test
  public void testGetVectorApplicationLogsStreamQuery_WithCustomConfig() {
    // Arrange
    Config customConfig =
        createCustomTestConfig(120, "s3://custom/checkpoints", "s3://custom/logs");
    ApplicationLogsStreamToS3 customStream =
        new ApplicationLogsStreamToS3(customConfig, mockKafkaService);

    Dataset<Row> mockKafkaDataset = mock(Dataset.class);
    Dataset<Row> mockMappedDataset = setupDatasetTransformationMocks(mockKafkaDataset);
    DataStreamWriter<Row> mockWriter = setupDataStreamWriterMocks(mockMappedDataset);

    try (MockedStatic<ConfigUtils> mockedConfigUtils = mockStatic(ConfigUtils.class)) {
      mockedConfigUtils
          .when(() -> ConfigUtils.getSparkConfig(any(Config.class)))
          .thenReturn(new HashMap<>());

      // Act
      StreamingQuery result = customStream.getVectorApplicationLogsStreamQuery(mockKafkaDataset);

      // Assert - Verify custom config values are used
      assertNotNull(result);
      verify(mockWriter, times(1)).option("checkpointLocation", "s3://custom/checkpoints");
      verify(mockWriter, times(1)).start("s3://custom/logs");
    }
  }
}
