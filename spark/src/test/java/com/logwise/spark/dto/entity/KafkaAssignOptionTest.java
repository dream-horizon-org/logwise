package com.logwise.spark.dto.entity;

import static org.testng.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for KafkaAssignOption DTO.
 *
 * <p>Tests verify the DTO functionality including validation, data management, and JSON
 * serialization.
 */
public class KafkaAssignOptionTest {

  private KafkaAssignOption assignOption;

  @BeforeMethod
  public void setUp() {
    assignOption = new KafkaAssignOption();
  }

  @Test
  public void testConstructor_InitializesEmptyMap() {
    // Assert
    assertNotNull(assignOption.getAssign(), "Assign map should not be null");
    assertEquals(assignOption.getAssign().size(), 0, "Assign map should be empty initially");
  }

  @Test
  public void testAddPartition_WithValidInput_AddsPartitionSuccessfully() {
    // Act
    assignOption.addPartition("test-topic", 0);

    // Assert
    Map<String, List<Integer>> assign = assignOption.getAssign();
    assertEquals(assign.size(), 1, "Should contain one topic");
    assertTrue(assign.containsKey("test-topic"), "Should contain test-topic");
    assertEquals(assign.get("test-topic").size(), 1, "test-topic should have one partition");
    assertEquals(assign.get("test-topic").get(0), Integer.valueOf(0), "Partition should be 0");
  }

  @Test
  public void testAddPartition_WithMultiplePartitions_AddsAllPartitions() {
    // Act
    assignOption.addPartition("test-topic", 0);
    assignOption.addPartition("test-topic", 1);
    assignOption.addPartition("test-topic", 2);

    // Assert
    List<Integer> partitions = assignOption.getAssign().get("test-topic");
    assertNotNull(partitions, "Partitions list should not be null");
    assertEquals(partitions.size(), 3, "Should have three partitions");
    assertTrue(partitions.contains(0), "Should contain partition 0");
    assertTrue(partitions.contains(1), "Should contain partition 1");
    assertTrue(partitions.contains(2), "Should contain partition 2");
  }

