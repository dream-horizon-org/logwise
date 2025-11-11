package com.logwise.spark.dto.mapper;

import static org.testng.Assert.*;

import com.logwise.spark.dto.entity.KafkaAssignOption;
import com.logwise.spark.dto.entity.StartingOffsetsByTimestampOption;
import java.util.*;
import org.testng.annotations.Test;

/**
 * Unit tests for KafkaAssignOptionMapper.
 *
 * <p>Tests verify the mapping logic from StartingOffsetsByTimestampOption to KafkaAssignOption.
 */
public class KafkaAssignOptionMapperTest {

  @Test
  public void testToKafkaAssignOption_WithSingleTopicAndPartition_MapsCorrectly() {
    // Arrange
    StartingOffsetsByTimestampOption offsetOption = new StartingOffsetsByTimestampOption();
    offsetOption.addPartition("test-topic", "0", 1000L);

    // Act
    KafkaAssignOption result = KafkaAssignOptionMapper.toKafkaAssignOption.apply(offsetOption);

    // Assert
    assertNotNull(result, "Result should not be null");
    assertNotNull(result.getAssign(), "Assign map should not be null");
    assertEquals(result.getAssign().size(), 1, "Should contain exactly one topic");
    assertTrue(result.getAssign().containsKey("test-topic"), "Should contain test-topic");
    assertEquals(
        result.getAssign().get("test-topic").size(),
        1,
        "test-topic should have exactly one partition");
    assertEquals(
        result.getAssign().get("test-topic").get(0), Integer.valueOf(0), "Partition should be 0");
  }

  @Test
  public void testToKafkaAssignOption_WithMultiplePartitions_MapsAllPartitions() {
    // Arrange
    StartingOffsetsByTimestampOption offsetOption = new StartingOffsetsByTimestampOption();
    offsetOption.addPartition("test-topic", "0", 1000L);
    offsetOption.addPartition("test-topic", "1", 1100L);
    offsetOption.addPartition("test-topic", "2", 1200L);

    // Act
    KafkaAssignOption result = KafkaAssignOptionMapper.toKafkaAssignOption.apply(offsetOption);

    // Assert
    assertNotNull(result, "Result should not be null");
    assertEquals(result.getAssign().size(), 1, "Should contain exactly one topic");
    assertEquals(
        result.getAssign().get("test-topic").size(), 3, "test-topic should have three partitions");

    List<Integer> partitions = result.getAssign().get("test-topic");
    assertTrue(partitions.contains(0), "Should contain partition 0");
    assertTrue(partitions.contains(1), "Should contain partition 1");
    assertTrue(partitions.contains(2), "Should contain partition 2");
  }

  @Test
  public void testToKafkaAssignOption_WithMultipleTopics_MapsAllTopics() {
    // Arrange
    StartingOffsetsByTimestampOption offsetOption = new StartingOffsetsByTimestampOption();
    offsetOption.addPartition("logs-topic-1", "0", 1000L);
    offsetOption.addPartition("logs-topic-1", "1", 1100L);
    offsetOption.addPartition("logs-topic-2", "0", 2000L);
    offsetOption.addPartition("metrics-topic", "0", 3000L);

    // Act
    KafkaAssignOption result = KafkaAssignOptionMapper.toKafkaAssignOption.apply(offsetOption);

    // Assert
    assertNotNull(result, "Result should not be null");
    assertEquals(result.getAssign().size(), 3, "Should contain three topics");

    assertTrue(result.getAssign().containsKey("logs-topic-1"), "Should contain logs-topic-1");
    assertTrue(result.getAssign().containsKey("logs-topic-2"), "Should contain logs-topic-2");
    assertTrue(result.getAssign().containsKey("metrics-topic"), "Should contain metrics-topic");

    assertEquals(
        result.getAssign().get("logs-topic-1").size(), 2, "logs-topic-1 should have 2 partitions");
    assertEquals(
        result.getAssign().get("logs-topic-2").size(), 1, "logs-topic-2 should have 1 partition");
    assertEquals(
        result.getAssign().get("metrics-topic").size(), 1, "metrics-topic should have 1 partition");
  }

