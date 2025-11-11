package com.logwise.spark.dto.entity;

import static org.testng.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for StartingOffsetsByTimestampOption DTO.
 *
 * <p>Tests verify the DTO functionality including validation, data management, and JSON
 * serialization.
 */
public class StartingOffsetsByTimestampOptionTest {

  private StartingOffsetsByTimestampOption offsetOption;

  @BeforeMethod
  public void setUp() {
    offsetOption = new StartingOffsetsByTimestampOption();
  }

  @Test
  public void testConstructor_InitializesEmptyMap() {
    // Assert
    assertNotNull(offsetOption.getOffsetByTimestamp(), "Offset map should not be null");
    assertEquals(
        offsetOption.getOffsetByTimestamp().size(), 0, "Offset map should be empty initially");
  }

  @Test
  public void testAddPartition_WithValidInput_AddsPartitionSuccessfully() {
    // Act
    offsetOption.addPartition("test-topic", "0", 1000L);

    // Assert
    Map<String, Map<String, Long>> offsets = offsetOption.getOffsetByTimestamp();
    assertEquals(offsets.size(), 1, "Should contain one topic");
    assertTrue(offsets.containsKey("test-topic"), "Should contain test-topic");
    assertEquals(offsets.get("test-topic").size(), 1, "test-topic should have one partition");
    assertEquals(
        offsets.get("test-topic").get("0"),
        Long.valueOf(1000L),
        "Partition 0 should have offset 1000");
  }

  @Test
  public void testAddPartition_WithMultiplePartitions_AddsAllPartitions() {
    // Act
    offsetOption.addPartition("test-topic", "0", 1000L);
    offsetOption.addPartition("test-topic", "1", 2000L);
    offsetOption.addPartition("test-topic", "2", 3000L);

    // Assert
    Map<String, Long> partitions = offsetOption.getOffsetByTimestamp().get("test-topic");
    assertNotNull(partitions, "Partitions map should not be null");
    assertEquals(partitions.size(), 3, "Should have three partitions");
    assertEquals(partitions.get("0"), Long.valueOf(1000L), "Partition 0 offset mismatch");
    assertEquals(partitions.get("1"), Long.valueOf(2000L), "Partition 1 offset mismatch");
    assertEquals(partitions.get("2"), Long.valueOf(3000L), "Partition 2 offset mismatch");
  }

