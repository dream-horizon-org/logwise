package com.logwise.spark.services;

import com.logwise.spark.base.MockConfigHelper;
import com.logwise.spark.clients.KafkaManagerClient;
import com.logwise.spark.dto.response.TopicIdentitiesResponse;
import com.typesafe.config.Config;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/** Unit tests for KafkaManagerService. */
public class KafkaManagerServiceTest {

    private KafkaManagerClient mockKafkaManagerClient;
    private Config mockConfig;
    private KafkaManagerService kafkaManagerService;

    @BeforeMethod
    public void setUp() {
        mockKafkaManagerClient = Mockito.mock(KafkaManagerClient.class);
        mockConfig = MockConfigHelper.createMinimalSparkConfig();
        kafkaManagerService = new KafkaManagerService(mockKafkaManagerClient, mockConfig);
    }

    @Test
    public void testGetTopicIdentities_WithValidResponse_ReturnsTopicIdentities() {
        // Arrange
        TopicIdentitiesResponse response = new TopicIdentitiesResponse();
        List<TopicIdentitiesResponse.TopicIdentitiesItem> items = new ArrayList<>();
        TopicIdentitiesResponse.TopicIdentitiesItem item = new TopicIdentitiesResponse.TopicIdentitiesItem();
        item.setTopic("logs-test");
        item.setPartitions(3);
        item.setProducerRate("100.0");
        items.add(item);
        response.setTopicIdentities(items);

        Mockito.when(mockKafkaManagerClient.topicIdentities("test-cluster")).thenReturn(response);

        // Act
        TopicIdentitiesResponse result = kafkaManagerService.getTopicIdentities("test-cluster");

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getTopicIdentities().size(), 1);
        Mockito.verify(mockKafkaManagerClient).topicIdentities("test-cluster");
    }

    @Test
    public void testGetTopicIdentities_WithException_ReturnsNull() {
        // Arrange
        Mockito.when(mockKafkaManagerClient.topicIdentities("test-cluster"))
                .thenThrow(new RuntimeException("Connection error"));

        // Act
        TopicIdentitiesResponse result = kafkaManagerService.getTopicIdentities("test-cluster");

        // Assert
        Assert.assertNull(result);
        Mockito.verify(mockKafkaManagerClient).topicIdentities("test-cluster");
    }

    @Test
    public void testGetActiveLogsTopicPartitionCount_WithActiveTopics_ReturnsPartitionCount() {
        // Arrange
        TopicIdentitiesResponse response = new TopicIdentitiesResponse();
        List<TopicIdentitiesResponse.TopicIdentitiesItem> items = new ArrayList<>();

        TopicIdentitiesResponse.TopicIdentitiesItem item1 = new TopicIdentitiesResponse.TopicIdentitiesItem();
        item1.setTopic("logs-app1");
        item1.setPartitions(5);
        item1.setProducerRate("100.0");
        items.add(item1);

        TopicIdentitiesResponse.TopicIdentitiesItem item2 = new TopicIdentitiesResponse.TopicIdentitiesItem();
        item2.setTopic("logs-app2");
        item2.setPartitions(3);
        item2.setProducerRate("50.0");
        items.add(item2);

        response.setTopicIdentities(items);

        // Act
        Integer result = kafkaManagerService.getActiveLogsTopicPartitionCount(response, "logs.*");

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result, Integer.valueOf(8)); // 5 + 3
    }

    @Test
    public void testGetActiveLogsTopicPartitionCount_WithZeroProducerRate_UsesActiveTopicsRatio() {
        // Arrange
        TopicIdentitiesResponse response = new TopicIdentitiesResponse();
        List<TopicIdentitiesResponse.TopicIdentitiesItem> items = new ArrayList<>();

        TopicIdentitiesResponse.TopicIdentitiesItem item = new TopicIdentitiesResponse.TopicIdentitiesItem();
        item.setTopic("logs-app1");
        item.setPartitions(10);
        item.setProducerRate("0.0"); // Zero producer rate
        items.add(item);

        response.setTopicIdentities(items);

        // Act
        Integer result = kafkaManagerService.getActiveLogsTopicPartitionCount(response, "logs.*");

        // Assert
        // Should use activeTopicsRatio (0.5) * totalPartitions (10) = 5
        Assert.assertNotNull(result);
        Assert.assertEquals(result, Integer.valueOf(5));
    }

    @Test
    public void testGetActiveLogsTopicPartitionCount_WithNonMatchingPattern_ReturnsZero() {
        // Arrange
        TopicIdentitiesResponse response = new TopicIdentitiesResponse();
        List<TopicIdentitiesResponse.TopicIdentitiesItem> items = new ArrayList<>();

        TopicIdentitiesResponse.TopicIdentitiesItem item = new TopicIdentitiesResponse.TopicIdentitiesItem();
        item.setTopic("other-topic");
        item.setPartitions(5);
        item.setProducerRate("100.0");
        items.add(item);

        response.setTopicIdentities(items);

        // Act
        Integer result = kafkaManagerService.getActiveLogsTopicPartitionCount(response, "logs.*");

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result, Integer.valueOf(0));
    }

    @Test
    public void testGetActiveLogsTopicPartitionCount_WithException_ReturnsNull() {
        // Arrange
        TopicIdentitiesResponse response = new TopicIdentitiesResponse();
        response.setTopicIdentities(null); // This will cause NPE

        // Act
        Integer result = kafkaManagerService.getActiveLogsTopicPartitionCount(response, "logs.*");

        // Assert
        Assert.assertNull(result);
    }
}
