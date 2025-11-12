package com.logwise.spark.protobuf;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for VectorLogs Protobuf serialization and deserialization.
 *
 * <p>
 * Tests verify that Protobuf messages can be serialized and deserialized
 * correctly, preserving
 * all data including maps, timestamps, and special characters.
 */
public class VectorLogsSerializationTest {

    @Test
    public void testSerializeDeserialize_WithAllFields_ReturnsIdenticalMessage()
            throws InvalidProtocolBufferException {
        // Arrange
        Timestamp timestamp = Timestamp.newBuilder().setSeconds(1609459200L).setNanos(500000000).build();
        Map<String, String> ddtags = new HashMap<>();
        ddtags.put("env", "prod");
        ddtags.put("service", "api");
        Map<String, String> extra = new HashMap<>();
        extra.put("trace_id", "12345");
        extra.put("span_id", "67890");

        VectorLogs original = VectorLogs.newBuilder()
                .setMessage("Test log message")
                .putAllDdtags(ddtags)
                .setTimestamp(timestamp)
                .setEnv("production")
                .setServiceName("api-service")
                .setComponentName("api-container")
                .setHostname("host-123")
                .setDdsource("vector")
                .setSourceType("application")
                .setStatus("info")
                .putAllExtra(extra)
                .build();

        // Act
        byte[] serialized = original.toByteArray();
        VectorLogs deserialized = VectorLogs.parseFrom(serialized);

        // Assert
        Assert.assertEquals(deserialized.getMessage(), original.getMessage());
        Assert.assertEquals(deserialized.getDdtagsMap(), original.getDdtagsMap());
        Assert.assertEquals(deserialized.getTimestamp(), original.getTimestamp());
        Assert.assertEquals(deserialized.getEnv(), original.getEnv());
        Assert.assertEquals(deserialized.getServiceName(), original.getServiceName());
        Assert.assertEquals(deserialized.getComponentName(), original.getComponentName());
        Assert.assertEquals(deserialized.getHostname(), original.getHostname());
        Assert.assertEquals(deserialized.getDdsource(), original.getDdsource());
        Assert.assertEquals(deserialized.getSourceType(), original.getSourceType());
        Assert.assertEquals(deserialized.getStatus(), original.getStatus());
        Assert.assertEquals(deserialized.getExtraMap(), original.getExtraMap());
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
        Assert.assertTrue(deserialized.getDdtagsMap().isEmpty());
        Assert.assertFalse(deserialized.hasTimestamp());
        Assert.assertEquals(deserialized.getEnv(), "");
        Assert.assertEquals(deserialized.getServiceName(), "");
    }

    @Test
    public void testSerializeDeserialize_WithDdtagsMap_PreservesMapStructure()
            throws InvalidProtocolBufferException {
        // Arrange
        Map<String, String> ddtags = new HashMap<>();
        ddtags.put("key1", "value1");
        ddtags.put("key2", "value2");
        ddtags.put("key3", "value3");

        VectorLogs original = VectorLogs.newBuilder().setMessage("Test").putAllDdtags(ddtags).build();

        // Act
        byte[] serialized = original.toByteArray();
        VectorLogs deserialized = VectorLogs.parseFrom(serialized);

        // Assert
        Assert.assertEquals(deserialized.getDdtagsCount(), 3);
        Assert.assertEquals(deserialized.getDdtagsMap(), ddtags);
        Assert.assertEquals(deserialized.getDdtagsOrDefault("key1", "default"), "value1");
        Assert.assertEquals(deserialized.getDdtagsOrDefault("key2", "default"), "value2");
        Assert.assertEquals(deserialized.getDdtagsOrDefault("key3", "default"), "value3");
    }

    @Test
    public void testSerializeDeserialize_WithExtraMap_PreservesMapStructure()
            throws InvalidProtocolBufferException {
        // Arrange
        Map<String, String> extra = new HashMap<>();
        extra.put("custom_field_1", "custom_value_1");
        extra.put("custom_field_2", "custom_value_2");

        VectorLogs original = VectorLogs.newBuilder().setMessage("Test").putAllExtra(extra).build();

        // Act
        byte[] serialized = original.toByteArray();
        VectorLogs deserialized = VectorLogs.parseFrom(serialized);

        // Assert
        Assert.assertEquals(deserialized.getExtraCount(), 2);
        Assert.assertEquals(deserialized.getExtraMap(), extra);
        Assert.assertEquals(deserialized.getExtraOrDefault("custom_field_1", "default"), "custom_value_1");
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
        VectorLogs original = VectorLogs.newBuilder()
                .setMessage("")
                .setEnv("")
                .setServiceName("")
                .setComponentName("")
                .build();

        // Act
        byte[] serialized = original.toByteArray();
        VectorLogs deserialized = VectorLogs.parseFrom(serialized);

        // Assert
        Assert.assertEquals(deserialized.getMessage(), "");
        Assert.assertEquals(deserialized.getEnv(), "");
        Assert.assertEquals(deserialized.getServiceName(), "");
    }

    @Test
    public void testSerializeDeserialize_WithNullOptionalFields_HandlesCorrectly()
            throws InvalidProtocolBufferException {
        // Arrange - Optional fields (ddsource, sourceType, status) are not set
        VectorLogs original = VectorLogs.newBuilder().setMessage("Test").build();

        // Act
        byte[] serialized = original.toByteArray();
        VectorLogs deserialized = VectorLogs.parseFrom(serialized);

        // Assert
        Assert.assertFalse(deserialized.hasDdsource());
        Assert.assertFalse(deserialized.hasSourceType());
        Assert.assertFalse(deserialized.hasStatus());
        Assert.assertEquals(deserialized.getDdsource(), "");
        Assert.assertEquals(deserialized.getSourceType(), "");
        Assert.assertEquals(deserialized.getStatus(), "");
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
        byte[] invalidBytes = new byte[] { 0x00, 0x01, 0x02, 0x03, 0x04, 0x05 };

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
    public void testRoundTrip_WithNestedMaps_PreservesStructure()
            throws InvalidProtocolBufferException {
        // Arrange
        Map<String, String> ddtags = new HashMap<>();
        ddtags.put("nested.key1", "value1");
        ddtags.put("nested.key2", "value2");
        Map<String, String> extra = new HashMap<>();
        extra.put("metadata.trace", "trace-123");
        extra.put("metadata.span", "span-456");

        VectorLogs original = VectorLogs.newBuilder()
                .setMessage("Test")
                .putAllDdtags(ddtags)
                .putAllExtra(extra)
                .build();

        // Act
        byte[] serialized = original.toByteArray();
        VectorLogs deserialized = VectorLogs.parseFrom(serialized);

        // Assert
        Assert.assertEquals(deserialized.getDdtagsMap(), ddtags);
        Assert.assertEquals(deserialized.getExtraMap(), extra);
        Assert.assertEquals(deserialized.getDdtagsOrDefault("nested.key1", ""), "value1");
        Assert.assertEquals(deserialized.getExtraOrDefault("metadata.trace", ""), "trace-123");
    }
}
