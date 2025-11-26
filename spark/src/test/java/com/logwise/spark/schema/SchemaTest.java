package com.logwise.spark.schema;

import static org.testng.Assert.*;

import com.logwise.spark.constants.Constants;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.testng.annotations.Test;

/**
 * Unit tests for Schema utility class.
 *
 * <p>Tests verify that the schema definitions are correct and contain all required fields.
 */
public class SchemaTest {

  @Test
  public void testGetVectorApplicationLogsSchema_ReturnsNonNull() {
    // Act
    StructType schema = Schema.getVectorApplicationLogsSchema();

    // Assert
    assertNotNull(schema, "Schema should not be null");
  }

  @Test
  public void testGetVectorApplicationLogsSchema_ContainsAllRequiredFields() {
    // Act
    StructType schema = Schema.getVectorApplicationLogsSchema();

    // Assert
    assertNotNull(schema.getFieldIndex(Constants.APPLICATION_LOG_COLUMN_MESSAGE));
    assertNotNull(schema.getFieldIndex(Constants.APPLICATION_LOG_COLUMN_TIMESTAMP));
    assertNotNull(schema.getFieldIndex(Constants.APPLICATION_LOG_COLUMN_SERVICE_NAME));
  }

  @Test
  public void testGetVectorApplicationLogsSchema_HasCorrectNumberOfFields() {
    // Act
    StructType schema = Schema.getVectorApplicationLogsSchema();

    // Assert
    assertEquals(schema.fields().length, 3, "Schema should contain exactly 3 fields");
  }

  @Test
  public void testGetVectorApplicationLogsSchema_AllFieldsAreStringType() {
    // Act
    StructType schema = Schema.getVectorApplicationLogsSchema();

    // Assert
    for (StructField field : schema.fields()) {
      assertEquals(
          field.dataType(),
          DataTypes.StringType,
          String.format("Field '%s' should be of StringType", field.name()));
    }
  }

  @Test
  public void testGetVectorApplicationLogsSchema_MessageFieldExists() {
    // Act
    StructType schema = Schema.getVectorApplicationLogsSchema();

    // Assert
    assertTrue(schema.fieldNames().length > 0, "Schema should contain at least one field");

    StructField messageField = schema.fields()[0];
    assertEquals(
        messageField.name(),
        Constants.APPLICATION_LOG_COLUMN_MESSAGE,
        "First field should be message");
    assertEquals(
        messageField.dataType(), DataTypes.StringType, "Message field should be StringType");
  }

  @Test
  public void testGetVectorApplicationLogsSchema_TimestampFieldExists() {
    // Act
    StructType schema = Schema.getVectorApplicationLogsSchema();
    StructField timestampField = schema.apply(Constants.APPLICATION_LOG_COLUMN_TIMESTAMP);

    // Assert
    assertNotNull(timestampField, "Timestamp field should exist");
    assertEquals(
        timestampField.name(),
        Constants.APPLICATION_LOG_COLUMN_TIMESTAMP,
        "Field name should match constant");
    assertEquals(
        timestampField.dataType(), DataTypes.StringType, "Timestamp field should be StringType");
  }

  @Test
  public void testGetVectorApplicationLogsSchema_ServiceNameFieldExists() {
    // Act
    StructType schema = Schema.getVectorApplicationLogsSchema();
    StructField serviceNameField = schema.apply(Constants.APPLICATION_LOG_COLUMN_SERVICE_NAME);

    // Assert
    assertNotNull(serviceNameField, "Service name field should exist");
    assertEquals(
        serviceNameField.name(),
        Constants.APPLICATION_LOG_COLUMN_SERVICE_NAME,
        "Field name should match constant");
    assertEquals(
        serviceNameField.dataType(),
        DataTypes.StringType,
        "Service name field should be StringType");
  }

  @Test
  public void testGetVectorApplicationLogsSchema_IsReusable() {
    // Act - Call method multiple times
    StructType schema1 = Schema.getVectorApplicationLogsSchema();
    StructType schema2 = Schema.getVectorApplicationLogsSchema();

    // Assert - Should return equivalent schemas (not necessarily same instance)
    assertEquals(
        schema1.fields().length,
        schema2.fields().length,
        "Multiple calls should return schemas with same number of fields");

    for (int i = 0; i < schema1.fields().length; i++) {
      assertEquals(
          schema1.fields()[i].name(),
          schema2.fields()[i].name(),
          "Field names should match across multiple calls");
      assertEquals(
          schema1.fields()[i].dataType(),
          schema2.fields()[i].dataType(),
          "Field types should match across multiple calls");
    }
  }
}
