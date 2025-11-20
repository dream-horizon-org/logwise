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
}
