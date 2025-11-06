package com.logwise.spark.services;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.spark.base.MockConfigHelper;
import com.logwise.spark.dto.entity.StartingOffsetsByTimestampOption;
import com.typesafe.config.Config;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
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
 * Unit tests for KafkaService.
 *
 * <p>Tests use a factory pattern with dependency injection for KafkaConsumer creation, allowing for
 * proper mocking with mockito-core.
 */
public class KafkaServiceTest {

  // Test Constants
  private static final String KAFKA_PORT = "9092";
  private static final String LOCALHOST = "localhost";
  private static final String LOGS_TOPIC_PATTERN = "logs.*";
  private static final String NONEXISTENT_PATTERN = "nonexistent.*";
  private static final String INVALID_HOSTNAME = "invalid.hostname.that.does.not.exist.12345";

  // Test Topics
  private static final String LOGS_TOPIC_1 = "logs-topic1";
  private static final String LOGS_TOPIC_2 = "logs-topic2";
  private static final String OTHER_TOPIC = "other-topic";

  // Test Timestamp: January 1, 2021, 00:00:00 UTC
  private static final Long TEST_TIMESTAMP = Instant.parse("2021-01-01T00:00:00Z").toEpochMilli();

  private Config mockConfig;
  private KafkaService kafkaService;
  private KafkaConsumer<String, String> mockConsumer;
  private Function<Map<String, Object>, KafkaConsumer<String, String>> mockFactory;

  @BeforeMethod
  public void setUp() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("kafka.bootstrap.servers.port", KAFKA_PORT);
    mockConfig = MockConfigHelper.createConfig(configMap);

    // Create mock consumer
    mockConsumer = mock(KafkaConsumer.class);

    // Create mock factory that returns the mock consumer
    mockFactory = mock(Function.class);
    when(mockFactory.apply(any())).thenReturn(mockConsumer);

