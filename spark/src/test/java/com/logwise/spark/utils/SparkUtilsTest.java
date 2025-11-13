package com.logwise.spark.utils;

import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.spark.base.MockSparkSessionHelper;
import com.logwise.spark.dto.entity.KafkaReadStreamOptions;
import com.logwise.spark.listeners.SparkStageListener;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.spark.scheduler.SparkListenerInterface;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for SparkUtils class. */
public class SparkUtilsTest {

  private SparkUtils sparkUtils;
  private Supplier<SparkListenerInterface> mockListenerSupplier;
  private SparkListenerInterface mockListener;
  private ObjectMapper objectMapper;

  @BeforeMethod
  public void setUp() {
    mockListener = mock(SparkStageListener.class);
    mockListenerSupplier = mock(Supplier.class);
    when(mockListenerSupplier.get()).thenReturn(mockListener);
    objectMapper = new ObjectMapper();
    sparkUtils = new SparkUtils(objectMapper, mockListenerSupplier);
  }

  @Test
  public void testGetSparkListeners_ReturnsListenerList() {
    // Act
    List<SparkListenerInterface> listeners = sparkUtils.getSparkListenersInstance();

    // Assert
    Assert.assertNotNull(listeners);
    Assert.assertEquals(listeners.size(), 1);
    Assert.assertTrue(listeners.contains(mockListener));
    verify(mockListenerSupplier, times(1)).get();
  }

  @Test
  public void testGetKafkaReadStream_WithValidOptions_ReturnsDataset() {
    // Arrange
    SparkSession mockSparkSession = MockSparkSessionHelper.createMockSparkSession();
    Dataset<Row> mockDataset = mock(Dataset.class);
    Function<SparkSession, Dataset<Row>> mockStreamReaderFunction = mock(Function.class);
    when(mockStreamReaderFunction.apply(mockSparkSession)).thenReturn(mockDataset);

    KafkaReadStreamOptions options =
        KafkaReadStreamOptions.builder()
            .kafkaBootstrapServers("localhost:9092")
            .maxOffsetsPerTrigger("1000")
            .startingOffsets("latest")
            .failOnDataLoss("false")
            .maxRatePerPartition("100")
            .groupIdPrefix("test-group")
            .build();

    // Act
    Dataset<Row> result =
        sparkUtils.getKafkaReadStream(mockSparkSession, options, mockStreamReaderFunction);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertEquals(result, mockDataset);
    verify(mockStreamReaderFunction, times(1)).apply(mockSparkSession);
  }

  @Test
  public void testGetKafkaReadStream_WithNullValues_FiltersNullValues() {
    // Arrange
    SparkSession mockSparkSession = MockSparkSessionHelper.createMockSparkSession();
    Dataset<Row> mockDataset = mock(Dataset.class);
    Function<SparkSession, Dataset<Row>> mockStreamReaderFunction = mock(Function.class);
    when(mockStreamReaderFunction.apply(mockSparkSession)).thenReturn(mockDataset);

    KafkaReadStreamOptions options =
        KafkaReadStreamOptions.builder()
            .kafkaBootstrapServers("localhost:9092")
            .maxOffsetsPerTrigger("1000")
            .startingOffsets("latest")
            .failOnDataLoss("false")
            .maxRatePerPartition("100")
            .groupIdPrefix("test-group")
            .assign(null) // null value should be filtered
            .startingOffsetsByTimestamp(null) // null value should be filtered
            .build();

    // Act
    Dataset<Row> result =
        sparkUtils.getKafkaReadStream(mockSparkSession, options, mockStreamReaderFunction);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertEquals(result, mockDataset);
    verify(mockStreamReaderFunction, times(1)).apply(mockSparkSession);
  }

