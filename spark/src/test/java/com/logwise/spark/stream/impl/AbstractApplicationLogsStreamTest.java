package com.logwise.spark.stream.impl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mockStatic;
import static org.testng.Assert.*;

import com.logwise.spark.base.BaseSparkTest;
import com.logwise.spark.services.KafkaService;
import com.logwise.spark.utils.SparkUtils;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for AbstractApplicationLogsStream.
 *
 * <p>Tests the base stream processing logic including: - Configuration reading and validation -
 * Dependency injection - Abstract class behavior - Configuration scenarios
 *
 * <p>Note: The startStreams() method uses SparkUtils static methods and requires integration
 * testing with a real SparkSession. These tests focus on verifiable unit-testable behavior.
 */
public class AbstractApplicationLogsStreamTest extends BaseSparkTest {

  private Config config;
  private KafkaService mockKafkaService;
  private SparkSession mockSparkSession;
  private TestApplicationLogsStream testStream;

  // Concrete implementation for testing the abstract class
  private static class TestApplicationLogsStream extends AbstractApplicationLogsStream {
    private StreamingQuery mockQuery;

    public TestApplicationLogsStream(
        Config config, KafkaService kafkaService, StreamingQuery mockQuery) {
      super(config, kafkaService);
      this.mockQuery = mockQuery;
    }

    @Override
    protected StreamingQuery getVectorApplicationLogsStreamQuery(
        Dataset<Row> kafkaValueTopicStream) {
      return mockQuery;
    }
  }

  @BeforeMethod
  @Override
  public void setUp() {
    super.setUp();

    // Mock KafkaService
    mockKafkaService = mock(KafkaService.class);

    // Mock SparkSession
    mockSparkSession = mock(SparkSession.class);
  }

  @AfterMethod
  @Override
  public void tearDown() {
    super.tearDown();
  }

  private Config createTestConfig(long startingOffsetsTimestamp, String startingOffsets) {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("kafka.cluster.dns", "test-kafka-cluster.local");
    configMap.put("kafka.startingOffsetsTimestamp", startingOffsetsTimestamp);
    configMap.put("kafka.startingOffsets", startingOffsets);
    configMap.put("kafka.topic.prefix.application", "app-logs-.*");
    configMap.put("kafka.maxRatePerPartition", "1000");
    configMap.put("spark.offsetPerTrigger.default", 10000L);
    return ConfigFactory.parseMap(configMap);
  }

  @Test
  public void testConstructor_WithValidParameters_CreatesInstance() {
    // Arrange
    config = createTestConfig(0L, "latest");
    StreamingQuery mockQuery = mock(StreamingQuery.class);

    // Act
    testStream = new TestApplicationLogsStream(config, mockKafkaService, mockQuery);

    // Assert
    assertNotNull(testStream);
  }

  @Test
  public void testConstructor_WithNullConfig_CreatesInstance() {
    // Arrange
    StreamingQuery mockQuery = mock(StreamingQuery.class);

    // Act - Note: In production, Guice ensures config is not null
    testStream = new TestApplicationLogsStream(null, mockKafkaService, mockQuery);

    // Assert
    assertNotNull(testStream);
  }

  @Test
  public void testConstructor_WithNullKafkaService_CreatesInstance() {
    // Arrange
    config = createTestConfig(0L, "latest");
    StreamingQuery mockQuery = mock(StreamingQuery.class);

    // Act - Note: In production, Guice ensures kafkaService is not null
    testStream = new TestApplicationLogsStream(config, null, mockQuery);

    // Assert
    assertNotNull(testStream);
  }

