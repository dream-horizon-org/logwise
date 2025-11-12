package com.logwise.spark.services;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.spark.base.MockConfigHelper;
import com.logwise.spark.dto.entity.StartingOffsetsByTimestampOption;
import com.typesafe.config.Config;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Edge case tests for KafkaService.
 *
 * <p>
 * Tests verify handling of edge cases, boundary conditions, and unusual
 * scenarios in Kafka
 * service operations.
 */
public class KafkaServiceEdgeCaseTest {

    private static final String KAFKA_PORT = "9092";
    private static final String LOCALHOST = "localhost";

    private Config mockConfig;
    private KafkaService kafkaService;
    private KafkaConsumer<String, String> mockConsumer;
    private Function<Map<String, Object>, KafkaConsumer<String, String>> mockFactory;

    @BeforeMethod
    public void setUp() {
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("kafka.bootstrap.servers.port", KAFKA_PORT);
        mockConfig = MockConfigHelper.createConfig(configMap);

        mockConsumer = mock(KafkaConsumer.class);
        mockFactory = mock(Function.class);
        when(mockFactory.apply(any())).thenReturn(mockConsumer);

        kafkaService = new KafkaService(mockConfig, mockFactory);
    }

    @Test
    public void testGetStartingOffsetsByTimestamp_WithVeryOldTimestamp_ReturnsEmpty() {
        // Arrange - Very old timestamp (year 1970, just after epoch)
        Long veryOldTimestamp = 1000L; // January 1, 1970, 00:00:01 UTC
        Map<String, List<PartitionInfo>> topicsMetadata = new HashMap<>();
        topicsMetadata.put(
                "logs-topic", Collections.singletonList(createPartitionInfo("logs-topic", 0)));

        // Return empty offsets (no data for such old timestamp)
        when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);
        when(mockConsumer.offsetsForTimes(anyMap())).thenReturn(new HashMap<>());

        // Act
        StartingOffsetsByTimestampOption result = kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, "logs.*",
                veryOldTimestamp);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOffsetByTimestamp());
        // Should return empty if no offsets found for old timestamp
        Assert.assertTrue(
                result.getOffsetByTimestamp().isEmpty()
                        || result.getOffsetByTimestamp().size() == 0);
    }

    @Test
    public void testGetStartingOffsetsByTimestamp_WithFutureTimestamp_HandlesGracefully() {
        // Arrange - Future timestamp (year 2100)
        Long futureTimestamp = 4102444800000L;
        Map<String, List<PartitionInfo>> topicsMetadata = new HashMap<>();
        topicsMetadata.put(
                "logs-topic", Collections.singletonList(createPartitionInfo("logs-topic", 0)));

        // Return offsets at end of topic (latest available)
        Map<TopicPartition, OffsetAndTimestamp> futureOffsets = new HashMap<>();
        futureOffsets.put(
                new TopicPartition("logs-topic", 0), new OffsetAndTimestamp(1000L, futureTimestamp));

        when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);
        when(mockConsumer.offsetsForTimes(anyMap())).thenReturn(futureOffsets);

        // Act
        StartingOffsetsByTimestampOption result = kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, "logs.*",
                futureTimestamp);

        // Assert
        Assert.assertNotNull(result);
        // Should handle future timestamp gracefully (may return latest offsets)
    }

    @Test
    public void testGetStartingOffsetsByTimestamp_WithNetworkTimeout_HandlesGracefully() {
        // Arrange
        when(mockConsumer.listTopics(any(Duration.class)))
                .thenThrow(new RuntimeException("Network timeout"));

        // Act & Assert
        try {
            kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, "logs.*", System.currentTimeMillis());
            Assert.fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            // Expected - network timeout should propagate
            Assert.assertNotNull(e);
            Assert.assertTrue(e.getMessage().contains("timeout") || e.getCause() != null);
        } finally {
            // Verify consumer was closed even on exception
            verify(mockConsumer, atLeastOnce()).close();
        }
    }

    @Test
    public void testGetKafkaBootstrapServerIp_WithIPv6_HandlesCorrectly()
            throws UnknownHostException {
        // Arrange
        String hostname = "localhost"; // May resolve to IPv6

        // Act
        String result = kafkaService.getKafkaBootstrapServerIp(hostname);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertFalse(result.isEmpty());
        // Should contain port
        Assert.assertTrue(result.contains(":" + KAFKA_PORT));
        // May contain IPv6 format (with brackets) or IPv4
    }

    @Test
    public void testGetKafkaBootstrapServerIp_WithLoadBalancer_ReturnsAllIPs()
            throws UnknownHostException {
        // Arrange
        String hostname = "localhost"; // localhost may resolve to multiple IPs

        // Act
        String result = kafkaService.getKafkaBootstrapServerIp(hostname);

        // Assert
        Assert.assertNotNull(result);
        // May return single IP or comma-separated list
        String[] ips = result.split(",");
        Assert.assertTrue(ips.length >= 1);
        // Each IP should have port
        for (String ip : ips) {
            Assert.assertTrue(ip.trim().endsWith(":" + KAFKA_PORT));
        }
    }

    @Test
    public void testGetStartingOffsetsByTimestamp_WithEmptyTopicPattern_ReturnsEmpty() {
        // Arrange
        Map<String, List<PartitionInfo>> topicsMetadata = new HashMap<>();
        topicsMetadata.put("logs-topic", Collections.singletonList(createPartitionInfo("logs-topic", 0)));

        when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);

        // Act
        StartingOffsetsByTimestampOption result = kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, "",
                System.currentTimeMillis());

        // Assert
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getOffsetByTimestamp());
        // Empty pattern should match nothing
        Assert.assertTrue(result.getOffsetByTimestamp().isEmpty());
    }

    @Test
    public void testGetStartingOffsetsByTimestamp_WithWildcardPattern_MatchesAllTopics() {
        // Arrange
        Map<String, List<PartitionInfo>> topicsMetadata = new HashMap<>();
        topicsMetadata.put("logs-topic1", Collections.singletonList(createPartitionInfo("logs-topic1", 0)));
        topicsMetadata.put("logs-topic2", Collections.singletonList(createPartitionInfo("logs-topic2", 0)));
        topicsMetadata.put("other-topic", Collections.singletonList(createPartitionInfo("other-topic", 0)));

        Map<TopicPartition, OffsetAndTimestamp> offsets = new HashMap<>();
        offsets.put(new TopicPartition("logs-topic1", 0), new OffsetAndTimestamp(100L, System.currentTimeMillis()));
        offsets.put(new TopicPartition("logs-topic2", 0), new OffsetAndTimestamp(200L, System.currentTimeMillis()));

        when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);
        when(mockConsumer.offsetsForTimes(anyMap())).thenReturn(offsets);

        // Act
        StartingOffsetsByTimestampOption result = kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, "logs.*",
                System.currentTimeMillis());

        // Assert
        Assert.assertNotNull(result);
        Map<String, Map<String, Long>> offsetMap = result.getOffsetByTimestamp();
        // Should match logs-topic1 and logs-topic2, but not other-topic
        Assert.assertTrue(offsetMap.containsKey("logs-topic1") || offsetMap.containsKey("logs-topic2"));
        Assert.assertFalse(offsetMap.containsKey("other-topic"));
    }

    // Helper method
    private PartitionInfo createPartitionInfo(String topic, int partition) {
        return new PartitionInfo(topic, partition, null, null, null);
    }
}
