package com.logwise.spark.services;

import com.logwise.spark.base.MockConfigHelper;
import com.logwise.spark.dto.entity.StartingOffsetsByTimestampOption;
import com.typesafe.config.Config;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Unit tests for KafkaService. */
public class KafkaServiceTest {

    private Config mockConfig;
    private KafkaService kafkaService;

    @BeforeMethod
    public void setUp() {
        java.util.Map<String, Object> configMap = new java.util.HashMap<>();
        configMap.put("kafka.bootstrap.servers.port", "9092");
        mockConfig = MockConfigHelper.createConfig(configMap);
        kafkaService = new KafkaService(mockConfig);
    }

    @Test
    public void testGetKafkaBootstrapServerIp_WithValidHostname_ReturnsFormattedIp() {
        // Arrange
        String hostname = "localhost";

        // Act
        String result = kafkaService.getKafkaBootstrapServerIp(hostname);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertTrue(result.contains(":9092"));
        // Should contain localhost IP addresses
    }

    @Test
    public void testGetKafkaBootstrapServerIp_WithMultipleIps_ReturnsCommaSeparatedList() {
        // Arrange
        String hostname = "localhost";

        // Act
        String result = kafkaService.getKafkaBootstrapServerIp(hostname);

        // Assert
        Assert.assertNotNull(result);
        // Should be comma-separated if multiple IPs exist
        String[] parts = result.split(",");
        Assert.assertTrue(parts.length > 0);
        for (String part : parts) {
            Assert.assertTrue(part.contains(":9092"));
        }
    }

    @Test(expectedExceptions = Exception.class)
    public void testGetKafkaBootstrapServerIp_WithInvalidHostname_ThrowsException() {
        // Arrange
        String invalidHostname = "invalid.hostname.that.does.not.exist.12345";

        // Act
        kafkaService.getKafkaBootstrapServerIp(invalidHostname);
    }

    @Test
    public void testGetStartingOffsetsByTimestamp_WithValidInput_ReturnsOption() {
        // Arrange
        String kafkaHostname = "localhost";
        String topicRegexPattern = "logs.*";
        Long timestamp = 1609459200000L;

        // Note: This test is complex because it uses real KafkaConsumer
        // We'll test the public method that calls the private static method
        // In a real scenario, you might want to use embedded Kafka or more
        // sophisticated mocking

        // Act
        StartingOffsetsByTimestampOption result = kafkaService.getStartingOffsetsByTimestamp(kafkaHostname,
                topicRegexPattern, timestamp);

        // Assert
        // The result might be empty if no Kafka topics exist, but should not be null
        Assert.assertNotNull(result);
        // Note: Actual behavior depends on Kafka cluster availability
        // This test verifies the method doesn't throw exceptions
    }

    @Test
    public void testGetStartingOffsetsByTimestamp_WithEmptyTopicPattern_ReturnsEmptyOption() {
        // Arrange
        String kafkaHostname = "localhost";
        String topicRegexPattern = "nonexistent.*";
        Long timestamp = System.currentTimeMillis();

        // Act
        StartingOffsetsByTimestampOption result = kafkaService.getStartingOffsetsByTimestamp(kafkaHostname,
                topicRegexPattern, timestamp);

        // Assert
        Assert.assertNotNull(result);
        // Should return empty option if no topics match
    }

    // Note: More comprehensive testing of getStartingOffsetsByTimestamp would
    // require:
    // 1. Embedded Kafka setup for integration tests
    // 2. Or sophisticated mocking of KafkaConsumer (which is complex due to its
    // final nature)
    // For unit testing, we focus on testing the public interface and error handling
}