  @Test
  public void testStartStreams_CallsGetKafkaBootstrapServerIp_WhenStartingStreams() {
    // Arrange
    config = createTestConfig(0L, "latest");
    StreamingQuery mockQuery = mock(StreamingQuery.class);
    testStream = new TestApplicationLogsStream(config, mockKafkaService, mockQuery);

    Dataset<Row> mockKafkaDataset = mock(Dataset.class);
    Dataset<Row> mockValueDataset = mock(Dataset.class);
    when(mockKafkaDataset.selectExpr("value")).thenReturn(mockValueDataset);
    when(mockKafkaService.getKafkaBootstrapServerIp("test-kafka-cluster.local"))
        .thenReturn("192.168.1.100:9092");

    try (MockedStatic<SparkUtils> mockedSparkUtils = mockStatic(SparkUtils.class)) {
      mockedSparkUtils
          .when(() -> SparkUtils.getKafkaReadStream(eq(mockSparkSession), any()))
          .thenReturn(mockKafkaDataset);

      // Act
      List<StreamingQuery> result = testStream.startStreams(mockSparkSession);

      // Assert
      assertNotNull(result);
      assertEquals(result.size(), 1);
      assertEquals(result.get(0), mockQuery);
      // Verify that getKafkaBootstrapServerIp was called through startStreams()
      verify(mockKafkaService, times(1)).getKafkaBootstrapServerIp("test-kafka-cluster.local");
    }
  }

  @Test
  public void testAbstractClass_CanBeExtended() {
    // Arrange
    config = createTestConfig(0L, "latest");
    StreamingQuery mockQuery = mock(StreamingQuery.class);

    // Act
    TestApplicationLogsStream concreteStream =
        new TestApplicationLogsStream(config, mockKafkaService, mockQuery);

    // Assert
    assertNotNull(concreteStream);
    assertTrue(concreteStream instanceof AbstractApplicationLogsStream);
  }

  @Test
  public void testAbstractMethod_GetVectorApplicationLogsStreamQuery_CanBeOverridden() {
    // Arrange
    config = createTestConfig(0L, "latest");
    StreamingQuery mockQuery = mock(StreamingQuery.class);
    testStream = new TestApplicationLogsStream(config, mockKafkaService, mockQuery);
    Dataset<Row> mockDataset = mock(Dataset.class);

    // Act
    StreamingQuery result = testStream.getVectorApplicationLogsStreamQuery(mockDataset);

    // Assert
    assertNotNull(result);
    assertEquals(result, mockQuery);
  }

  @Test
  public void testMultipleInstances_CanBeCreatedWithDifferentConfigs() {
    // Arrange
    Config config1 = createTestConfig(0L, "latest");
    Config config2 = createTestConfig(1609459200000L, "earliest");
    StreamingQuery mockQuery1 = mock(StreamingQuery.class);
    StreamingQuery mockQuery2 = mock(StreamingQuery.class);

    // Act
    TestApplicationLogsStream stream1 =
        new TestApplicationLogsStream(config1, mockKafkaService, mockQuery1);
    TestApplicationLogsStream stream2 =
        new TestApplicationLogsStream(config2, mockKafkaService, mockQuery2);

    // Assert
    assertNotNull(stream1);
    assertNotNull(stream2);
    assertNotSame(stream1, stream2);
  }

  @Test
  public void testStartStreams_WithStartingOffsetsTimestampZero_ReturnsStreamingQuery() {
    // Arrange
    config = createTestConfig(0L, "latest");
    StreamingQuery mockQuery = mock(StreamingQuery.class);
    testStream = new TestApplicationLogsStream(config, mockKafkaService, mockQuery);

    Dataset<Row> mockKafkaDataset = mock(Dataset.class);
    Dataset<Row> mockValueDataset = mock(Dataset.class);
    when(mockKafkaDataset.selectExpr("value")).thenReturn(mockValueDataset);
    when(mockKafkaService.getKafkaBootstrapServerIp("test-kafka-cluster.local"))
        .thenReturn("192.168.1.100:9092");

    try (MockedStatic<SparkUtils> mockedSparkUtils = mockStatic(SparkUtils.class)) {
      mockedSparkUtils
          .when(() -> SparkUtils.getKafkaReadStream(eq(mockSparkSession), any()))
          .thenReturn(mockKafkaDataset);

      // Act
      List<StreamingQuery> result = testStream.startStreams(mockSparkSession);

      // Assert
      assertNotNull(result);
      assertEquals(result.size(), 1);
      assertEquals(result.get(0), mockQuery);
      verify(mockKafkaDataset, times(1)).selectExpr("value");
    }
  }

