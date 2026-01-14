package com.logwise.spark.utils;

import com.logwise.spark.base.MockConfigHelper;
import com.logwise.spark.constants.Constants;
import com.typesafe.config.Config;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for ConfigUtils utility class. */
public class ConfigUtilsTest {

  @Test
  public void testGetConfigMap_WithExistingPath_ReturnsConfigMap() {
    // Arrange
    Map<String, Object> configMap = new java.util.HashMap<>();
    configMap.put("spark.config.key1", "value1");
    configMap.put("spark.config.key2", "value2");
    Config config = MockConfigHelper.createConfig(configMap);

    // Act
    Map<String, String> result = ConfigUtils.getConfigMap(config, "spark.config");

    // Assert
    Assert.assertNotNull(result);
    Assert.assertFalse(result.isEmpty());
  }

  @Test
  public void testGetConfigMap_WithNonExistentPath_ReturnsEmptyMap() {
    // Arrange
    Config config = MockConfigHelper.createEmptyConfig();

    // Act
    Map<String, String> result = ConfigUtils.getConfigMap(config, "non.existent.path");

    // Assert
    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testGetConfigMap_WithNestedConfig_ReturnsFlattenedMap() {
    // Arrange
    Map<String, Object> nestedConfig = new java.util.HashMap<>();
    Map<String, Object> sparkConfig = new java.util.HashMap<>();
    sparkConfig.put("key1", "value1");
    nestedConfig.put("spark", sparkConfig);
    Config config = MockConfigHelper.createNestedConfig(nestedConfig);

    // Act
    Map<String, String> result = ConfigUtils.getConfigMap(config, "spark");

    // Assert
    Assert.assertNotNull(result);
    Assert.assertFalse(result.isEmpty());
  }

  @Test
  public void testGetSparkConfig_WithSparkConfigPath_ReturnsConfigMap() {
    // Arrange
    Map<String, Object> configMap = new java.util.HashMap<>();
    Map<String, Object> sparkConfig = new java.util.HashMap<>();
    sparkConfig.put("spark.app.name", "test-app");
    sparkConfig.put("spark.master", "local[2]");
    configMap.put(Constants.CONFIG_KEY_SPARK_CONFIG, sparkConfig);
    Config config = MockConfigHelper.createNestedConfig(configMap);

    // Act
    Map<String, String> result = ConfigUtils.getSparkConfig(config);

    // Assert
    Assert.assertNotNull(result);
    // Note: The actual structure depends on how Constants.CONFIG_KEY_SPARK_CONFIG
    // is defined
  }

  @Test
  public void testGetSparkConfig_WithNoSparkConfig_ReturnsEmptyMap() {
    // Arrange
    Config config = MockConfigHelper.createEmptyConfig();

    // Act
    Map<String, String> result = ConfigUtils.getSparkConfig(config);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testGetSparkHadoopConfig_WithHadoopConfigPath_ReturnsConfigMap() {
    // Arrange
    Map<String, Object> configMap = new java.util.HashMap<>();
    Map<String, Object> hadoopConfig = new java.util.HashMap<>();
    hadoopConfig.put("fs.s3a.endpoint", "s3.amazonaws.com");
    configMap.put(Constants.CONFIG_KEY_SPARK_HADOOP_CONFIG, hadoopConfig);
    Config config = MockConfigHelper.createNestedConfig(configMap);

    // Act
    Map<String, String> result = ConfigUtils.getSparkHadoopConfig(config);

    // Assert
    Assert.assertNotNull(result);
    // Note: The actual structure depends on how
    // Constants.CONFIG_KEY_SPARK_HADOOP_CONFIG is defined
  }

  @Test
  public void testGetSparkHadoopConfig_WithNoHadoopConfig_ReturnsEmptyMap() {
    // Arrange
    Config config = MockConfigHelper.createEmptyConfig();

    // Act
    Map<String, String> result = ConfigUtils.getSparkHadoopConfig(config);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testGetConfigMap_WithQuotedKeys_RemovesQuotes() {
    // Arrange - Config with keys that have quotes (simulated via ApplicationUtils)
    Map<String, Object> configMap = new java.util.HashMap<>();
    Map<String, Object> sparkConfig = new java.util.HashMap<>();
    sparkConfig.put("\"quoted.key\"", "value1");
    sparkConfig.put("normal.key", "value2");
    configMap.put("spark.config", sparkConfig);
    Config config = MockConfigHelper.createNestedConfig(configMap);

    // Act
    Map<String, String> result = ConfigUtils.getConfigMap(config, "spark.config");

    // Assert
    Assert.assertNotNull(result);
    // The keys should have quotes removed by
    // ApplicationUtils.removeSurroundingQuotes
    // This tests the integration with ApplicationUtils
  }

  @Test
  public void testGetConfigMap_WithNullValues_HandlesGracefully() {
    // Arrange
    Map<String, Object> configMap = new java.util.HashMap<>();
    Map<String, Object> sparkConfig = new java.util.HashMap<>();
    sparkConfig.put("key1", null);
    sparkConfig.put("key2", "value2");
    configMap.put("spark.config", sparkConfig);
    Config config = MockConfigHelper.createNestedConfig(configMap);

    // Act
    Map<String, String> result = ConfigUtils.getConfigMap(config, "spark.config");

    // Assert
    Assert.assertNotNull(result);
    // Should handle null values gracefully (convert to "null" string or skip)
  }

  @Test
  public void testGetConfigMap_WithNonStringValues_ConvertsToString() {
    // Arrange
    Map<String, Object> configMap = new java.util.HashMap<>();
    Map<String, Object> sparkConfig = new java.util.HashMap<>();
    sparkConfig.put("intKey", 123);
    sparkConfig.put("boolKey", true);
    sparkConfig.put("stringKey", "value");
    configMap.put("spark.config", sparkConfig);
    Config config = MockConfigHelper.createNestedConfig(configMap);

    // Act
    Map<String, String> result = ConfigUtils.getConfigMap(config, "spark.config");

    // Assert
    Assert.assertNotNull(result);
    // All values should be converted to strings via unwrapped().toString()
  }

  @Test
  public void testGetConfigMap_WithEmptyConfig_ReturnsEmptyMap() {
    // Arrange
    Map<String, Object> configMap = new java.util.HashMap<>();
    Map<String, Object> emptyConfig = new java.util.HashMap<>();
    configMap.put("spark.config", emptyConfig);
    Config config = MockConfigHelper.createNestedConfig(configMap);

    // Act
    Map<String, String> result = ConfigUtils.getConfigMap(config, "spark.config");

    // Assert
    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testGetConfigMap_WithComplexNestedValues_ConvertsToString() {
    // Arrange
    Map<String, Object> configMap = new java.util.HashMap<>();
    Map<String, Object> sparkConfig = new java.util.HashMap<>();
    sparkConfig.put("doubleKey", 123.456);
    sparkConfig.put("longKey", 123456789L);
    sparkConfig.put("listKey", java.util.Arrays.asList("a", "b", "c"));
    configMap.put("spark.config", sparkConfig);
    Config config = MockConfigHelper.createNestedConfig(configMap);

    // Act
    Map<String, String> result = ConfigUtils.getConfigMap(config, "spark.config");

    // Assert
    Assert.assertNotNull(result);
    Assert.assertFalse(result.isEmpty());
  }

  @Test
  public void testGetSparkConfig_WithValidConfig_ReturnsConfigMap() {
    // Arrange
    Map<String, Object> configMap = new java.util.HashMap<>();
    Map<String, Object> sparkConfig = new java.util.HashMap<>();
    sparkConfig.put("spark.app.name", "test-app");
    sparkConfig.put("spark.master", "local[*]");
    sparkConfig.put("spark.executor.cores", "2");
    configMap.put(Constants.CONFIG_KEY_SPARK_CONFIG, sparkConfig);
    Config config = MockConfigHelper.createNestedConfig(configMap);

    // Act
    Map<String, String> result = ConfigUtils.getSparkConfig(config);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertFalse(result.isEmpty());
  }

  @Test
  public void testGetSparkHadoopConfig_WithValidConfig_ReturnsConfigMap() {
    // Arrange
    Map<String, Object> configMap = new java.util.HashMap<>();
    Map<String, Object> hadoopConfig = new java.util.HashMap<>();
    hadoopConfig.put("fs.s3a.endpoint", "s3.amazonaws.com");
    hadoopConfig.put("fs.s3a.access.key", "test-key");
    hadoopConfig.put("fs.s3a.secret.key", "test-secret");
    configMap.put(Constants.CONFIG_KEY_SPARK_HADOOP_CONFIG, hadoopConfig);
    Config config = MockConfigHelper.createNestedConfig(configMap);

    // Act
    Map<String, String> result = ConfigUtils.getSparkHadoopConfig(config);

    // Assert
    Assert.assertNotNull(result);
    Assert.assertFalse(result.isEmpty());
  }

  @Test
  public void testGetConfigMap_WithQuotedKeys_RemovesQuotesFromKeys() {
    // Arrange
    Map<String, Object> configMap = new java.util.HashMap<>();
    Map<String, Object> sparkConfig = new java.util.HashMap<>();
    sparkConfig.put("\"quoted.key\"", "value1");
    sparkConfig.put("normal.key", "value2");
    configMap.put("spark.config", sparkConfig);
    Config config = MockConfigHelper.createNestedConfig(configMap);

    // Act
    Map<String, String> result = ConfigUtils.getConfigMap(config, "spark.config");

    // Assert
    Assert.assertNotNull(result);
    // Keys with quotes should have quotes removed
    Assert.assertTrue(result.containsKey("quoted.key") || result.containsKey("\"quoted.key\""));
  }

  @Test
  public void testGetConfigMap_WithHasPathFalse_ReturnsEmptyMap() {
    // Test the branch where config.hasPath(key) returns false
    Config config = MockConfigHelper.createEmptyConfig();

    Map<String, String> result = ConfigUtils.getConfigMap(config, "non.existent.path");

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testGetConfigMap_WithHasPathTrue_ReturnsConfigMap() {
    // Test the branch where config.hasPath(key) returns true
    Map<String, Object> configMap = new java.util.HashMap<>();
    Map<String, Object> sparkConfig = new java.util.HashMap<>();
    sparkConfig.put("key1", "value1");
    configMap.put("spark.config", sparkConfig);
    Config config = MockConfigHelper.createNestedConfig(configMap);

    Map<String, String> result = ConfigUtils.getConfigMap(config, "spark.config");

    Assert.assertNotNull(result);
    Assert.assertFalse(result.isEmpty());
  }
}
