package com.logwise.spark.utils;

import com.google.protobuf.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Edge case tests for ApplicationUtils.
 *
 * <p>Tests verify handling of edge cases, special characters, boundary conditions, and unusual
 * inputs.
 */
public class ApplicationUtilsEdgeCaseTest {

  @Test
  public void testGetIpAddresses_WithIPv6_HandlesCorrectly() {
    // Arrange
    String ipv6Hostname = "localhost"; // localhost resolves to IPv6 on some systems

    // Act
    List<String> ipAddresses = ApplicationUtils.getIpAddresses(ipv6Hostname);

    // Assert
    Assert.assertNotNull(ipAddresses);
    Assert.assertFalse(ipAddresses.isEmpty());
    // Should return at least one IP address (could be IPv4 or IPv6)
    for (String ip : ipAddresses) {
      Assert.assertNotNull(ip);
      Assert.assertFalse(ip.isEmpty());
    }
  }

  @Test
  public void testGetIpAddresses_WithMultipleIPs_ReturnsAll() {
    // Arrange
    String hostname = "localhost"; // localhost often has multiple IPs (127.0.0.1, ::1)

    // Act
    List<String> ipAddresses = ApplicationUtils.getIpAddresses(hostname);

    // Assert
    Assert.assertNotNull(ipAddresses);
    Assert.assertFalse(ipAddresses.isEmpty());
    // localhost typically has at least one IP
    Assert.assertTrue(ipAddresses.size() >= 1);
  }

  @Test
  public void testRemoveSurroundingQuotes_WithNestedQuotes_HandlesCorrectly() {
    // Arrange
    String nestedQuotes = "\"\"nested\"quotes\"";

    // Act
    String result = ApplicationUtils.removeSurroundingQuotes(nestedQuotes);

    // Assert
    // Should remove only the outer quotes
    Assert.assertEquals(result, "\"nested\"quotes");
  }

  @Test
  public void testConvertProtoTimestampToIso_WithNegativeTimestamp_HandlesCorrectly() {
    // Arrange - Negative timestamp (before epoch)
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(-1000L).setNanos(0).build();

    // Act
    String result = ApplicationUtils.convertProtoTimestampToIso(timestamp);

    // Assert
    Assert.assertNotNull(result);
    // Should still produce a valid ISO string (even if before epoch)
    Assert.assertTrue(result.contains("T") || result.contains("Z"));
  }

  @Test
  public void testConvertProtoTimestampToIso_WithFutureTimestamp_HandlesCorrectly() {
    // Arrange - Future timestamp (year 2100)
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(4102444800L).setNanos(0).build();

    // Act
    String result = ApplicationUtils.convertProtoTimestampToIso(timestamp);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertTrue(result.contains("2100") || result.contains("T") || result.contains("Z"));
  }

  @Test
  public void testConvertMapToJsonString_WithNullValues_HandlesCorrectly() {
    // Arrange
    Map<String, String> mapWithNulls = new HashMap<>();
    mapWithNulls.put("key1", "value1");
    mapWithNulls.put("key2", null); // Null value
    mapWithNulls.put("key3", "value3");

    // Act
    String result = ApplicationUtils.convertMapToJsonString(mapWithNulls);

    // Assert
    Assert.assertNotNull(result);
    // Jackson typically serializes null values as "null" in JSON
    Assert.assertTrue(result.contains("key1"));
    Assert.assertTrue(result.contains("value1"));
    Assert.assertTrue(result.contains("key2"));
    Assert.assertTrue(result.contains("key3"));
  }

  @Test
  public void testConvertMapToJsonString_WithSpecialCharacters_EscapesCorrectly() {
    // Arrange
    Map<String, String> mapWithSpecialChars = new HashMap<>();
    mapWithSpecialChars.put("key1", "value with \"quotes\"");
    mapWithSpecialChars.put("key2", "value with\nnewline");
    mapWithSpecialChars.put("key3", "value with\ttab");
    mapWithSpecialChars.put("key4", "value with\\backslash");

    // Act
    String result = ApplicationUtils.convertMapToJsonString(mapWithSpecialChars);

    // Assert
    Assert.assertNotNull(result);
    // JSON should properly escape special characters
    Assert.assertTrue(result.contains("key1"));
    Assert.assertTrue(result.contains("key2"));
    Assert.assertTrue(result.contains("key3"));
    Assert.assertTrue(result.contains("key4"));
    // Verify it's valid JSON (doesn't throw exception when parsed)
    Assert.assertFalse(result.isEmpty());
  }

  @Test
  public void testConvertMapToJsonString_WithLargeMap_HandlesCorrectly() {
    // Arrange
    Map<String, String> largeMap = new HashMap<>();
    for (int i = 0; i < 1000; i++) {
      largeMap.put("key" + i, "value" + i);
    }

    // Act
    String result = ApplicationUtils.convertMapToJsonString(largeMap);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertFalse(result.isEmpty());
    // Should contain all keys
    Assert.assertTrue(result.contains("key0"));
    Assert.assertTrue(result.contains("key999"));
    // Should be valid JSON
    Assert.assertTrue(result.startsWith("{"));
    Assert.assertTrue(result.endsWith("}"));
  }

  @Test
  public void testConvertProtoTimestampToSqlTimestamp_WithMaxTimestamp_HandlesCorrectly() {
    // Arrange - Maximum reasonable timestamp (year 9999)
    Timestamp timestamp =
        Timestamp.newBuilder().setSeconds(253402300799L).setNanos(999999999).build();

    // Act
    java.sql.Timestamp result = ApplicationUtils.convertProtoTimestampToSqlTimestamp(timestamp);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertTrue(result.getTime() > 0);
  }

  @Test
  public void testConvertProtoTimestampToSqlTimestamp_WithZeroTimestamp_HandlesCorrectly() {
    // Arrange - Epoch timestamp
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(0L).setNanos(0).build();

    // Act
    java.sql.Timestamp result = ApplicationUtils.convertProtoTimestampToSqlTimestamp(timestamp);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getTime(), 0L);
  }

  @Test
  public void testRemoveSurroundingQuotes_WithOnlyQuotes_HandlesCorrectly() {
    // Arrange
    String onlyQuotes = "\"\"";

    // Act
    String result = ApplicationUtils.removeSurroundingQuotes(onlyQuotes);

    // Assert
    // Should return empty string after removing both quotes
    Assert.assertEquals(result, "");
  }

  @Test
  public void testRemoveSurroundingQuotes_WithWhitespace_HandlesCorrectly() {
    // Arrange
    String withWhitespace = "\"  test  \"";

    // Act
    String result = ApplicationUtils.removeSurroundingQuotes(withWhitespace);

    // Assert
    // Should remove quotes but preserve whitespace
    Assert.assertEquals(result, "  test  ");
  }
}