  @Test
  public void testAddPartition_WithMultipleTopics_AddsAllTopics() {
    // Act
    assignOption.addPartition("topic-1", 0);
    assignOption.addPartition("topic-2", 0);
    assignOption.addPartition("topic-3", 0);

    // Assert
    Map<String, List<Integer>> assign = assignOption.getAssign();
    assertEquals(assign.size(), 3, "Should contain three topics");
    assertTrue(assign.containsKey("topic-1"), "Should contain topic-1");
    assertTrue(assign.containsKey("topic-2"), "Should contain topic-2");
    assertTrue(assign.containsKey("topic-3"), "Should contain topic-3");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddPartition_WithNullTopicName_ThrowsException() {
    // Act - should throw IllegalArgumentException
    assignOption.addPartition(null, 0);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddPartition_WithEmptyTopicName_ThrowsException() {
    // Act - should throw IllegalArgumentException
    assignOption.addPartition("", 0);
  }

  @Test
  public void testAddPartition_WithSamePartitionTwice_AddsBothOccurrences() {
    // Act
    assignOption.addPartition("test-topic", 0);
    assignOption.addPartition("test-topic", 0);

    // Assert
    List<Integer> partitions = assignOption.getAssign().get("test-topic");
    assertEquals(partitions.size(), 2, "Should have two entries (allows duplicates)");
    assertEquals(partitions.get(0), Integer.valueOf(0), "First entry should be 0");
    assertEquals(partitions.get(1), Integer.valueOf(0), "Second entry should be 0");
  }

  @Test
  public void testAddPartition_UpdatesExistingTopic_AddsNewPartition() {
    // Arrange
    assignOption.addPartition("test-topic", 0);

    // Act
    assignOption.addPartition("test-topic", 1);

    // Assert
    List<Integer> partitions = assignOption.getAssign().get("test-topic");
    assertEquals(partitions.size(), 2, "Should have two partitions");
    assertEquals(partitions.get(0), Integer.valueOf(0), "First partition should be 0");
    assertEquals(partitions.get(1), Integer.valueOf(1), "Second partition should be 1");
  }

  @Test
  public void testAddTopic_WithValidPartitionsList_AddsTopicSuccessfully() {
    // Arrange
    List<Integer> partitions = Arrays.asList(0, 1, 2);

    // Act
    assignOption.addTopic("test-topic", partitions);

    // Assert
    Map<String, List<Integer>> assign = assignOption.getAssign();
    assertEquals(assign.size(), 1, "Should contain one topic");
    assertTrue(assign.containsKey("test-topic"), "Should contain test-topic");
    assertEquals(assign.get("test-topic").size(), 3, "test-topic should have three partitions");
    assertEquals(assign.get("test-topic").get(0), Integer.valueOf(0), "Partition 0 mismatch");
    assertEquals(assign.get("test-topic").get(1), Integer.valueOf(1), "Partition 1 mismatch");
    assertEquals(assign.get("test-topic").get(2), Integer.valueOf(2), "Partition 2 mismatch");
  }

  @Test
  public void testAddTopic_WithNullPartitionsList_AddsTopicWithEmptyList() {
    // Act
    assignOption.addTopic("test-topic", null);

    // Assert
    Map<String, List<Integer>> assign = assignOption.getAssign();
    assertEquals(assign.size(), 1, "Should contain one topic");
    assertTrue(assign.containsKey("test-topic"), "Should contain test-topic");
    assertEquals(
        assign.get("test-topic").size(), 0, "test-topic should have empty partitions list");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddTopic_WithNullTopicName_ThrowsException() {
    // Arrange
    List<Integer> partitions = Arrays.asList(0, 1);

    // Act - should throw IllegalArgumentException
    assignOption.addTopic(null, partitions);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testAddTopic_WithEmptyTopicName_ThrowsException() {
    // Arrange
    List<Integer> partitions = Arrays.asList(0, 1);

    // Act - should throw IllegalArgumentException
    assignOption.addTopic("", partitions);
  }

  @Test
  public void testAddTopic_WithEmptyPartitionsList_AddsTopicWithEmptyList() {
    // Arrange
    List<Integer> partitions = new ArrayList<>();

    // Act
    assignOption.addTopic("test-topic", partitions);

    // Assert
    assertTrue(assignOption.getAssign().containsKey("test-topic"), "Should contain test-topic");
    assertEquals(
        assignOption.getAssign().get("test-topic").size(),
        0,
        "test-topic should have empty partitions list");
  }

  @Test
  public void testAddTopic_OverwritesExistingTopic() {
    // Arrange
    assignOption.addPartition("test-topic", 0);
    assignOption.addPartition("test-topic", 1);

    List<Integer> newPartitions = Arrays.asList(5, 6);

    // Act
    assignOption.addTopic("test-topic", newPartitions);

    // Assert
    List<Integer> partitions = assignOption.getAssign().get("test-topic");
    assertEquals(partitions.size(), 2, "Should have two partitions after overwriting");
    assertFalse(partitions.contains(0), "Old partition 0 should be removed");
    assertFalse(partitions.contains(1), "Old partition 1 should be removed");
    assertTrue(partitions.contains(5), "New partition 5 should be present");
    assertTrue(partitions.contains(6), "New partition 6 should be present");
  }

  @Test
  public void testToJson_WithEmptyAssign_ReturnsEmptyJsonObject() throws Exception {
    // Act
    String json = assignOption.toJson();

    // Assert
    assertNotNull(json, "JSON should not be null");
    ObjectMapper mapper = new ObjectMapper();
    Map result = mapper.readValue(json, Map.class);
    assertEquals(result.size(), 0, "JSON should represent empty object");
  }

  @Test
  public void testToJson_WithSingleTopicAndPartition_ReturnsValidJson() throws Exception {
    // Arrange
    assignOption.addPartition("test-topic", 0);

    // Act
    String json = assignOption.toJson();

    // Assert
    assertNotNull(json, "JSON should not be null");
    ObjectMapper mapper = new ObjectMapper();
    Map<String, List<Integer>> result = mapper.readValue(json, Map.class);

    assertEquals(result.size(), 1, "JSON should contain one topic");
    assertTrue(result.containsKey("test-topic"), "JSON should contain test-topic");
    assertEquals(result.get("test-topic").size(), 1, "test-topic should have one partition");
    assertEquals(result.get("test-topic").get(0), Integer.valueOf(0), "Partition should be 0");
  }

  @Test
  public void testToJson_WithMultipleTopicsAndPartitions_ReturnsValidJson() throws Exception {
    // Arrange
    assignOption.addPartition("topic-1", 0);
    assignOption.addPartition("topic-1", 1);
    assignOption.addPartition("topic-2", 0);

    // Act
    String json = assignOption.toJson();

    // Assert
    assertNotNull(json, "JSON should not be null");
    ObjectMapper mapper = new ObjectMapper();
    Map<String, List<Integer>> result = mapper.readValue(json, Map.class);

    assertEquals(result.size(), 2, "JSON should contain two topics");
    assertTrue(result.containsKey("topic-1"), "JSON should contain topic-1");
    assertTrue(result.containsKey("topic-2"), "JSON should contain topic-2");
    assertEquals(result.get("topic-1").size(), 2, "topic-1 should have two partitions");
    assertEquals(result.get("topic-2").size(), 1, "topic-2 should have one partition");
  }

  @Test
  public void testToString_ReturnsNonNullString() {
    // Arrange
    assignOption.addPartition("test-topic", 0);

    // Act
    String result = assignOption.toString();

    // Assert
    assertNotNull(result, "toString should not return null");
    assertTrue(result.contains("KafkaAssignOption"), "toString should contain class name");
    assertTrue(result.contains("test-topic"), "toString should contain topic name");
  }

  @Test
  public void testGetAssign_ReturnsActualMap() {
    // Arrange
    assignOption.addPartition("test-topic", 0);

    // Act
    Map<String, List<Integer>> assign = assignOption.getAssign();
    assign.put("new-topic", new ArrayList<>());

    // Assert - verify that modifying returned map affects the internal state
    assertTrue(
        assignOption.getAssign().containsKey("new-topic"),
        "Modifications to returned map should affect internal state");
  }

  @Test
  public void testAddPartition_WithLargePartitionNumber_HandlesCorrectly() {
    // Act
    assignOption.addPartition("test-topic", 9999);

    // Assert
    assertTrue(
        assignOption.getAssign().get("test-topic").contains(9999),
        "Should handle large partition numbers");
  }

  @Test
  public void testAddPartition_WithZeroPartition_HandlesCorrectly() {
    // Act
    assignOption.addPartition("test-topic", 0);

    // Assert
    assertTrue(
        assignOption.getAssign().get("test-topic").contains(0),
        "Should handle zero partition number");
  }

  @Test
  public void testAddPartition_WithNegativePartition_HandlesCorrectly() {
    // Act
    assignOption.addPartition("test-topic", -1);

    // Assert
    assertTrue(
        assignOption.getAssign().get("test-topic").contains(-1),
        "Should handle negative partition numbers (though unusual)");
  }

  @Test
  public void testAddTopic_WithDuplicatePartitionsInList_AddsBothOccurrences() {
    // Arrange
    List<Integer> partitions = Arrays.asList(0, 1, 0, 2, 1);

    // Act
    assignOption.addTopic("test-topic", partitions);

    // Assert
    List<Integer> resultPartitions = assignOption.getAssign().get("test-topic");
    assertEquals(resultPartitions.size(), 5, "Should contain all 5 elements including duplicates");

    // Count occurrences
    long count0 = resultPartitions.stream().filter(p -> p == 0).count();
    long count1 = resultPartitions.stream().filter(p -> p == 1).count();

    assertEquals(count0, 2, "Should have two occurrences of partition 0");
    assertEquals(count1, 2, "Should have two occurrences of partition 1");
  }

  @Test
  public void testAddTopic_WithNonSequentialPartitions_PreservesOrder() {
    // Arrange
    List<Integer> partitions = Arrays.asList(5, 2, 8, 0);

    // Act
    assignOption.addTopic("test-topic", partitions);

    // Assert
    List<Integer> resultPartitions = assignOption.getAssign().get("test-topic");
    assertEquals(resultPartitions.get(0), Integer.valueOf(5), "First partition should be 5");
    assertEquals(resultPartitions.get(1), Integer.valueOf(2), "Second partition should be 2");
    assertEquals(resultPartitions.get(2), Integer.valueOf(8), "Third partition should be 8");
    assertEquals(resultPartitions.get(3), Integer.valueOf(0), "Fourth partition should be 0");
  }

  @Test
  public void testAddPartition_PreservesInsertionOrder() {
    // Act
    assignOption.addPartition("test-topic", 5);
    assignOption.addPartition("test-topic", 2);
    assignOption.addPartition("test-topic", 8);

    // Assert
    List<Integer> partitions = assignOption.getAssign().get("test-topic");
    assertEquals(partitions.get(0), Integer.valueOf(5), "First added partition should be first");
    assertEquals(partitions.get(1), Integer.valueOf(2), "Second added partition should be second");
    assertEquals(partitions.get(2), Integer.valueOf(8), "Third added partition should be third");
  }

  @Test
  public void testAddTopic_WithLargePartitionList_HandlesCorrectly() {
    // Arrange
    List<Integer> partitions = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      partitions.add(i);
    }

    // Act
    assignOption.addTopic("test-topic", partitions);

    // Assert
    List<Integer> resultPartitions = assignOption.getAssign().get("test-topic");
    assertEquals(resultPartitions.size(), 1000, "Should handle large partition lists");
    assertEquals(resultPartitions.get(0), Integer.valueOf(0), "First partition should be 0");
    assertEquals(resultPartitions.get(999), Integer.valueOf(999), "Last partition should be 999");
  }
}
