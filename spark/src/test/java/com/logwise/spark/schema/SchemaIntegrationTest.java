package com.logwise.spark.schema;

import static org.testng.Assert.*;

import com.logwise.spark.constants.Constants;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Integration tests for Schema using real SparkSession (local mode).
 *
 * <p>Tests verify that schemas work correctly with actual Spark DataFrame operations, including
 * creation, querying, filtering, and transformations.
 */
public class SchemaIntegrationTest {

  private SparkSession spark;

  @BeforeMethod
  public void setUp() {
    spark =
        SparkSession.builder()
            .appName("SchemaIntegrationTest")
            .master("local[1]")
            .config("spark.sql.shuffle.partitions", "1")
            .config("spark.sql.adaptive.enabled", "false")
            .getOrCreate();
  }

  @AfterMethod
  public void tearDown() {
    if (spark != null) {
      spark.stop();
    }
  }

  @Test
  public void testCreateDataFrame_WithValidSchema_ReturnsDataFrame() {
    // Arrange
    StructType schema = Schema.getVectorApplicationLogsSchema();
    Row row =
        RowFactory.create(
            "Test log message",
            "2021-01-01T00:00:00Z",
            "production",
            "api-service",
            "api-container");

    // Act
    Dataset<Row> df = spark.createDataFrame(java.util.Arrays.asList(row), schema);

    // Assert
    assertNotNull(df);
    assertEquals(df.count(), 1L);
    assertEquals(df.schema(), schema);
  }

  @Test
  public void testCreateDataFrame_WithAllRequiredFields_Succeeds() {
    // Arrange
    StructType schema = Schema.getVectorApplicationLogsSchema();
    Row row = RowFactory.create("Message", "Timestamp", "Env", "ServiceName", "ComponentName");

    // Act
    Dataset<Row> df = spark.createDataFrame(java.util.Arrays.asList(row), schema);

    // Assert
    assertEquals(df.count(), 1L);
    StructField[] fields = df.schema().fields();
    assertEquals(fields.length, 5);
    assertEquals(fields[0].name(), Constants.APPLICATION_LOG_COLUMN_MESSAGE);
    assertEquals(fields[2].name(), Constants.APPLICATION_LOG_COLUMN_ENVIRONMENT_NAME);
    assertEquals(fields[3].name(), Constants.APPLICATION_LOG_COLUMN_SERVICE_NAME);
  }