  @Test
  public void testToKafkaAssignOption_WithEmptyOffsetOption_ReturnsEmptyAssign() {
    // Arrange
    StartingOffsetsByTimestampOption offsetOption = new StartingOffsetsByTimestampOption();

    // Act
    KafkaAssignOption result = KafkaAssignOptionMapper.toKafkaAssignOption.apply(offsetOption);

    // Assert
    assertNotNull(result, "Result should not be null");
    assertNotNull(result.getAssign(), "Assign map should not be null");
    assertEquals(result.getAssign().size(), 0, "Assign map should be empty");
  }

  @Test
  public void testToKafkaAssignOption_WithTopicUsingAddTopicMethod_MapsCorrectly() {
    // Arrange
    StartingOffsetsByTimestampOption offsetOption = new StartingOffsetsByTimestampOption();
    Map<String, Long> partitions = new HashMap<>();
    partitions.put("0", 1000L);
    partitions.put("1", 1100L);
    partitions.put("2", 1200L);
    offsetOption.addTopic("test-topic", partitions);

    // Act
    KafkaAssignOption result = KafkaAssignOptionMapper.toKafkaAssignOption.apply(offsetOption);

    // Assert
    assertNotNull(result, "Result should not be null");
    assertEquals(result.getAssign().size(), 1, "Should contain exactly one topic");
    assertEquals(
        result.getAssign().get("test-topic").size(), 3, "test-topic should have three partitions");

    List<Integer> resultPartitions = result.getAssign().get("test-topic");
    assertTrue(resultPartitions.contains(0), "Should contain partition 0");
    assertTrue(resultPartitions.contains(1), "Should contain partition 1");
    assertTrue(resultPartitions.contains(2), "Should contain partition 2");
  }

  @Test
  public void testToKafkaAssignOption_PartitionKeysAreConvertedToIntegers() {
    // Arrange
    StartingOffsetsByTimestampOption offsetOption = new StartingOffsetsByTimestampOption();
    offsetOption.addPartition("test-topic", "0", 1000L);
    offsetOption.addPartition("test-topic", "5", 1100L);
    offsetOption.addPartition("test-topic", "10", 1200L);

    // Act
    KafkaAssignOption result = KafkaAssignOptionMapper.toKafkaAssignOption.apply(offsetOption);

    // Assert
    List<Integer> partitions = result.getAssign().get("test-topic");
    assertNotNull(partitions, "Partitions list should not be null");

    // Verify all partitions are Integers
    for (Integer partition : partitions) {
      assertNotNull(partition, "Partition should not be null");
      assertTrue(partition instanceof Integer, "Partition should be an Integer");
    }

    assertTrue(partitions.contains(0), "Should contain partition 0");
    assertTrue(partitions.contains(5), "Should contain partition 5");
    assertTrue(partitions.contains(10), "Should contain partition 10");
  }

  @Test
  public void testToKafkaAssignOption_PreservesTopicNames() {
    // Arrange
    StartingOffsetsByTimestampOption offsetOption = new StartingOffsetsByTimestampOption();
    offsetOption.addPartition("logs-production", "0", 1000L);
    offsetOption.addPartition("logs-staging", "0", 1100L);
    offsetOption.addPartition("logs-development", "0", 1200L);

    // Act
    KafkaAssignOption result = KafkaAssignOptionMapper.toKafkaAssignOption.apply(offsetOption);

    // Assert
    assertEquals(result.getAssign().size(), 3, "Should contain three topics");
    assertTrue(
        result.getAssign().containsKey("logs-production"), "Should contain logs-production topic");
    assertTrue(result.getAssign().containsKey("logs-staging"), "Should contain logs-staging topic");
    assertTrue(
        result.getAssign().containsKey("logs-development"),
        "Should contain logs-development topic");
  }

