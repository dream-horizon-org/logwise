package com.logwise.spark.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for VectorLogs Protobuf serialization and deserialization.
 *
 * <p>Tests verify that Protobuf messages can be serialized and deserialized correctly, preserving
 * all data including timestamps and special characters.
 */
public class VectorLogsSerializationTest {

  @Test
  public void testSerializeDeserialize_WithAllFields_ReturnsIdenticalMessage()
      throws InvalidProtocolBufferException {
    // Arrange
    Timestamp timestamp =
        Timestamp.newBuilder().setSeconds(1609459200L).setNanos(500000000).build();

    VectorLogs original =
        VectorLogs.newBuilder()
            .setMessage("Test log message")
            .setTimestamp(timestamp)
            .setEnvironmentName("production")
            .setServiceName("api-service")
            .setComponentType("api-container")
            .setLogLevel("info")
            .build();

    // Act
    byte[] serialized = original.toByteArray();
    VectorLogs deserialized = VectorLogs.parseFrom(serialized);

    // Assert
    Assert.assertEquals(deserialized.getMessage(), original.getMessage());
    Assert.assertEquals(deserialized.getTimestamp(), original.getTimestamp());
    Assert.assertEquals(deserialized.getEnvironmentName(), original.getEnvironmentName());
    Assert.assertEquals(deserialized.getServiceName(), original.getServiceName());
    Assert.assertEquals(deserialized.getComponentType(), original.getComponentType());
    Assert.assertEquals(deserialized.getLogLevel(), original.getLogLevel());
  }

  @Test
  public void testSerializeDeserialize_WithMinimalFields_ReturnsValidMessage()
      throws InvalidProtocolBufferException {
    // Arrange
    VectorLogs original = VectorLogs.newBuilder().setMessage("Minimal log").build();

    // Act
    byte[] serialized = original.toByteArray();
    VectorLogs deserialized = VectorLogs.parseFrom(serialized);

    // Assert
    Assert.assertEquals(deserialized.getMessage(), "Minimal log");
    Assert.assertFalse(deserialized.hasTimestamp());
    Assert.assertEquals(deserialized.getEnvironmentName(), "");
    Assert.assertEquals(deserialized.getServiceName(), "");
    Assert.assertEquals(deserialized.getComponentType(), "");
    Assert.assertEquals(deserialized.getLogLevel(), "");
  }

  @Test
  public void testSerializeDeserialize_WithTimestamp_PreservesTimestamp()
      throws InvalidProtocolBufferException {
    // Arrange
    Timestamp timestamp =
        Timestamp.newBuilder().setSeconds(1609459200L).setNanos(500000000).build();
    VectorLogs original =
        VectorLogs.newBuilder().setMessage("Test").setTimestamp(timestamp).build();

    // Act
    byte[] serialized = original.toByteArray();
    VectorLogs deserialized = VectorLogs.parseFrom(serialized);

    // Assert
    Assert.assertTrue(deserialized.hasTimestamp());
    Assert.assertEquals(deserialized.getTimestamp().getSeconds(), 1609459200L);
    Assert.assertEquals(deserialized.getTimestamp().getNanos(), 500000000);
  }

  @Test
  public void testSerializeDeserialize_WithSpecialCharacters_HandlesCorrectly()
      throws InvalidProtocolBufferException {
    // Arrange
    String messageWithSpecialChars = "Log with special chars: <>&\"'\\n\\t";
    VectorLogs original = VectorLogs.newBuilder().setMessage(messageWithSpecialChars).build();

    // Act
    byte[] serialized = original.toByteArray();
    VectorLogs deserialized = VectorLogs.parseFrom(serialized);

    // Assert
    Assert.assertEquals(deserialized.getMessage(), messageWithSpecialChars);
  }

  @Test
  public void testSerializeDeserialize_WithUnicodeCharacters_HandlesCorrectly()
      throws InvalidProtocolBufferException {
    // Arrange
    String unicodeMessage = "Unicode test: 中文 العربية русский 🚀 🎉";
    VectorLogs original = VectorLogs.newBuilder().setMessage(unicodeMessage).build();

    // Act
    byte[] serialized = original.toByteArray();
    VectorLogs deserialized = VectorLogs.parseFrom(serialized);

    // Assert
    Assert.assertEquals(deserialized.getMessage(), unicodeMessage);
  }

