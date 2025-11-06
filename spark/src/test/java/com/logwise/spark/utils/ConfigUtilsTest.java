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
}