  @Test
  public void testToKafkaAssignOption_WithMixedPartitionNumbers_HandlesCorrectly() {
    // Arrange
    StartingOffsetsByTimestampOption offsetOption = new StartingOffsetsByTimestampOption();
    // Adding partitions in non-sequential order
    offsetOption.addPartition("test-topic", "5", 1000L);
    offsetOption.addPartition("test-topic", "2", 1100L);
    offsetOption.addPartition("test-topic", "8", 1200L);
    offsetOption.addPartition("test-topic", "0", 1300L);

    // Act
    KafkaAssignOption result = KafkaAssignOptionMapper.toKafkaAssignOption.apply(offsetOption);

    // Assert
    List<Integer> partitions = result.getAssign().get("test-topic");
    assertEquals(partitions.size(), 4, "Should have 4 partitions");

    // Verify all expected partitions are present
    assertTrue(partitions.contains(0), "Should contain partition 0");
    assertTrue(partitions.contains(2), "Should contain partition 2");
    assertTrue(partitions.contains(5), "Should contain partition 5");
    assertTrue(partitions.contains(8), "Should contain partition 8");
  }

  @Test
  public void testToKafkaAssignOption_ResultIsIndependentOfInput() {
    // Arrange
    StartingOffsetsByTimestampOption offsetOption = new StartingOffsetsByTimestampOption();
    offsetOption.addPartition("test-topic", "0", 1000L);

    // Act
    KafkaAssignOption result = KafkaAssignOptionMapper.toKafkaAssignOption.apply(offsetOption);

    // Modify the original offset option
    offsetOption.addPartition("test-topic", "1", 2000L);
    offsetOption.addPartition("new-topic", "0", 3000L);

    // Assert - result should not be affected by changes to input
    assertEquals(
        result.getAssign().size(),
        1,
        "Result should still have only one topic after input modification");
    assertEquals(
        result.getAssign().get("test-topic").size(),
        1,
        "Result should still have only one partition after input modification");
    assertFalse(
        result.getAssign().containsKey("new-topic"),
        "Result should not contain topics added after mapping");
  }

  @Test
  public void testToKafkaAssignOption_MapperCanBeReused() {
    // Arrange
    StartingOffsetsByTimestampOption offsetOption1 = new StartingOffsetsByTimestampOption();
    offsetOption1.addPartition("topic-1", "0", 1000L);

    StartingOffsetsByTimestampOption offsetOption2 = new StartingOffsetsByTimestampOption();
    offsetOption2.addPartition("topic-2", "0", 2000L);

    // Act - Use the same mapper function multiple times
    KafkaAssignOption result1 = KafkaAssignOptionMapper.toKafkaAssignOption.apply(offsetOption1);
    KafkaAssignOption result2 = KafkaAssignOptionMapper.toKafkaAssignOption.apply(offsetOption2);

    // Assert - Each result should be independent and correct
    assertNotNull(result1, "First result should not be null");
    assertNotNull(result2, "Second result should not be null");

    assertTrue(result1.getAssign().containsKey("topic-1"), "Result 1 should contain topic-1");
    assertFalse(result1.getAssign().containsKey("topic-2"), "Result 1 should not contain topic-2");

    assertTrue(result2.getAssign().containsKey("topic-2"), "Result 2 should contain topic-2");
    assertFalse(result2.getAssign().containsKey("topic-1"), "Result 2 should not contain topic-1");
  }

  @Test
  public void testToKafkaAssignOption_WithLargePartitionNumbers_HandlesCorrectly() {
    // Arrange
    StartingOffsetsByTimestampOption offsetOption = new StartingOffsetsByTimestampOption();
    offsetOption.addPartition("test-topic", "999", 1000L);
    offsetOption.addPartition("test-topic", "1000", 1100L);
    offsetOption.addPartition("test-topic", "9999", 1200L);

    // Act
    KafkaAssignOption result = KafkaAssignOptionMapper.toKafkaAssignOption.apply(offsetOption);

    // Assert
    List<Integer> partitions = result.getAssign().get("test-topic");
    assertEquals(partitions.size(), 3, "Should have 3 partitions");
    assertTrue(partitions.contains(999), "Should contain partition 999");
    assertTrue(partitions.contains(1000), "Should contain partition 1000");
    assertTrue(partitions.contains(9999), "Should contain partition 9999");
  }
}
