package com.logwise.orchestrator.config;

import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.enums.KafkaType;
import com.typesafe.config.Optional;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.NonFinal;

@Data
@Getter
public class ApplicationConfig {
  // Tenant configurations
  @NonFinal @NotNull @Valid List<TenantConfig> tenants;

  @Data
  public static class TenantConfig {
    // Tenant name
    @NonFinal @NotNull String name;
    // defaultLogsRetentionDays is used to set default retention days for service
    // logs in db while
    // onboarding
    @NonFinal @NotNull Integer defaultLogsRetentionDays;
    // envLogsRetentionDays is used to override default retention days for service
    // logs in db
    @NonFinal @NotNull @Optional List<EnvLogsRetentionDaysConfig> envLogsRetentionDays = List.of();
    // objectStore is used for s3/gcs configurations
    @NonFinal @NotNull ObjectStoreConfig objectStore;

    @NonFinal @NotNull VectorConfig vector;
    // kafka is used for kafka configurations
    @NonFinal @NotNull KafkaConfig kafka;
    // spark is used for spark configurations
    @NonFinal @NotNull SparkConfig spark;
    // delayMetrics is used for delay metrics configurations
    @NonFinal @NotNull DelayMetricsConfig delayMetrics;
  }

  @Data
  public static class ObjectStoreConfig {
    @NonFinal @Optional S3Config aws;
  }

  @Data
  public static class S3Config {
    @NonFinal String region;
    @NonFinal String bucket;
    // roleArn is used for cross account access
    @NonFinal @Optional String roleArn;
    // endpointOverride is used for localstack
    @NonFinal @Optional String endpointOverride;
  }

  @Data
  public static class VectorConfig {
    @NonFinal @NotNull String host;
    @NonFinal @NotNull Integer apiPort;
  }

  @Data
  public static class KafkaConfig {
    // Feature flag for new partition scaling system
    @NonFinal @Optional Boolean enablePartitionScaling = true;

    // Kafka type selection
    @NonFinal @Optional KafkaType kafkaType = KafkaType.EC2;

    // Custom setter to handle string-to-enum conversion from config
    public void setKafkaType(Object kafkaType) {
      if (kafkaType == null) {
        this.kafkaType = KafkaType.EC2;
      } else if (kafkaType instanceof KafkaType) {
        this.kafkaType = (KafkaType) kafkaType;
      } else {
        this.kafkaType = KafkaType.fromValue(kafkaType.toString());
      }
    }

    // Common connection settings
    @NonFinal @NotNull String kafkaBrokersHost;
    @NonFinal @Optional Integer kafkaBrokerPort = ApplicationConstants.KAFKA_BROKER_PORT;

    // Scaling thresholds
    @NonFinal @Optional Long maxLagPerPartition = 50_000L;
    @NonFinal @Optional Integer defaultPartitions = 3;

    // MSK-specific fields
    @NonFinal @Optional String mskClusterArn;
    @NonFinal @Optional String mskRegion;

    // Confluent-specific fields
    @NonFinal @Optional String confluentApiKey;
    @NonFinal @Optional String confluentApiSecret;
    @NonFinal @Optional String confluentRestEndpoint;

    // SSL/TLS configuration (common)
    @NonFinal @Optional String sslTruststoreLocation;
    @NonFinal @Optional String sslTruststorePassword;
    @NonFinal @Optional String sslKeystoreLocation;
    @NonFinal @Optional String sslKeystorePassword;

    // Legacy (deprecated, for migration)
    @NonFinal @Optional
    Integer maxProducerRatePerPartition =
        ApplicationConstants.KAFKA_MAX_PRODUCER_RATE_PER_PARTITION;
  }

  @Data
  public static class SparkConfig {
    @NonFinal @NotNull String sparkMasterHost;
    @NonFinal @NotNull String kafkaMaxRatePerPartition;
    @NonFinal @NotNull String kafkaStartingOffsets;
    @NonFinal @NotNull String subscribePattern;
    @NonFinal @NotNull String sparkJarPath;
    @NonFinal @NotNull String clientSparkVersion;
    @NonFinal @NotNull String mainClass;
    @NonFinal @NotNull String appName;
    @NonFinal @NotNull String driverCoresMax;
    @NonFinal @NotNull String driverCores;
    @NonFinal @NotNull String driverMemory;
    @NonFinal @NotNull String driverMaxResultSize;
    @NonFinal @NotNull String log4jPropertiesFilePath;
    @NonFinal @NotNull String executorCores;
    @NonFinal @NotNull String executorMemory;
    @NonFinal @NotNull String logsDir;
    @NonFinal @NotNull String checkPointDir;
    @NonFinal @Optional String s3aAccessKey;
    @NonFinal @Optional String s3aSecretKey;
    @NonFinal @Optional String awsAccessKeyId;
    @NonFinal @Optional String awsSecretAccessKey;
    @NonFinal @Optional String awsSessionToken;
    @NonFinal @Optional String awsRegion;
  }

  @Data
  public static class DelayMetricsConfig {
    @NonFinal @NotNull ApplicationDelayMetricsConfig app;
  }

  @Data
  public static class ApplicationDelayMetricsConfig {
    @NonFinal @NotNull String sampleEnv;
    @NonFinal @NotNull String sampleServiceName;
    @NonFinal @NotNull String sampleComponentType;
  }

  @Data
  public static class EnvLogsRetentionDaysConfig {
    @NonFinal @NotNull List<String> envs;
    @NonFinal @NotNull Integer retentionDays;
  }
}
