package com.logwise.spark.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for SparkHistoryServerGetApplicationsResponse DTO.
 *
 * <p>
 * Tests verify getters/setters, serialization/deserialization, and nested
 * object handling.
 */
public class SparkHistoryServerGetApplicationsResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testGettersSetters_WithValidData_WorksCorrectly() {
        // Arrange
        SparkHistoryServerGetApplicationsResponse response = new SparkHistoryServerGetApplicationsResponse();
        List<SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem> items = new ArrayList<>();
        SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem item = new SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem();
        item.setName("app-name");
        item.setId("app-id-123");
        items.add(item);
        response.setSparkHistoryServerGetApplicationsResponse(items);

        // Act & Assert
        Assert.assertNotNull(response.getSparkHistoryServerGetApplicationsResponse());
        Assert.assertEquals(response.getSparkHistoryServerGetApplicationsResponse().size(), 1);
        Assert.assertEquals(
                response.getSparkHistoryServerGetApplicationsResponse().get(0).getName(), "app-name");
        Assert.assertEquals(
                response.getSparkHistoryServerGetApplicationsResponse().get(0).getId(), "app-id-123");
    }

    @Test
    public void testGettersSetters_WithNestedAttempts_WorksCorrectly() {
        // Arrange
        SparkHistoryServerGetApplicationsResponse response = new SparkHistoryServerGetApplicationsResponse();
        List<SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem> items = new ArrayList<>();
        SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem item = new SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem();
        item.setName("app-name");
        item.setId("app-id");

        List<SparkHistoryServerGetApplicationsResponse.AttemptsItem> attempts = new ArrayList<>();
        SparkHistoryServerGetApplicationsResponse.AttemptsItem attempt = new SparkHistoryServerGetApplicationsResponse.AttemptsItem();
        attempt.setDuration(1000L);
        attempt.setLastUpdated("2021-01-01");
        attempt.setAppSparkVersion("3.1.2");
        attempt.setStartTime("2021-01-01T00:00:00Z");
        attempt.setSparkUser("user");
        attempt.setStartTimeEpoch(1609459200000L);
        attempt.setEndTime("2021-01-01T01:00:00Z");
        attempt.setCompleted(true);
        attempt.setLastUpdatedEpoch(1609459200000L);
        attempt.setEndTimeEpoch(1609462800000L);
        attempts.add(attempt);
        item.setAttempts(attempts);
        items.add(item);
        response.setSparkHistoryServerGetApplicationsResponse(items);

        // Act & Assert
        Assert.assertNotNull(response.getSparkHistoryServerGetApplicationsResponse());
        List<SparkHistoryServerGetApplicationsResponse.AttemptsItem> retrievedAttempts = response
                .getSparkHistoryServerGetApplicationsResponse().get(0).getAttempts();
        Assert.assertNotNull(retrievedAttempts);
        Assert.assertEquals(retrievedAttempts.size(), 1);
        Assert.assertEquals(retrievedAttempts.get(0).getDuration(), 1000L);
        Assert.assertEquals(retrievedAttempts.get(0).getAppSparkVersion(), "3.1.2");
        Assert.assertTrue(retrievedAttempts.get(0).isCompleted());
    }

    @Test
    public void testGettersSetters_WithNullValues_HandlesCorrectly() {
        // Arrange
        SparkHistoryServerGetApplicationsResponse response = new SparkHistoryServerGetApplicationsResponse();

        // Act
        response.setSparkHistoryServerGetApplicationsResponse(null);

        // Assert
        Assert.assertNull(response.getSparkHistoryServerGetApplicationsResponse());
    }

    @Test
    public void testEquals_WithSameData_ReturnsTrue() {
        // Arrange
        SparkHistoryServerGetApplicationsResponse response1 = new SparkHistoryServerGetApplicationsResponse();
        List<SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem> items1 = new ArrayList<>();
        SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem item1 = new SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem();
        item1.setName("app-name");
        item1.setId("app-id");
        items1.add(item1);
        response1.setSparkHistoryServerGetApplicationsResponse(items1);

        SparkHistoryServerGetApplicationsResponse response2 = new SparkHistoryServerGetApplicationsResponse();
        List<SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem> items2 = new ArrayList<>();
        SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem item2 = new SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem();
        item2.setName("app-name");
        item2.setId("app-id");
        items2.add(item2);
        response2.setSparkHistoryServerGetApplicationsResponse(items2);

        // Act & Assert
        Assert.assertEquals(response1, response2);
    }

    @Test
    public void testHashCode_WithSameData_ReturnsSameHash() {
        // Arrange
        SparkHistoryServerGetApplicationsResponse response1 = new SparkHistoryServerGetApplicationsResponse();
        List<SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem> items = new ArrayList<>();
        SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem item = new SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem();
        item.setName("app-name");
        item.setId("app-id");
        items.add(item);
        response1.setSparkHistoryServerGetApplicationsResponse(items);

        SparkHistoryServerGetApplicationsResponse response2 = new SparkHistoryServerGetApplicationsResponse();
        response2.setSparkHistoryServerGetApplicationsResponse(items);

        // Act & Assert
        Assert.assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    public void testToString_WithData_ReturnsNonEmptyString() {
        // Arrange
        SparkHistoryServerGetApplicationsResponse response = new SparkHistoryServerGetApplicationsResponse();
        List<SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem> items = new ArrayList<>();
        SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem item = new SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem();
        item.setName("app-name");
        items.add(item);
        response.setSparkHistoryServerGetApplicationsResponse(items);

        // Act
        String toString = response.toString();

        // Assert
        Assert.assertNotNull(toString);
        Assert.assertFalse(toString.isEmpty());
        Assert.assertTrue(toString.contains("SparkHistoryServerGetApplicationsResponse"));
    }

    @Test
    public void testSerialization_WithJackson_WorksCorrectly() throws Exception {
        // Arrange
        SparkHistoryServerGetApplicationsResponse response = new SparkHistoryServerGetApplicationsResponse();
        List<SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem> items = new ArrayList<>();
        SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem item = new SparkHistoryServerGetApplicationsResponse.SparkHistoryServerGetApplicationsResponseItem();
        item.setName("app-name");
        item.setId("app-id-123");
        items.add(item);
        response.setSparkHistoryServerGetApplicationsResponse(items);

        // Act
        String json = objectMapper.writeValueAsString(response);

        // Assert
        Assert.assertNotNull(json);
        Assert.assertTrue(json.contains("app-name"));
        Assert.assertTrue(json.contains("app-id-123"));
    }

    @Test
    public void testDeserialization_WithJackson_WorksCorrectly() throws Exception {
        // Arrange
        String json = "{\"sparkHistoryServerGetApplicationsResponse\":[{\"name\":\"app-name\",\"id\":\"app-id-123\",\"attempts\":[]}]}";

        // Act
        SparkHistoryServerGetApplicationsResponse response = objectMapper.readValue(json,
                SparkHistoryServerGetApplicationsResponse.class);

        // Assert
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getSparkHistoryServerGetApplicationsResponse());
        Assert.assertEquals(response.getSparkHistoryServerGetApplicationsResponse().size(), 1);
        Assert.assertEquals(
                response.getSparkHistoryServerGetApplicationsResponse().get(0).getName(), "app-name");
        Assert.assertEquals(
                response.getSparkHistoryServerGetApplicationsResponse().get(0).getId(), "app-id-123");
    }

    @Test
    public void testDeserialization_WithMissingFields_HandlesGracefully() throws Exception {
        // Arrange
        String json = "{\"sparkHistoryServerGetApplicationsResponse\":[]}";

        // Act
        SparkHistoryServerGetApplicationsResponse response = objectMapper.readValue(json,
                SparkHistoryServerGetApplicationsResponse.class);

        // Assert
        Assert.assertNotNull(response);
        Assert.assertNotNull(response.getSparkHistoryServerGetApplicationsResponse());
        Assert.assertEquals(response.getSparkHistoryServerGetApplicationsResponse().size(), 0);
    }

    @Test
    public void testDeserialization_WithNestedAttempts_WorksCorrectly() throws Exception {
        // Arrange
        String json = "{\"sparkHistoryServerGetApplicationsResponse\":[{\"name\":\"app-name\",\"id\":\"app-id\",\"attempts\":[{\"duration\":1000,\"lastUpdated\":\"2021-01-01\",\"appSparkVersion\":\"3.1.2\",\"completed\":true}]}]}";

        // Act
        SparkHistoryServerGetApplicationsResponse response = objectMapper.readValue(json,
                SparkHistoryServerGetApplicationsResponse.class);

        // Assert
        Assert.assertNotNull(response);
        List<SparkHistoryServerGetApplicationsResponse.AttemptsItem> attempts = response
                .getSparkHistoryServerGetApplicationsResponse().get(0).getAttempts();
        Assert.assertNotNull(attempts);
        Assert.assertEquals(attempts.size(), 1);
        Assert.assertEquals(attempts.get(0).getDuration(), 1000L);
        Assert.assertEquals(attempts.get(0).getAppSparkVersion(), "3.1.2");
        Assert.assertTrue(attempts.get(0).isCompleted());
    }
}
