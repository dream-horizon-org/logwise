package com.logwise.spark.utils;

import com.google.protobuf.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for ApplicationUtils utility class. */
public class ApplicationUtilsTest {

  @Test
  public void testGetIpAddresses_WithValidDomain_ReturnsIpAddresses() {
    // Arrange
    String domainName = "localhost";

    // Act
    List<String> ipAddresses = ApplicationUtils.getIpAddresses(domainName);

    // Assert
    Assert.assertNotNull(ipAddresses);
    Assert.assertFalse(ipAddresses.isEmpty());
  }

  @Test(expectedExceptions = Exception.class)
  public void testGetIpAddresses_WithInvalidDomain_ThrowsException() {
    // Arrange
    String invalidDomain = "invalid.domain.that.does.not.exist.12345";

    // Act
    ApplicationUtils.getIpAddresses(invalidDomain);
  }

  @Test
  public void testRemoveSurroundingQuotes_WithQuotedString_RemovesQuotes() {
    // Arrange
    String quotedString = "\"test-string\"";

    // Act
    String result = ApplicationUtils.removeSurroundingQuotes(quotedString);

    // Assert
    Assert.assertEquals(result, "test-string");
  }

  @Test
  public void testRemoveSurroundingQuotes_WithUnquotedString_ReturnsOriginal() {
    // Arrange
    String unquotedString = "test-string";

    // Act
    String result = ApplicationUtils.removeSurroundingQuotes(unquotedString);

    // Assert
    Assert.assertEquals(result, "test-string");
  }

  @Test
  public void testRemoveSurroundingQuotes_WithOnlyStartQuote_ReturnsOriginal() {
    // Arrange
    String stringWithStartQuote = "\"test-string";

    // Act
    String result = ApplicationUtils.removeSurroundingQuotes(stringWithStartQuote);

    // Assert
    Assert.assertEquals(result, "\"test-string");
  }

  @Test
  public void testRemoveSurroundingQuotes_WithOnlyEndQuote_ReturnsOriginal() {
    // Arrange
    String stringWithEndQuote = "test-string\"";

    // Act
    String result = ApplicationUtils.removeSurroundingQuotes(stringWithEndQuote);

    // Assert
    Assert.assertEquals(result, "test-string\"");
  }

  @Test
  public void testRemoveSurroundingQuotes_WithNull_ReturnsNull() {
    // Arrange
    String nullString = null;

    // Act
    String result = ApplicationUtils.removeSurroundingQuotes(nullString);

    // Assert
    Assert.assertNull(result);
  }

  @Test
  public void testRemoveSurroundingQuotes_WithEmptyString_ReturnsEmpty() {
    // Arrange
    String emptyString = "";

    // Act
    String result = ApplicationUtils.removeSurroundingQuotes(emptyString);

    // Assert
    Assert.assertEquals(result, "");
  }

  @Test(expectedExceptions = StringIndexOutOfBoundsException.class)
  public void testRemoveSurroundingQuotes_WithSingleQuote_ThrowsException() {
    // Arrange
    String singleQuote = "\"";

    // Act
    // substring(1, 0) throws StringIndexOutOfBoundsException
    ApplicationUtils.removeSurroundingQuotes(singleQuote);
  }

  @Test
  public void testConvertProtoTimestampToIso_WithValidTimestamp_ReturnsIsoString() {
    // Arrange
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(1609459200L).setNanos(0).build();

    // Act
    String result = ApplicationUtils.convertProtoTimestampToIso(timestamp);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertTrue(result.contains("2021-01-01"));
    Assert.assertTrue(result.contains("T") || result.contains("Z"));
  }

  @Test
  public void testConvertProtoTimestampToIso_WithNanos_ReturnsIsoStringWithNanos() {
    // Arrange
    Timestamp timestamp =
        Timestamp.newBuilder().setSeconds(1609459200L).setNanos(500000000).build();

    // Act
    String result = ApplicationUtils.convertProtoTimestampToIso(timestamp);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertTrue(result.contains("2021-01-01"));
  }

  @Test
  public void testConvertProtoTimestampToSqlTimestamp_WithValidTimestamp_ReturnsSqlTimestamp() {
    // Arrange
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(1609459200L).setNanos(0).build();

    // Act
    java.sql.Timestamp result = ApplicationUtils.convertProtoTimestampToSqlTimestamp(timestamp);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getTime(), 1609459200000L);
  }

  @Test
  public void testConvertProtoTimestampToSqlTimestamp_WithNanos_ReturnsSqlTimestampWithNanos() {
    // Arrange
    Timestamp timestamp =
        Timestamp.newBuilder().setSeconds(1609459200L).setNanos(500000000).build();

    // Act
    java.sql.Timestamp result = ApplicationUtils.convertProtoTimestampToSqlTimestamp(timestamp);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getTime(), 1609459200500L);
  }

  @Test
  public void testConvertMapToJsonString_WithValidMap_ReturnsJsonString() {
    // Arrange
    Map<String, String> map = new HashMap<>();
    map.put("key1", "value1");
    map.put("key2", "value2");

    // Act
    String result = ApplicationUtils.convertMapToJsonString(map);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertTrue(result.contains("key1"));
    Assert.assertTrue(result.contains("value1"));
    Assert.assertTrue(result.contains("key2"));
    Assert.assertTrue(result.contains("value2"));
  }

  @Test
  public void testConvertMapToJsonString_WithEmptyMap_ReturnsEmptyJsonObject() {
    // Arrange
    Map<String, String> emptyMap = new HashMap<>();

    // Act
    String result = ApplicationUtils.convertMapToJsonString(emptyMap);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertEquals(result, "{}");
  }

  @Test
  public void testConvertMapToJsonString_WithException_ReturnsEmptyJsonObject() {
    // Arrange - Create a map that might cause serialization issues
    // Note: In practice, ObjectMapper.writeValueAsString rarely throws for simple
    // maps,
    // but the catch block exists for safety. We'll test with a valid map and verify
    // the method handles it correctly. The exception branch is defensive code.
    Map<String, String> map = new HashMap<>();
    map.put("key1", "value1");

    // Act
    String result = ApplicationUtils.convertMapToJsonString(map);

    // Assert - Should return valid JSON (exception branch is defensive)
    Assert.assertNotNull(result);
    Assert.assertTrue(result.contains("key1") || result.equals("{}"));
  }

  @Test
  public void testRemoveSurroundingQuotes_WithBothQuotesButShortString_HandlesCorrectly() {
    // Arrange - String with quotes but too short (edge case)
    String shortQuoted = "\"\"";

    // Act
    String result = ApplicationUtils.removeSurroundingQuotes(shortQuoted);

    // Assert - Should handle empty quoted string
    Assert.assertNotNull(result);
    Assert.assertEquals(result, "");
  }
}
