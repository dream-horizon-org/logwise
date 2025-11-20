package com.logwise.orchestrator.testconfig;

import com.logwise.orchestrator.config.ApplicationConfig;
import lombok.experimental.UtilityClass;

/**
 * Shared test configuration utilities. Provides common mock configurations and test data for unit
 * tests.
 */
@UtilityClass
public class ApplicationTestConfig {

  /**
   * Creates a mock S3Config with default test values.
   *
   * @return ApplicationConfig.S3Config with test bucket and region
   */
  public static ApplicationConfig.S3Config createMockS3Config() {
    ApplicationConfig.S3Config s3Config = new ApplicationConfig.S3Config();
    s3Config.setBucket("test-bucket");
    s3Config.setRegion("us-east-1");
    return s3Config;
  }

  /**
   * Creates a mock ObjectStoreConfig with AWS S3 configuration.
   *
   * @return ApplicationConfig.ObjectStoreConfig with test S3 config
   */
  public static ApplicationConfig.ObjectStoreConfig createMockObjectStoreConfig() {
    ApplicationConfig.ObjectStoreConfig config = new ApplicationConfig.ObjectStoreConfig();
    config.setAws(createMockS3Config());
    return config;
  }

  /**
   * Creates a mock TenantConfig with default test values.
   *
   * @param tenantName the tenant name
   * @return ApplicationConfig.TenantConfig with test configuration
   */
  public static ApplicationConfig.TenantConfig createMockTenantConfig(String tenantName) {
    ApplicationConfig.TenantConfig tenantConfig = new ApplicationConfig.TenantConfig();
    tenantConfig.setName(tenantName);
    tenantConfig.setObjectStore(createMockObjectStoreConfig());
    tenantConfig.setDefaultLogsRetentionDays(7);

    // Create KafkaConfig with required fields
    ApplicationConfig.KafkaConfig kafkaConfig = new ApplicationConfig.KafkaConfig();
    kafkaConfig.setKafkaBrokersHost("localhost:9092");
    tenantConfig.setKafka(kafkaConfig);

    // Create SparkConfig with all required fields
    ApplicationConfig.SparkConfig sparkConfig = new ApplicationConfig.SparkConfig();
    sparkConfig.setLogsDir("logs");
    sparkConfig.setCheckPointDir("checkpoints");
    sparkConfig.setSparkMasterHost("localhost");
    sparkConfig.setKafkaMaxRatePerPartition("1000");
    sparkConfig.setKafkaStartingOffsets("latest");
    sparkConfig.setSubscribePattern("test-topic");
    sparkConfig.setSparkJarPath("s3://bucket/spark.jar");
    sparkConfig.setClientSparkVersion("3.0.0");
    sparkConfig.setMainClass("com.example.Main");
    sparkConfig.setAppName("test-app");
    sparkConfig.setDriverCoresMax("10");
    sparkConfig.setDriverCores("2");
    sparkConfig.setDriverMemory("4G");
    sparkConfig.setDriverMaxResultSize("2G");
    sparkConfig.setLog4jPropertiesFilePath("log4j.properties");
    sparkConfig.setExecutorCores("2");
    sparkConfig.setExecutorMemory("4G");
    tenantConfig.setSpark(sparkConfig);

    ApplicationConfig.DelayMetricsConfig delayMetricsConfig =
        new ApplicationConfig.DelayMetricsConfig();
    ApplicationConfig.ApplicationDelayMetricsConfig appDelayMetricsConfig =
        new ApplicationConfig.ApplicationDelayMetricsConfig();
    appDelayMetricsConfig.setSampleEnv("local");
    appDelayMetricsConfig.setSampleComponentType("application");
    appDelayMetricsConfig.setSampleServiceName("test-service");
    delayMetricsConfig.setApp(appDelayMetricsConfig);
    tenantConfig.setDelayMetrics(delayMetricsConfig);

    return tenantConfig;
  }

  /**
   * Creates a mock SparkConfig with default test values.
   *
   * @return ApplicationConfig.SparkConfig with test configuration
   */
  public static ApplicationConfig.SparkConfig createMockSparkConfig() {
    ApplicationConfig.SparkConfig sparkConfig = new ApplicationConfig.SparkConfig();
    sparkConfig.setLogsDir("logs");
    return sparkConfig;
  }
}