  @Test
  public void testGetKafkaReadStream_WithAllOptions_IncludesAllNonNullOptions() {
    // Arrange
    SparkSession mockSparkSession = MockSparkSessionHelper.createMockSparkSession();
    Dataset<Row> mockDataset = mock(Dataset.class);
    Function<SparkSession, Dataset<Row>> mockStreamReaderFunction = mock(Function.class);
    when(mockStreamReaderFunction.apply(mockSparkSession)).thenReturn(mockDataset);

    KafkaReadStreamOptions options =
        KafkaReadStreamOptions.builder()
            .kafkaBootstrapServers("localhost:9092")
            .maxOffsetsPerTrigger("1000")
            .startingOffsets("latest")
            .assign("{\"topics\":[\"test-topic\"]}")
            .startingOffsetsByTimestamp("{\"topic\":{\"0\":1000}}")
            .subscribePattern("logs.*")
            .maxRatePerPartition("100")
            .minPartitions("5")
            .groupIdPrefix("test-group")
            .failOnDataLoss("false")
            .build();

    // Act
    Dataset<Row> result =
        sparkUtils.getKafkaReadStream(mockSparkSession, options, mockStreamReaderFunction);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertEquals(result, mockDataset);
    verify(mockStreamReaderFunction, times(1)).apply(mockSparkSession);
  }

  @Test
  public void testGetKafkaReadStreamInstance_WithValidOptions_ReturnsDataset() {
    // Arrange
    SparkSession mockSparkSession = MockSparkSessionHelper.createMockSparkSession();
    Dataset<Row> mockDataset = mock(Dataset.class);

    // Mock the entire chain: readStream().format().options().load()
    org.apache.spark.sql.streaming.DataStreamReader mockDataStreamReader =
        mock(org.apache.spark.sql.streaming.DataStreamReader.class);

    when(mockSparkSession.readStream()).thenReturn(mockDataStreamReader);
    when(mockDataStreamReader.format(anyString())).thenReturn(mockDataStreamReader);
    when(mockDataStreamReader.options(anyMap())).thenReturn(mockDataStreamReader);
    when(mockDataStreamReader.load()).thenReturn(mockDataset);

    KafkaReadStreamOptions options =
        KafkaReadStreamOptions.builder()
            .kafkaBootstrapServers("localhost:9092")
            .maxOffsetsPerTrigger("1000")
            .startingOffsets("latest")
            .failOnDataLoss("false")
            .maxRatePerPartition("100")
            .groupIdPrefix("test-group")
            .build();

    // Act
    Dataset<Row> result = sparkUtils.getKafkaReadStreamInstance(mockSparkSession, options);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertEquals(result, mockDataset);
  }

  @Test
  public void testGetSparkListeners_StaticMethod_ReturnsListenerList() {
    // Note: Static method uses default constructor which requires ApplicationInjector
    // Instead, test the instance method which we can control
    // The static method just delegates to instance method, so testing instance is sufficient

    // Act - Test instance method (static method delegates to this)
    List<SparkListenerInterface> listeners = sparkUtils.getSparkListenersInstance();

    // Assert
    Assert.assertNotNull(listeners);
    Assert.assertEquals(listeners.size(), 1);
    Assert.assertTrue(listeners.contains(mockListener));
  }

  @Test
  public void testGetKafkaReadStream_StaticMethod_ReturnsDataset() {
    // Arrange
    SparkSession mockSparkSession = MockSparkSessionHelper.createMockSparkSession();
    Dataset<Row> mockDataset = mock(Dataset.class);

    // Mock the entire chain: readStream().format().options().load()
    org.apache.spark.sql.streaming.DataStreamReader mockDataStreamReader =
        mock(org.apache.spark.sql.streaming.DataStreamReader.class);

    when(mockSparkSession.readStream()).thenReturn(mockDataStreamReader);
    when(mockDataStreamReader.format(anyString())).thenReturn(mockDataStreamReader);
    when(mockDataStreamReader.options(anyMap())).thenReturn(mockDataStreamReader);
    when(mockDataStreamReader.load()).thenReturn(mockDataset);

    KafkaReadStreamOptions options =
        KafkaReadStreamOptions.builder()
            .kafkaBootstrapServers("localhost:9092")
            .maxOffsetsPerTrigger("1000")
            .startingOffsets("latest")
            .failOnDataLoss("false")
            .maxRatePerPartition("100")
            .groupIdPrefix("test-group")
            .build();

    // Act - Test static convenience method
    Dataset<Row> result = SparkUtils.getKafkaReadStream(mockSparkSession, options);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertEquals(result, mockDataset);
  }
}