  @Test(expectedExceptions = ArrayIndexOutOfBoundsException.class)
  public void testCreateDataFrame_WithMissingFields_ThrowsExceptionOnAccess() {
    // Arrange
    StructType schema = Schema.getVectorApplicationLogsSchema();
    // Create row with only 4 fields instead of 5
    Row incompleteRow = RowFactory.create("Message", "Timestamp", "Env", "ServiceName");

    // Act - Spark creates DataFrame but Row only has 4 fields
    Dataset<Row> df = spark.createDataFrame(java.util.Arrays.asList(incompleteRow), schema);

    // Assert - DataFrame is created but accessing missing fields throws exception
    assertNotNull(df);
    assertEquals(df.count(), 1L);
    Row result = df.first();
    // First 4 fields should have values
    assertEquals(result.getString(0), "Message");
    assertEquals(result.getString(1), "Timestamp");
    assertEquals(result.getString(2), "Env");
    assertEquals(result.getString(3), "ServiceName");
    // Accessing field beyond Row size should throw ArrayIndexOutOfBoundsException
    result.getString(4); // ComponentName - should throw
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testCreateDataFrame_WithWrongDataTypes_ThrowsException() {
    // Arrange
    StructType schema = Schema.getVectorApplicationLogsSchema();
    // Create row with integer instead of string for message field
    Row wrongTypeRow =
        RowFactory.create(
            12345, // Wrong type - should be String
            "Timestamp",
            "Env",
            "ServiceName",
            "ComponentName");

    // Act - should throw IllegalArgumentException
    spark.createDataFrame(java.util.Arrays.asList(wrongTypeRow), schema);
  }

  @Test
  public void testQueryDataFrame_WithSchema_ExecutesSuccessfully() {
    // Arrange
    StructType schema = Schema.getVectorApplicationLogsSchema();
    Row row1 =
        RowFactory.create(
            "Error occurred", "2021-01-01T00:00:00Z", "prod", "api-service", "api-container");
    Row row2 =
        RowFactory.create(
            "Info message", "2021-01-01T00:01:00Z", "prod", "api-service", "api-container");

    Dataset<Row> df = spark.createDataFrame(java.util.Arrays.asList(row1, row2), schema);

    // Act
    Dataset<Row> result = df.select(Constants.APPLICATION_LOG_COLUMN_MESSAGE);

    // Assert
    assertEquals(result.count(), 2L);
    assertEquals(result.schema().fields().length, 1);
    assertEquals(result.schema().fields()[0].name(), Constants.APPLICATION_LOG_COLUMN_MESSAGE);
  }

  @Test
  public void testFilterDataFrame_ByMessageField_ReturnsFilteredResults() {
    // Arrange
    StructType schema = Schema.getVectorApplicationLogsSchema();
    Row row1 =
        RowFactory.create(
            "Error occurred", "2021-01-01T00:00:00Z", "prod", "api-service", "api-container");
    Row row2 =
        RowFactory.create(
            "Info message", "2021-01-01T00:01:00Z", "prod", "api-service", "api-container");

    Dataset<Row> df = spark.createDataFrame(java.util.Arrays.asList(row1, row2), schema);

    // Act
    Dataset<Row> filtered =
        df.filter(
            org.apache
                .spark
                .sql
                .functions
                .col(Constants.APPLICATION_LOG_COLUMN_MESSAGE)
                .contains("Error"));

    // Assert
    assertEquals(filtered.count(), 1L);
    Row result = filtered.first();
    assertEquals(result.getString(0), "Error occurred");
  }

  @Test
  public void testSelectColumns_WithSchema_ReturnsCorrectColumns() {
    // Arrange
    StructType schema = Schema.getVectorApplicationLogsSchema();
    Row row =
        RowFactory.create(
            "Test message", "2021-01-01T00:00:00Z", "prod", "api-service", "api-container");

    Dataset<Row> df = spark.createDataFrame(java.util.Arrays.asList(row), schema);

    // Act
    Dataset<Row> selected =
        df.select(
            Constants.APPLICATION_LOG_COLUMN_MESSAGE,
            Constants.APPLICATION_LOG_COLUMN_ENVIRONMENT_NAME,
            Constants.APPLICATION_LOG_COLUMN_SERVICE_NAME);

    // Assert
    assertEquals(selected.count(), 1L);
    StructField[] fields = selected.schema().fields();
    assertEquals(fields.length, 3);
    assertEquals(fields[0].name(), Constants.APPLICATION_LOG_COLUMN_MESSAGE);
    assertEquals(fields[1].name(), Constants.APPLICATION_LOG_COLUMN_ENVIRONMENT_NAME);
    assertEquals(fields[2].name(), Constants.APPLICATION_LOG_COLUMN_SERVICE_NAME);
  }

  @Test
  public void testSchemaCompatibility_WithProtobufFields_MatchesExpected() {
    // Arrange
    StructType schema = Schema.getVectorApplicationLogsSchema();

    // Act & Assert - Verify all fields that correspond to Protobuf fields exist
    assertNotNull(schema.fieldIndex(Constants.APPLICATION_LOG_COLUMN_MESSAGE));
    assertNotNull(schema.fieldIndex(Constants.APPLICATION_LOG_COLUMN_TIMESTAMP));
    assertNotNull(schema.fieldIndex(Constants.APPLICATION_LOG_COLUMN_ENVIRONMENT_NAME));
    assertNotNull(schema.fieldIndex(Constants.APPLICATION_LOG_COLUMN_SERVICE_NAME));
    assertNotNull(schema.fieldIndex(Constants.APPLICATION_LOG_COLUMN_COMPONENT_TYPE));

    // Verify all fields are StringType (as expected for Protobuf string fields)
    for (StructField field : schema.fields()) {
      assertEquals(field.dataType(), DataTypes.StringType);
    }
  }

  @Test
  public void testWriteDataFrame_WithSchema_ValidatesBeforeWrite() {
    // Arrange
    StructType schema = Schema.getVectorApplicationLogsSchema();
    Row row =
        RowFactory.create(
            "Test message", "2021-01-01T00:00:00Z", "prod", "api-service", "api-container");

    Dataset<Row> df = spark.createDataFrame(java.util.Arrays.asList(row), schema);

    // Act - Try to write (we'll use in-memory format for testing)
    // Note: Actual S3 write would require S3 setup, so we just validate the
    // DataFrame is writable
    Dataset<Row> writable = df.select("*");

    // Assert
    assertNotNull(writable);
    assertEquals(writable.schema(), schema);
    assertEquals(writable.count(), 1L);
  }

  @Test
  public void testSchemaEvolution_AddingNewField_HandlesGracefully() {
    // Arrange
    StructType originalSchema = Schema.getVectorApplicationLogsSchema();

    Row originalRow =
        RowFactory.create(
            "Test message", "2021-01-01T00:00:00Z", "prod", "api-service", "api-container");

    // Act - Create DataFrame with original schema
    Dataset<Row> df = spark.createDataFrame(java.util.Arrays.asList(originalRow), originalSchema);

    // Add new field with null values (simulating schema evolution)
    Dataset<Row> extended =
        df.withColumn(
            "new_field", org.apache.spark.sql.functions.lit(null).cast(DataTypes.StringType));

    // Assert
    assertEquals(extended.schema().fields().length, 6);
    assertEquals(extended.schema().fields()[5].name(), "new_field");
    assertTrue(extended.schema().fields()[5].nullable());
  }
}