  @Test
  public void testSerializeDeserialize_WithEmptyStrings_HandlesCorrectly()
      throws InvalidProtocolBufferException {
    // Arrange
    VectorLogs original =
        VectorLogs.newBuilder()
            .setMessage("")
            .setEnvironmentName("")
            .setServiceName("")
            .setComponentType("")
            .setLogLevel("")
            .build();

    // Act
    byte[] serialized = original.toByteArray();
    VectorLogs deserialized = VectorLogs.parseFrom(serialized);

    // Assert
    Assert.assertEquals(deserialized.getMessage(), "");
    Assert.assertEquals(deserialized.getEnvironmentName(), "");
    Assert.assertEquals(deserialized.getServiceName(), "");
    Assert.assertEquals(deserialized.getComponentType(), "");
    Assert.assertEquals(deserialized.getLogLevel(), "");
  }

  @Test
  public void testSerializeDeserialize_WithOptionalTimestamp_HandlesCorrectly()
      throws InvalidProtocolBufferException {
    // Arrange - Optional timestamp field is not set
    VectorLogs original = VectorLogs.newBuilder().setMessage("Test").build();

    // Act
    byte[] serialized = original.toByteArray();
    VectorLogs deserialized = VectorLogs.parseFrom(serialized);

    // Assert
    Assert.assertFalse(deserialized.hasTimestamp());
    // getTimestamp() should return default instance when not set
    Assert.assertNotNull(deserialized.getTimestamp());
  }

  @Test
  public void testSerializeDeserialize_WithLargeMessage_HandlesCorrectly()
      throws InvalidProtocolBufferException {
    // Arrange
    StringBuilder largeMessage = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      largeMessage.append("This is a large log message. ");
    }
    VectorLogs original = VectorLogs.newBuilder().setMessage(largeMessage.toString()).build();

    // Act
    byte[] serialized = original.toByteArray();
    VectorLogs deserialized = VectorLogs.parseFrom(serialized);

    // Assert
    Assert.assertEquals(deserialized.getMessage(), largeMessage.toString());
    Assert.assertTrue(deserialized.getMessage().length() > 10000);
  }

  @Test(expectedExceptions = InvalidProtocolBufferException.class)
  public void testDeserialize_WithInvalidBytes_ThrowsException()
      throws InvalidProtocolBufferException {
    // Arrange
    byte[] invalidBytes = new byte[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};

    // Act - should throw InvalidProtocolBufferException
    VectorLogs.parseFrom(invalidBytes);
  }

  @Test
  public void testSerialize_WithMaxFieldLength_HandlesCorrectly()
      throws InvalidProtocolBufferException {
    // Arrange - Create a message with very long string (but within reasonable
    // limits)
    StringBuilder longString = new StringBuilder();
    for (int i = 0; i < 10000; i++) {
      longString.append("A");
    }
    VectorLogs original = VectorLogs.newBuilder().setMessage(longString.toString()).build();

    // Act
    byte[] serialized = original.toByteArray();
    VectorLogs deserialized = VectorLogs.parseFrom(serialized);

    // Assert
    Assert.assertEquals(deserialized.getMessage(), longString.toString());
    Assert.assertEquals(deserialized.getMessage().length(), 10000);
  }

  @Test
  public void testRoundTrip_WithAllFields_PreservesStructure()
      throws InvalidProtocolBufferException {
    // Arrange
    Timestamp timestamp =
        Timestamp.newBuilder().setSeconds(1609459200L).setNanos(500000000).build();
    VectorLogs original =
        VectorLogs.newBuilder()
            .setMessage("Test message")
            .setServiceName("test-service")
            .setEnvironmentName("test-env")
            .setComponentType("test-component")
            .setLogLevel("debug")
            .setTimestamp(timestamp)
            .build();

    // Act
    byte[] serialized = original.toByteArray();
    VectorLogs deserialized = VectorLogs.parseFrom(serialized);

    // Assert
    Assert.assertEquals(deserialized.getMessage(), "Test message");
    Assert.assertEquals(deserialized.getServiceName(), "test-service");
    Assert.assertEquals(deserialized.getEnvironmentName(), "test-env");
    Assert.assertEquals(deserialized.getComponentType(), "test-component");
    Assert.assertEquals(deserialized.getLogLevel(), "debug");
    Assert.assertTrue(deserialized.hasTimestamp());
    Assert.assertEquals(deserialized.getTimestamp().getSeconds(), timestamp.getSeconds());
    Assert.assertEquals(deserialized.getTimestamp().getNanos(), timestamp.getNanos());
  }
}