    // Create KafkaService with mocked factory
    kafkaService = new KafkaService(mockConfig, mockFactory);
  }

  @Test
  public void testGetKafkaBootstrapServerIp_WithValidHostname_ReturnsFormattedIp() {
    // Act
    String result = kafkaService.getKafkaBootstrapServerIp(LOCALHOST);

    // Assert
    Assert.assertNotNull(result, "Result should not be null");
    Assert.assertTrue(
        result.contains(":" + KAFKA_PORT), "Result should contain port: " + KAFKA_PORT);
  }

  @Test
  public void testGetKafkaBootstrapServerIp_WithMultipleIps_ReturnsCommaSeparatedList() {
    // Act
    String result = kafkaService.getKafkaBootstrapServerIp(LOCALHOST);

    // Assert
    Assert.assertNotNull(result, "Result should not be null");
    String[] parts = result.split(",");
    Assert.assertTrue(parts.length > 0, "Should return at least one IP address");

    for (String part : parts) {
      Assert.assertFalse(part.trim().isEmpty(), "Each part should not be empty");
      Assert.assertTrue(
          part.endsWith(":" + KAFKA_PORT), "Each part should end with port: " + KAFKA_PORT);
      // Verify format is valid (handles both IPv4 and IPv6)
      String ipPart = part.substring(0, part.lastIndexOf(":"));
      Assert.assertFalse(ipPart.isEmpty(), "IP address part should not be empty");
    }
  }

  @Test(expectedExceptions = UnknownHostException.class)
  public void testGetKafkaBootstrapServerIp_WithInvalidHostname_ThrowsUnknownHostException()
      throws UnknownHostException {
    // Act - should throw UnknownHostException
    kafkaService.getKafkaBootstrapServerIp(INVALID_HOSTNAME);
  }

  @Test
  public void testGetStartingOffsetsByTimestamp_WithValidInput_ReturnsCorrectOffsets() {
    // Arrange
    Map<String, List<PartitionInfo>> topicsMetadata = createTopicsMetadata();
    Map<TopicPartition, OffsetAndTimestamp> expectedOffsets = createExpectedOffsets(TEST_TIMESTAMP);

    when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);
    when(mockConsumer.offsetsForTimes(anyMap())).thenReturn(expectedOffsets);

    // Act
    StartingOffsetsByTimestampOption result =
        kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, LOGS_TOPIC_PATTERN, TEST_TIMESTAMP);

    // Assert
    Assert.assertNotNull(result, "Result should not be null");
    Assert.assertNotNull(result.getOffsetByTimestamp(), "Offset map should not be null");
    Assert.assertTrue(
        result.getOffsetByTimestamp().size() > 0, "Should return offsets for matching topics");

    // Verify topics matching the pattern are included
    Map<String, Map<String, Long>> offsetMap = result.getOffsetByTimestamp();
    Assert.assertTrue(
        offsetMap.containsKey(LOGS_TOPIC_1) || offsetMap.containsKey(LOGS_TOPIC_2),
        "Should contain at least one matching topic");
    Assert.assertFalse(
        offsetMap.containsKey(OTHER_TOPIC), "Should not contain non-matching topics");

    // Verify mock interactions
    verify(mockFactory, times(1)).apply(any());
    verify(mockConsumer, times(1)).listTopics(any(Duration.class));
    verify(mockConsumer, times(1)).offsetsForTimes(anyMap());
    verify(mockConsumer, times(1)).close();
  }

  @Test
  public void testGetStartingOffsetsByTimestamp_WithNoMatchingTopics_ReturnsEmptyOption() {
    // Arrange
    Map<String, List<PartitionInfo>> topicsMetadata = createTopicsMetadata();

    when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);
    // offsetsForTimes is still called even with no matching topics (with empty map)
    when(mockConsumer.offsetsForTimes(anyMap())).thenReturn(new HashMap<>());

    // Act
    StartingOffsetsByTimestampOption result =
        kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, NONEXISTENT_PATTERN, TEST_TIMESTAMP);

    // Assert
    Assert.assertNotNull(result, "Result should not be null");
    Assert.assertNotNull(result.getOffsetByTimestamp(), "Offset map should not be null");
    Assert.assertEquals(
        result.getOffsetByTimestamp().size(),
        0,
        "Should return empty map when no topics match the pattern");

    // Verify offsetsForTimes was called with an empty map
    verify(mockConsumer, times(1)).listTopics(any(Duration.class));
    verify(mockConsumer, times(1)).offsetsForTimes(argThat(map -> map.isEmpty()));
    verify(mockConsumer, times(1)).close();
  }

  @Test
  public void testGetStartingOffsetsByTimestamp_WithNullOffsets_FiltersOutNullValues() {
    // Arrange
    Map<String, List<PartitionInfo>> topicsMetadata = new HashMap<>();
    topicsMetadata.put(
        LOGS_TOPIC_1, Collections.singletonList(createPartitionInfo(LOGS_TOPIC_1, 0)));

    Map<TopicPartition, OffsetAndTimestamp> offsetsWithNull = new HashMap<>();
    offsetsWithNull.put(new TopicPartition(LOGS_TOPIC_1, 0), null); // No data for timestamp

    when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);
    when(mockConsumer.offsetsForTimes(anyMap())).thenReturn(offsetsWithNull);

    // Act
    StartingOffsetsByTimestampOption result =
        kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, LOGS_TOPIC_PATTERN, TEST_TIMESTAMP);

    // Assert
    Assert.assertNotNull(result, "Result should not be null");
    Assert.assertNotNull(result.getOffsetByTimestamp(), "Offset map should not be null");
    Assert.assertEquals(
        result.getOffsetByTimestamp().size(),
        0,
        "Should return empty map when all offsets are null");

    // Verify mock interactions
    verify(mockConsumer, times(1))
        .offsetsForTimes(argThat(map -> map.containsKey(new TopicPartition(LOGS_TOPIC_1, 0))));
    verify(mockConsumer, times(1)).close();
  }

  @Test
  public void testGetStartingOffsetsByTimestamp_WithMixedNullOffsets_ReturnsOnlyValidOffsets() {
    // Arrange
    Map<String, List<PartitionInfo>> topicsMetadata = new HashMap<>();
    topicsMetadata.put(
        LOGS_TOPIC_1,
        Arrays.asList(createPartitionInfo(LOGS_TOPIC_1, 0), createPartitionInfo(LOGS_TOPIC_1, 1)));

    Map<TopicPartition, OffsetAndTimestamp> mixedOffsets = new HashMap<>();
    mixedOffsets.put(
        new TopicPartition(LOGS_TOPIC_1, 0), new OffsetAndTimestamp(100L, TEST_TIMESTAMP)); // Valid
    mixedOffsets.put(new TopicPartition(LOGS_TOPIC_1, 1), null); // Null

    when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);
    when(mockConsumer.offsetsForTimes(anyMap())).thenReturn(mixedOffsets);

    // Act
    StartingOffsetsByTimestampOption result =
        kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, LOGS_TOPIC_PATTERN, TEST_TIMESTAMP);

    // Assert
    Assert.assertNotNull(result, "Result should not be null");
    Assert.assertEquals(
        result.getOffsetByTimestamp().size(), 1, "Should return only partition with valid offset");

    Map<String, Map<String, Long>> offsetMap = result.getOffsetByTimestamp();
    Assert.assertTrue(
        offsetMap.containsKey(LOGS_TOPIC_1), "Should contain topic with valid offset");
    Assert.assertEquals(
        offsetMap.get(LOGS_TOPIC_1).size(),
        1,
        "Should contain only one partition (the one with valid offset)");
  }

  @Test
  public void testGetStartingOffsetsByTimestamp_WithMultiplePartitions_ReturnsAllPartitions() {
    // Arrange
    Map<String, List<PartitionInfo>> topicsMetadata = new HashMap<>();
    topicsMetadata.put(
        LOGS_TOPIC_1,
        Arrays.asList(
            createPartitionInfo(LOGS_TOPIC_1, 0),
            createPartitionInfo(LOGS_TOPIC_1, 1),
            createPartitionInfo(LOGS_TOPIC_1, 2)));

    Map<TopicPartition, OffsetAndTimestamp> allValidOffsets = new HashMap<>();
    allValidOffsets.put(
        new TopicPartition(LOGS_TOPIC_1, 0), new OffsetAndTimestamp(100L, TEST_TIMESTAMP));
    allValidOffsets.put(
        new TopicPartition(LOGS_TOPIC_1, 1), new OffsetAndTimestamp(200L, TEST_TIMESTAMP));
    allValidOffsets.put(
        new TopicPartition(LOGS_TOPIC_1, 2), new OffsetAndTimestamp(300L, TEST_TIMESTAMP));

    when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);
    when(mockConsumer.offsetsForTimes(anyMap())).thenReturn(allValidOffsets);

    // Act
    StartingOffsetsByTimestampOption result =
        kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, LOGS_TOPIC_PATTERN, TEST_TIMESTAMP);

    // Assert
    Assert.assertNotNull(result, "Result should not be null");
    Map<String, Map<String, Long>> offsetMap = result.getOffsetByTimestamp();
    Assert.assertTrue(offsetMap.containsKey(LOGS_TOPIC_1), "Should contain the topic");
    Assert.assertEquals(
        offsetMap.get(LOGS_TOPIC_1).size(), 3, "Should contain all three partitions");
  }

  // ==================== Negative Test Cases ====================

  @Test(expectedExceptions = RuntimeException.class)
  public void
      testGetStartingOffsetsByTimestamp_WhenListTopicsThrowsException_PropagatesException() {
    // Arrange
    when(mockConsumer.listTopics(any(Duration.class)))
        .thenThrow(new RuntimeException("Kafka connection failed"));

    // Act - should throw RuntimeException
    kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, LOGS_TOPIC_PATTERN, TEST_TIMESTAMP);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void
      testGetStartingOffsetsByTimestamp_WhenOffsetsForTimesThrowsException_PropagatesException() {
    // Arrange
    Map<String, List<PartitionInfo>> topicsMetadata = createTopicsMetadata();

    when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);
    when(mockConsumer.offsetsForTimes(anyMap()))
        .thenThrow(new RuntimeException("Failed to fetch offsets"));

    // Act - should throw RuntimeException
    kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, LOGS_TOPIC_PATTERN, TEST_TIMESTAMP);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testGetStartingOffsetsByTimestamp_WithNullTimestamp_ThrowsNullPointerException() {
    // Arrange
    Map<String, List<PartitionInfo>> topicsMetadata = createTopicsMetadata();

    when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);
    when(mockConsumer.offsetsForTimes(anyMap())).thenReturn(new HashMap<>());

    // Act - passing null timestamp causes NPE during unboxing (Long -> long)
    kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, LOGS_TOPIC_PATTERN, null);
  }

  @Test
  public void testGetStartingOffsetsByTimestamp_WithEmptyTopicPattern_ReturnsEmptyOption() {
    // Arrange
    Map<String, List<PartitionInfo>> topicsMetadata = createTopicsMetadata();

    when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);

    // Act - empty pattern won't match any topics
    StartingOffsetsByTimestampOption result =
        kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, "", TEST_TIMESTAMP);

    // Assert
    Assert.assertNotNull(result, "Result should not be null");
    Assert.assertEquals(
        result.getOffsetByTimestamp().size(), 0, "Empty pattern should return empty offsets");
  }

  @Test
  public void testGetStartingOffsetsByTimestamp_WithInvalidRegexPattern_HandlesGracefully() {
    // Arrange
    Map<String, List<PartitionInfo>> topicsMetadata = createTopicsMetadata();
    String invalidPattern = "[invalid(regex"; // Invalid regex pattern

    when(mockConsumer.listTopics(any(Duration.class))).thenReturn(topicsMetadata);

    try {
      // Act - invalid regex should be handled
      StartingOffsetsByTimestampOption result =
          kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, invalidPattern, TEST_TIMESTAMP);

      // Assert - if no exception, should return empty result
      Assert.assertNotNull(result, "Result should not be null");
    } catch (Exception e) {
      // Assert - alternatively, may throw PatternSyntaxException which is acceptable
      Assert.assertTrue(
          e instanceof java.util.regex.PatternSyntaxException,
          "Should throw PatternSyntaxException for invalid regex");
    }
  }

  @Test
  public void testGetStartingOffsetsByTimestamp_EnsuresConsumerClosedOnException() {
    // Arrange
    when(mockConsumer.listTopics(any(Duration.class)))
        .thenThrow(new RuntimeException("Connection failed"));

    // Act
    try {
      kafkaService.getStartingOffsetsByTimestamp(LOCALHOST, LOGS_TOPIC_PATTERN, TEST_TIMESTAMP);
      Assert.fail("Should have thrown an exception");
    } catch (RuntimeException e) {
      // Expected exception
    }

    // Assert - verify consumer.close() was called even after exception
    verify(mockConsumer, times(1)).close();
  }

  // ==================== Helper Methods ====================

  /** Creates sample topics metadata for testing. */
  private Map<String, List<PartitionInfo>> createTopicsMetadata() {
    Map<String, List<PartitionInfo>> topicsMetadata = new HashMap<>();

    topicsMetadata.put(
        LOGS_TOPIC_1,
        Arrays.asList(createPartitionInfo(LOGS_TOPIC_1, 0), createPartitionInfo(LOGS_TOPIC_1, 1)));

    topicsMetadata.put(
        LOGS_TOPIC_2, Collections.singletonList(createPartitionInfo(LOGS_TOPIC_2, 0)));

    topicsMetadata.put(OTHER_TOPIC, Collections.singletonList(createPartitionInfo(OTHER_TOPIC, 0)));

    return topicsMetadata;
  }

  /** Creates expected offset map for testing. */
  private Map<TopicPartition, OffsetAndTimestamp> createExpectedOffsets(Long timestamp) {
    Map<TopicPartition, OffsetAndTimestamp> offsetsMap = new HashMap<>();

    offsetsMap.put(new TopicPartition(LOGS_TOPIC_1, 0), new OffsetAndTimestamp(100L, timestamp));
    offsetsMap.put(new TopicPartition(LOGS_TOPIC_1, 1), new OffsetAndTimestamp(200L, timestamp));
    offsetsMap.put(new TopicPartition(LOGS_TOPIC_2, 0), new OffsetAndTimestamp(300L, timestamp));

    return offsetsMap;
  }

  /**
   * Helper method to create PartitionInfo instance. Note: Using null for leader, replicas, and
   * in-sync replicas as they're not relevant for these tests.
   */
  private PartitionInfo createPartitionInfo(String topic, int partition) {
    return new PartitionInfo(topic, partition, null, null, null);
  }
}