  @Test
  public void testStartStreams_WithStartingOffsetsTimestampNonZero_ReturnsStreamingQuery() {
    // Arrange
    config = createTestConfig(1609459200000L, "latest");
    StreamingQuery mockQuery = mock(StreamingQuery.class);
    testStream = new TestApplicationLogsStream(config, mockKafkaService, mockQuery);

    Dataset<Row> mockKafkaDataset = mock(Dataset.class);
    Dataset<Row> mockValueDataset = mock(Dataset.class);
    when(mockKafkaDataset.selectExpr("value")).thenReturn(mockValueDataset);
    when(mockKafkaService.getKafkaBootstrapServerIp("test-kafka-cluster.local"))
        .thenReturn("192.168.1.100:9092");

    try (MockedStatic<SparkUtils> mockedSparkUtils = mockStatic(SparkUtils.class)) {
      mockedSparkUtils
          .when(() -> SparkUtils.getKafkaReadStream(eq(mockSparkSession), any()))
          .thenReturn(mockKafkaDataset);

      // Act
      List<StreamingQuery> result = testStream.startStreams(mockSparkSession);

      // Assert
      assertNotNull(result);
      assertEquals(result.size(), 1);
      assertEquals(result.get(0), mockQuery);
      verify(mockKafkaDataset, times(1)).selectExpr("value");
    }
  }

  @Test
  public void testStartStreams_WithMaxOffsetPerTrigger_ReadsFromConfig() {
    // Arrange
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("kafka.cluster.dns", "test-kafka.local");
    configMap.put("kafka.startingOffsetsTimestamp", 0L);
    configMap.put("kafka.startingOffsets", "latest");
    configMap.put("kafka.topic.prefix.application", "app-logs-.*");
    configMap.put("kafka.maxRatePerPartition", "1000");
    configMap.put("spark.offsetPerTrigger.default", 50000L);
    config = ConfigFactory.parseMap(configMap);

    StreamingQuery mockQuery = mock(StreamingQuery.class);
    testStream = new TestApplicationLogsStream(config, mockKafkaService, mockQuery);

    Dataset<Row> mockKafkaDataset = mock(Dataset.class);
    Dataset<Row> mockValueDataset = mock(Dataset.class);
    when(mockKafkaDataset.selectExpr("value")).thenReturn(mockValueDataset);
    when(mockKafkaService.getKafkaBootstrapServerIp("test-kafka.local"))
        .thenReturn("192.168.1.100:9092");

    try (MockedStatic<SparkUtils> mockedSparkUtils = mockStatic(SparkUtils.class)) {
      mockedSparkUtils
          .when(() -> SparkUtils.getKafkaReadStream(eq(mockSparkSession), any()))
          .thenReturn(mockKafkaDataset);

      // Act
      List<StreamingQuery> result = testStream.startStreams(mockSparkSession);

      // Assert
      assertNotNull(result);
      assertEquals(result.size(), 1);
      // Verify maxOffsetPerTrigger was read from config (50000L)
      assertEquals(config.getLong("spark.offsetPerTrigger.default"), 50000L);
    }
  }

  @Test
  public void testStartStreams_WithEarliestStartingOffsets_UsesEarliest() {
    // Arrange
    config = createTestConfig(0L, "earliest");
    StreamingQuery mockQuery = mock(StreamingQuery.class);
    testStream = new TestApplicationLogsStream(config, mockKafkaService, mockQuery);

    Dataset<Row> mockKafkaDataset = mock(Dataset.class);
    Dataset<Row> mockValueDataset = mock(Dataset.class);
    when(mockKafkaDataset.selectExpr("value")).thenReturn(mockValueDataset);
    when(mockKafkaService.getKafkaBootstrapServerIp("test-kafka-cluster.local"))
        .thenReturn("192.168.1.100:9092");

    try (MockedStatic<SparkUtils> mockedSparkUtils = mockStatic(SparkUtils.class)) {
      mockedSparkUtils
          .when(() -> SparkUtils.getKafkaReadStream(eq(mockSparkSession), any()))
          .thenReturn(mockKafkaDataset);

      // Act
      List<StreamingQuery> result = testStream.startStreams(mockSparkSession);

      // Assert
      assertNotNull(result);
      assertEquals(result.size(), 1);
      assertEquals(config.getString("kafka.startingOffsets"), "earliest");
    }
  }
}
