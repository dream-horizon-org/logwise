package com.logwise.spark.constants;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

/** Unit tests for StreamName enum. */
public class StreamNameTest {

  @Test
  public void testFromValue_WithValidValue_ReturnsStreamName() {
    // Act
    StreamName streamName = StreamName.fromValue("application-logs-stream-to-s3");

    // Assert
    assertNotNull(streamName);
    assertEquals(streamName, StreamName.APPLICATION_LOGS_STREAM_TO_S3);
    assertEquals(streamName.getValue(), "application-logs-stream-to-s3");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromValue_WithInvalidValue_ThrowsException() {
    // Act - should throw IllegalArgumentException
    StreamName.fromValue("invalid-stream-name");
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromValue_WithNullValue_ThrowsException() {
    // Act - should throw IllegalArgumentException
    StreamName.fromValue(null);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testFromValue_WithEmptyValue_ThrowsException() {
    // Act - should throw IllegalArgumentException
    StreamName.fromValue("");
  }

  @Test
  public void testGetValue_ReturnsCorrectValue() {
    // Act
    String value = StreamName.APPLICATION_LOGS_STREAM_TO_S3.getValue();

    // Assert
    assertNotNull(value);
    assertEquals(value, "application-logs-stream-to-s3");
  }

  @Test
  public void testFromValue_WithWhitespace_ThrowsException() {
    try {
      StreamName.fromValue(" application-logs-stream-to-s3 ");
      fail("Should throw IllegalArgumentException for whitespace");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Invalid value"));
    }
  }

  @Test
  public void testFromValue_WithPartialMatch_ThrowsException() {
    try {
      StreamName.fromValue("application-logs");
      fail("Should throw IllegalArgumentException for partial match");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("Invalid value"));
    }
  }

  @Test
  public void testFromValue_WithExactMatch_ReturnsCorrectEnum() {
    StreamName result = StreamName.fromValue("application-logs-stream-to-s3");
    assertEquals(result, StreamName.APPLICATION_LOGS_STREAM_TO_S3);
    assertEquals(result.getValue(), "application-logs-stream-to-s3");
  }

  @Test
  public void testValues_ReturnsAllStreamNames() {
    StreamName[] values = StreamName.values();
    assertNotNull(values);
    assertEquals(values.length, 1);
    assertEquals(values[0], StreamName.APPLICATION_LOGS_STREAM_TO_S3);
  }

  @Test
  public void testValueOf_WithValidName_ReturnsStreamName() {
    StreamName streamName = StreamName.valueOf("APPLICATION_LOGS_STREAM_TO_S3");
    assertNotNull(streamName);
    assertEquals(streamName, StreamName.APPLICATION_LOGS_STREAM_TO_S3);
  }
}
