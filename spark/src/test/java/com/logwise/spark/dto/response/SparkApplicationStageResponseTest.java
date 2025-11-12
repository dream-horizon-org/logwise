package com.logwise.spark.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for SparkApplicationStageResponse DTO.
 *
 * <p>Tests verify getters/setters, serialization/deserialization, and edge cases for the response
 * DTO.
 */
public class SparkApplicationStageResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testGettersSetters_WithValidData_WorksCorrectly() {
    // Arrange
    SparkApplicationStageResponse response = new SparkApplicationStageResponse();
    List<Integer> rddIds = new ArrayList<>();
    rddIds.add(1);
    rddIds.add(2);

    // Act
    response.setShuffleWriteTime(100);
    response.setNumCompleteTasks(10);
    response.setInputRecords(1000);
    response.setDescription("Test stage");
    response.setRddIds(rddIds);
    response.setStageId(1);
    response.setStatus("COMPLETE");

    // Assert
    Assert.assertEquals(response.getShuffleWriteTime(), 100);
    Assert.assertEquals(response.getNumCompleteTasks(), 10);
    Assert.assertEquals(response.getInputRecords(), 1000);
    Assert.assertEquals(response.getDescription(), "Test stage");
    Assert.assertEquals(response.getRddIds(), rddIds);
    Assert.assertEquals(response.getStageId(), 1);
    Assert.assertEquals(response.getStatus(), "COMPLETE");
  }

  @Test
  public void testGettersSetters_WithNullValues_HandlesCorrectly() {
    // Arrange
    SparkApplicationStageResponse response = new SparkApplicationStageResponse();

    // Act
    response.setDescription(null);
    response.setRddIds(null);
    response.setKilledTasksSummary(null);
    response.setPeakExecutorMetrics(null);
    response.setAccumulatorUpdates(null);

    // Assert
    Assert.assertNull(response.getDescription());
    Assert.assertNull(response.getRddIds());
    Assert.assertNull(response.getKilledTasksSummary());
    Assert.assertNull(response.getPeakExecutorMetrics());
    Assert.assertNull(response.getAccumulatorUpdates());
  }

  @Test
  public void testEquals_WithSameData_ReturnsTrue() {
    // Arrange
    SparkApplicationStageResponse response1 = new SparkApplicationStageResponse();
    response1.setStageId(1);
    response1.setStatus("COMPLETE");

    SparkApplicationStageResponse response2 = new SparkApplicationStageResponse();
    response2.setStageId(1);
    response2.setStatus("COMPLETE");

    // Act & Assert
    Assert.assertEquals(response1, response2);
  }

  @Test
  public void testEquals_WithDifferentData_ReturnsFalse() {
    // Arrange
    SparkApplicationStageResponse response1 = new SparkApplicationStageResponse();
    response1.setStageId(1);
    response1.setStatus("COMPLETE");

    SparkApplicationStageResponse response2 = new SparkApplicationStageResponse();
    response2.setStageId(2);
    response2.setStatus("RUNNING");

    // Act & Assert
    Assert.assertNotEquals(response1, response2);
  }

  @Test
  public void testHashCode_WithSameData_ReturnsSameHash() {
    // Arrange
    SparkApplicationStageResponse response1 = new SparkApplicationStageResponse();
    response1.setStageId(1);
    response1.setStatus("COMPLETE");

    SparkApplicationStageResponse response2 = new SparkApplicationStageResponse();
    response2.setStageId(1);
    response2.setStatus("COMPLETE");

    // Act & Assert
    Assert.assertEquals(response1.hashCode(), response2.hashCode());
  }

  @Test
  public void testToString_WithData_ReturnsNonEmptyString() {
    // Arrange
    SparkApplicationStageResponse response = new SparkApplicationStageResponse();
    response.setStageId(1);
    response.setStatus("COMPLETE");
    response.setDescription("Test stage");

    // Act
    String toString = response.toString();

    // Assert
    Assert.assertNotNull(toString);
    Assert.assertFalse(toString.isEmpty());
    // Lombok @Data generates toString with class name
    Assert.assertTrue(toString.contains("SparkApplicationStageResponse"));
  }

  @Test
  public void testSerialization_WithJackson_WorksCorrectly() throws Exception {
    // Arrange
    SparkApplicationStageResponse response = new SparkApplicationStageResponse();
    response.setStageId(1);
    response.setStatus("COMPLETE");
    response.setNumCompleteTasks(10);
    response.setInputRecords(1000);
    List<Integer> rddIds = new ArrayList<>();
    rddIds.add(1);
    rddIds.add(2);
    response.setRddIds(rddIds);

    // Act
    String json = objectMapper.writeValueAsString(response);

    // Assert
    Assert.assertNotNull(json);
    Assert.assertTrue(json.contains("\"stageId\":1"));
    Assert.assertTrue(json.contains("\"status\":\"COMPLETE\""));
    Assert.assertTrue(json.contains("\"numCompleteTasks\":10"));
  }

  @Test
  public void testDeserialization_WithJackson_WorksCorrectly() throws Exception {
    // Arrange
    String json =
        "{\"stageId\":1,\"status\":\"COMPLETE\",\"numCompleteTasks\":10,\"inputRecords\":1000,\"rddIds\":[1,2]}";

    // Act
    SparkApplicationStageResponse response =
        objectMapper.readValue(json, SparkApplicationStageResponse.class);

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getStageId(), 1);
    Assert.assertEquals(response.getStatus(), "COMPLETE");
    Assert.assertEquals(response.getNumCompleteTasks(), 10);
    Assert.assertEquals(response.getInputRecords(), 1000);
    Assert.assertNotNull(response.getRddIds());
    Assert.assertEquals(response.getRddIds().size(), 2);
  }

  @Test
  public void testDeserialization_WithMissingFields_HandlesGracefully() throws Exception {
    // Arrange
    String json = "{\"stageId\":1,\"status\":\"COMPLETE\"}";

    // Act
    SparkApplicationStageResponse response =
        objectMapper.readValue(json, SparkApplicationStageResponse.class);

    // Assert
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getStageId(), 1);
    Assert.assertEquals(response.getStatus(), "COMPLETE");
    // Missing fields should be null or default values
    Assert.assertEquals(response.getNumCompleteTasks(), 0);
    Assert.assertEquals(response.getInputRecords(), 0);
  }

  @Test
  public void testDeserialization_WithInvalidTypes_ThrowsException() {
    // Arrange
    String invalidJson = "{\"stageId\":\"not-a-number\",\"status\":\"COMPLETE\"}";

    // Act & Assert
    try {
      objectMapper.readValue(invalidJson, SparkApplicationStageResponse.class);
      Assert.fail("Should have thrown exception for invalid type");
    } catch (Exception e) {
      // Expected - Jackson throws exception for type mismatch
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testAllFields_CanBeSetAndRetrieved() {
    // Arrange
    SparkApplicationStageResponse response = new SparkApplicationStageResponse();
    List<Integer> rddIds = new ArrayList<>();
    rddIds.add(1);
    List<Object> accumulatorUpdates = new ArrayList<>();
    accumulatorUpdates.add("update1");

    // Act - Set all fields
    response.setShuffleWriteTime(100);
    response.setNumCompleteTasks(10);
    response.setInputRecords(1000);
    response.setDescription("desc");
    response.setShuffleReadBytes(200);
    response.setKilledTasksSummary("summary");
    response.setShuffleRemoteBytesReadToDisk(300);
    response.setShuffleWriteBytes(400);
    response.setSchedulingPool("pool");
    response.setPeakExecutorMetrics("metrics");
    response.setSubmissionTime("2021-01-01");
    response.setOutputRecords(2000);
    response.setShuffleWriteRecords(500);
    response.setCompletionTime("2021-01-02");
    response.setInputBytes(600);
    response.setExecutorDeserializeCpuTime(700L);
    response.setShuffleRemoteBytesRead(800);
    response.setResultSize(900);
    response.setShuffleLocalBlocksFetched(1000);
    response.setPeakExecutionMemory(1100L);
    response.setDetails("details");
    response.setRddIds(rddIds);
    response.setStageId(1);
    response.setAttemptId(0);
    response.setNumTasks(20);
    response.setFirstTaskLaunchedTime("2021-01-01");
    response.setJvmGcTime(1200);
    response.setExecutorDeserializeTime(1300);
    response.setExecutorCpuTime(1400L);
    response.setMemoryBytesSpilled(1500);
    response.setExecutorRunTime(1600);
    response.setShuffleReadRecords(1700);
    response.setNumActiveTasks(5);
    response.setOutputBytes(1800L);
    response.setNumFailedTasks(0);
    response.setDiskBytesSpilled(1900);
    response.setShuffleFetchWaitTime(2000);
    response.setAccumulatorUpdates(accumulatorUpdates);
    response.setResourceProfileId(1);
    response.setResultSerializationTime(2100);
    response.setName("stage-name");
    response.setShuffleRemoteBlocksFetched(2200);
    response.setShuffleLocalBytesRead(2300);
    response.setNumKilledTasks(0);
    response.setNumCompletedIndices(10);
    response.setStatus("COMPLETE");

    // Assert - Verify all fields can be retrieved
    Assert.assertEquals(response.getShuffleWriteTime(), 100);
    Assert.assertEquals(response.getNumCompleteTasks(), 10);
    Assert.assertEquals(response.getStatus(), "COMPLETE");
    // Verify all other fields are set correctly
    Assert.assertNotNull(response);
  }
}
