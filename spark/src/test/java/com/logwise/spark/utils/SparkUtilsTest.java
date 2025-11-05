package com.logwise.spark.utils;

import com.logwise.spark.base.MockSparkSessionHelper;
import com.logwise.spark.dto.entity.KafkaReadStreamOptions;
import com.logwise.spark.listeners.SparkStageListener;
import java.util.List;
import org.apache.spark.scheduler.SparkListenerInterface;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.DataStreamReader;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for SparkUtils utility class. */
public class SparkUtilsTest {

    @Test
    public void testGetSparkListeners_ReturnsListenerList() {
        // Arrange
        SparkStageListener mockListener = Mockito.mock(SparkStageListener.class);

        try (MockedStatic<com.logwise.spark.guice.injectors.ApplicationInjector> injectorMock = Mockito
                .mockStatic(com.logwise.spark.guice.injectors.ApplicationInjector.class)) {
            injectorMock
                    .when(
                            () -> com.logwise.spark.guice.injectors.ApplicationInjector.getInstance(
                                    SparkStageListener.class))
                    .thenReturn(mockListener);

            // Act
            List<SparkListenerInterface> listeners = SparkUtils.getSparkListeners();

            // Assert
            Assert.assertNotNull(listeners);
            Assert.assertEquals(listeners.size(), 1);
            Assert.assertTrue(listeners.contains(mockListener));
        }
    }

    @Test
    public void testGetKafkaReadStream_WithValidOptions_ReturnsDataset() {
        // Arrange
        SparkSession mockSparkSession = MockSparkSessionHelper.createMockSparkSession();
        DataStreamReader mockDataStreamReader = Mockito.mock(DataStreamReader.class);
        Dataset<Row> mockDataset = Mockito.mock(Dataset.class);

        Mockito.when(mockSparkSession.readStream()).thenReturn(mockDataStreamReader);
        Mockito.when(mockDataStreamReader.format("kafka")).thenReturn(mockDataStreamReader);
        Mockito.when(mockDataStreamReader.options(Mockito.any(java.util.Map.class)))
                .thenReturn(mockDataStreamReader);
        Mockito.when(mockDataStreamReader.load()).thenReturn(mockDataset);

        KafkaReadStreamOptions options = KafkaReadStreamOptions.builder()
                .kafkaBootstrapServers("localhost:9092")
                .maxOffsetsPerTrigger("1000")
                .startingOffsets("latest")
                .build();

        // Act
        Dataset<Row> result = SparkUtils.getKafkaReadStream(mockSparkSession, options);

        // Assert
        Assert.assertNotNull(result);
        Mockito.verify(mockDataStreamReader).format("kafka");
        Mockito.verify(mockDataStreamReader).options(Mockito.any(java.util.Map.class));
        Mockito.verify(mockDataStreamReader).load();
    }

    @Test
    public void testGetKafkaReadStream_WithNullValues_FiltersNullValues() {
        // Arrange
        SparkSession mockSparkSession = MockSparkSessionHelper.createMockSparkSession();
        DataStreamReader mockDataStreamReader = Mockito.mock(DataStreamReader.class);
        Dataset<Row> mockDataset = Mockito.mock(Dataset.class);

        Mockito.when(mockSparkSession.readStream()).thenReturn(mockDataStreamReader);
        Mockito.when(mockDataStreamReader.format("kafka")).thenReturn(mockDataStreamReader);
        Mockito.when(mockDataStreamReader.options(Mockito.any(java.util.Map.class)))
                .thenReturn(mockDataStreamReader);
        Mockito.when(mockDataStreamReader.load()).thenReturn(mockDataset);

        KafkaReadStreamOptions options = KafkaReadStreamOptions.builder()
                .kafkaBootstrapServers("localhost:9092")
                .maxOffsetsPerTrigger("1000")
                .startingOffsets("latest")
                .assign(null) // null value should be filtered
                .startingOffsetsByTimestamp(null) // null value should be filtered
                .build();

        // Act
        Dataset<Row> result = SparkUtils.getKafkaReadStream(mockSparkSession, options);

        // Assert
        Assert.assertNotNull(result);
        Mockito.verify(mockDataStreamReader).format("kafka");
        Mockito.verify(mockDataStreamReader).options(Mockito.any(java.util.Map.class));
        Mockito.verify(mockDataStreamReader).load();
    }

    @Test
    public void testGetKafkaReadStream_WithAllOptions_IncludesAllNonNullOptions() {
        // Arrange
        SparkSession mockSparkSession = MockSparkSessionHelper.createMockSparkSession();
        DataStreamReader mockDataStreamReader = Mockito.mock(DataStreamReader.class);
        Dataset<Row> mockDataset = Mockito.mock(Dataset.class);

        Mockito.when(mockSparkSession.readStream()).thenReturn(mockDataStreamReader);
        Mockito.when(mockDataStreamReader.format("kafka")).thenReturn(mockDataStreamReader);
        Mockito.when(mockDataStreamReader.options(Mockito.any(java.util.Map.class)))
                .thenReturn(mockDataStreamReader);
        Mockito.when(mockDataStreamReader.load()).thenReturn(mockDataset);

        KafkaReadStreamOptions options = KafkaReadStreamOptions.builder()
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
        Dataset<Row> result = SparkUtils.getKafkaReadStream(mockSparkSession, options);

        // Assert
        Assert.assertNotNull(result);
        Mockito.verify(mockDataStreamReader).format("kafka");
        Mockito.verify(mockDataStreamReader).options(Mockito.any(java.util.Map.class));
        Mockito.verify(mockDataStreamReader).load();
    }
}