  @Test
  public void testAddPartition_WithMultipleTopics_AddsAllTopics() {
    // Act
    offsetOption.addPartition("topic-1", "0", 1000L);
    offsetOption.addPartition("topic-2", "0", 2000L);
    offsetOption.addPartition("topic-3", "0", 3000L);

    // Assert
    Map<String, Map<String, Long>> offsets = offsetOption.getOffsetByTimestamp();
    assertEquals(offsets.size(), 3, "Should contain three topics");
    assertTrue(offsets.containsKey("topic-1"), "Should contain topic-1");
    assertTrue(offsets.containsKey("topic-2"), "Should contain topic-2");
    assertTrue(offsets.containsKey("topic-3"), "Should contain topic-3");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddPartition_WithNullTopicName_ThrowsException() {
    // Act - should throw IllegalArgumentException
    offsetOption.addPartition(null, "0", 1000L);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddPartition_WithEmptyTopicName_ThrowsException() {
    // Act - should throw IllegalArgumentException
    offsetOption.addPartition("", "0", 1000L);
  }

  @Test
  public void testAddPartition_UpdatesExistingTopic_AddsNewPartition() {
    // Arrange
    offsetOption.addPartition("test-topic", "0", 1000L);

    // Act
    offsetOption.addPartition("test-topic", "1", 2000L);

    // Assert
    Map<String, Long> partitions = offsetOption.getOffsetByTimestamp().get("test-topic");
    assertEquals(partitions.size(), 2, "Should have two partitions");
    assertEquals(partitions.get("0"), Long.valueOf(1000L), "Partition 0 should be preserved");
    assertEquals(partitions.get("1"), Long.valueOf(2000L), "Partition 1 should be added");
  }

  @Test
  public void testAddPartition_WithSamePartitionTwice_OverwritesValue() {
    // Arrange
    offsetOption.addPartition("test-topic", "0", 1000L);

    // Act - Add same partition with different offset
    offsetOption.addPartition("test-topic", "0", 5000L);

    // Assert
    Map<String, Long> partitions = offsetOption.getOffsetByTimestamp().get("test-topic");
    assertEquals(partitions.size(), 1, "Should still have one partition");
    assertEquals(
        partitions.get("0"), Long.valueOf(5000L), "Partition 0 should have updated offset");
  }

  @Test
  public void testAddTopic_WithValidPartitionsMap_AddsTopicSuccessfully() {
    // Arrange
    Map<String, Long> partitions = new HashMap<>();
    partitions.put("0", 1000L);
    partitions.put("1", 2000L);

    // Act
    offsetOption.addTopic("test-topic", partitions);

    // Assert
    Map<String, Map<String, Long>> offsets = offsetOption.getOffsetByTimestamp();
    assertEquals(offsets.size(), 1, "Should contain one topic");
    assertTrue(offsets.containsKey("test-topic"), "Should contain test-topic");
    assertEquals(offsets.get("test-topic").size(), 2, "test-topic should have two partitions");
    assertEquals(
        offsets.get("test-topic").get("0"), Long.valueOf(1000L), "Partition 0 offset mismatch");
    assertEquals(
        offsets.get("test-topic").get("1"), Long.valueOf(2000L), "Partition 1 offset mismatch");
  }

  @Test
  public void testAddTopic_WithNullPartitionsMap_AddsTopicWithEmptyMap() {
    // Act
    offsetOption.addTopic("test-topic", null);

    // Assert
    Map<String, Map<String, Long>> offsets = offsetOption.getOffsetByTimestamp();
    assertEquals(offsets.size(), 1, "Should contain one topic");
    assertTrue(offsets.containsKey("test-topic"), "Should contain test-topic");
    assertEquals(
        offsets.get("test-topic").size(), 0, "test-topic should have empty partitions map");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddTopic_WithNullTopicName_ThrowsException() {
    // Arrange
    Map<String, Long> partitions = new HashMap<>();
    partitions.put("0", 1000L);

    // Act - should throw IllegalArgumentException
    offsetOption.addTopic(null, partitions);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddTopic_WithEmptyTopicName_ThrowsException() {
    // Arrange
    Map<String, Long> partitions = new HashMap<>();
    partitions.put("0", 1000L);

    // Act - should throw IllegalArgumentException
    offsetOption.addTopic("", partitions);
  }

  @Test
  public void testAddTopic_WithEmptyPartitionsMap_AddsTopicWithEmptyMap() {
    // Arrange
    Map<String, Long> partitions = new HashMap<>();

    // Act
    offsetOption.addTopic("test-topic", partitions);

    // Assert
    assertTrue(
        offsetOption.getOffsetByTimestamp().containsKey("test-topic"), "Should contain test-topic");
    assertEquals(
        offsetOption.getOffsetByTimestamp().get("test-topic").size(),
        0,
        "test-topic should have empty partitions map");
  }

  @Test
  public void testAddTopic_OverwritesExistingTopic() {
    // Arrange
    offsetOption.addPartition("test-topic", "0", 1000L);
    offsetOption.addPartition("test-topic", "1", 2000L);

    Map<String, Long> newPartitions = new HashMap<>();
    newPartitions.put("5", 5000L);

    // Act
    offsetOption.addTopic("test-topic", newPartitions);

    // Assert
    Map<String, Long> partitions = offsetOption.getOffsetByTimestamp().get("test-topic");
    assertEquals(partitions.size(), 1, "Should have only one partition after overwriting");
    assertFalse(partitions.containsKey("0"), "Old partition 0 should be removed");
    assertFalse(partitions.containsKey("1"), "Old partition 1 should be removed");
    assertTrue(partitions.containsKey("5"), "New partition 5 should be present");
  }

  @Test
  public void testToJson_WithEmptyOffsets_ReturnsEmptyJsonObject() throws Exception {
    // Act
    String json = offsetOption.toJson();

    // Assert
    assertNotNull(json, "JSON should not be null");
    ObjectMapper mapper = new ObjectMapper();
    Map result = mapper.readValue(json, Map.class);
    assertEquals(result.size(), 0, "JSON should represent empty object");
  }

  @Test
  public void testToJson_WithSingleTopicAndPartition_ReturnsValidJson() throws Exception {
    // Arrange
    offsetOption.addPartition("test-topic", "0", 1000L);

    // Act
    String json = offsetOption.toJson();

    // Assert
    assertNotNull(json, "JSON should not be null");
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Map<String, Integer>> result = mapper.readValue(json, Map.class);

    assertEquals(result.size(), 1, "JSON should contain one topic");
    assertTrue(result.containsKey("test-topic"), "JSON should contain test-topic");
    assertEquals(result.get("test-topic").size(), 1, "test-topic should have one partition");
    assertEquals(
        result.get("test-topic").get("0"),
        Integer.valueOf(1000),
        "Partition 0 should have offset 1000");
  }

  @Test
  public void testToJson_WithMultipleTopicsAndPartitions_ReturnsValidJson() throws Exception {
    // Arrange
    offsetOption.addPartition("topic-1", "0", 1000L);
    offsetOption.addPartition("topic-1", "1", 2000L);
    offsetOption.addPartition("topic-2", "0", 3000L);

    // Act
    String json = offsetOption.toJson();

    // Assert
    assertNotNull(json, "JSON should not be null");
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Map<String, Integer>> result = mapper.readValue(json, Map.class);

    assertEquals(result.size(), 2, "JSON should contain two topics");
    assertTrue(result.containsKey("topic-1"), "JSON should contain topic-1");
    assertTrue(result.containsKey("topic-2"), "JSON should contain topic-2");
    assertEquals(result.get("topic-1").size(), 2, "topic-1 should have two partitions");
    assertEquals(result.get("topic-2").size(), 1, "topic-2 should have one partition");
  }

  @Test
  public void testToString_ReturnsNonNullString() {
    // Arrange
    offsetOption.addPartition("test-topic", "0", 1000L);

    // Act
    String result = offsetOption.toString();

    // Assert
    assertNotNull(result, "toString should not return null");
    assertTrue(
        result.contains("StartingOffsetsByTimestampOption"), "toString should contain class name");
    assertTrue(result.contains("test-topic"), "toString should contain topic name");
  }

  @Test
  public void testGetOffsetByTimestamp_ReturnsActualMap() {
    // Arrange
    offsetOption.addPartition("test-topic", "0", 1000L);

    // Act
    Map<String, Map<String, Long>> offsets = offsetOption.getOffsetByTimestamp();
    offsets.put("new-topic", new HashMap<>());

    // Assert - verify that modifying returned map affects the internal state
    assertTrue(
        offsetOption.getOffsetByTimestamp().containsKey("new-topic"),
        "Modifications to returned map should affect internal state");
  }

  @Test
  public void testAddPartition_WithLargeOffsetValue_HandlesCorrectly() {
    // Arrange
    Long largeOffset = Long.MAX_VALUE;

    // Act
    offsetOption.addPartition("test-topic", "0", largeOffset);

    // Assert
    assertEquals(
        offsetOption.getOffsetByTimestamp().get("test-topic").get("0"),
        largeOffset,
        "Should handle large offset values correctly");
  }

  @Test
  public void testAddPartition_WithZeroOffset_HandlesCorrectly() {
    // Act
    offsetOption.addPartition("test-topic", "0", 0L);

    // Assert
    assertEquals(
        offsetOption.getOffsetByTimestamp().get("test-topic").get("0"),
        Long.valueOf(0L),
        "Should handle zero offset correctly");
  }

  @Test
  public void testAddPartition_WithNegativeOffset_HandlesCorrectly() {
    // Act
    offsetOption.addPartition("test-topic", "0", -1L);

    // Assert
    assertEquals(
        offsetOption.getOffsetByTimestamp().get("test-topic").get("0"),
        Long.valueOf(-1L),
        "Should handle negative offset correctly");
  }

  @Test
  public void testAddPartition_WithHighPartitionNumber_HandlesCorrectly() {
    // Act
    offsetOption.addPartition("test-topic", "9999", 1000L);

    // Assert
    assertTrue(
        offsetOption.getOffsetByTimestamp().get("test-topic").containsKey("9999"),
        "Should handle high partition numbers");
  }
}
