package com.logwise.spark.services;

import static org.testng.Assert.*;

import com.logwise.spark.base.MockConfigHelper;
import com.typesafe.config.Config;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for KafkaService. */
public class KafkaServiceTest {

  // Test Constants
  private static final String KAFKA_PORT = "9092";
  private static final String LOCALHOST = "localhost";
  private static final String INVALID_HOSTNAME = "invalid.hostname.that.does.not.exist.12345";

  private Config mockConfig;
  private KafkaService kafkaService;

  @BeforeMethod
  public void setUp() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("kafka.bootstrap.servers.port", KAFKA_PORT);
    mockConfig = MockConfigHelper.createConfig(configMap);

    // Create KafkaService
    kafkaService = new KafkaService(mockConfig, true);
  }

  @Test
  public void testGetKafkaBootstrapServerIp_WithValidHostname_ReturnsFormattedIp() {
    // Act
    String result = kafkaService.getKafkaBootstrapServerIp(LOCALHOST);

    // Assert
    assertNotNull(result, "Result should not be null");
    assertTrue(result.contains(":" + KAFKA_PORT), "Result should contain port: " + KAFKA_PORT);
  }

  @Test
  public void testGetKafkaBootstrapServerIp_WithMultipleIps_ReturnsCommaSeparatedList() {
    // Act
    String result = kafkaService.getKafkaBootstrapServerIp(LOCALHOST);

    // Assert
    assertNotNull(result, "Result should not be null");
    String[] parts = result.split(",");
    assertTrue(parts.length > 0, "Should return at least one IP address");

    for (String part : parts) {
      assertFalse(part.trim().isEmpty(), "Each part should not be empty");
      assertTrue(part.endsWith(":" + KAFKA_PORT), "Each part should end with port: " + KAFKA_PORT);
      // Verify format is valid (handles both IPv4 and IPv6)
      String ipPart = part.substring(0, part.lastIndexOf(":"));
      assertFalse(ipPart.isEmpty(), "IP address part should not be empty");
    }
  }

  @Test(expectedExceptions = UnknownHostException.class)
  public void testGetKafkaBootstrapServerIp_WithInvalidHostname_ThrowsUnknownHostException()
      throws UnknownHostException {
    // Act - should throw UnknownHostException
    kafkaService.getKafkaBootstrapServerIp(INVALID_HOSTNAME);
  }
}
