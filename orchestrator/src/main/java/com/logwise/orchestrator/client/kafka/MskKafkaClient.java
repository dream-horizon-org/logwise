package com.logwise.orchestrator.client.kafka;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.logwise.orchestrator.config.ApplicationConfig.KafkaConfig;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.enums.KafkaType;
import com.logwise.orchestrator.util.ApplicationUtils;
import io.reactivex.Single;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SaslConfigs;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kafka.KafkaClient;
import software.amazon.awssdk.services.kafka.model.GetBootstrapBrokersRequest;

/**
 * Kafka client implementation for AWS MSK (Managed Streaming for Kafka). Uses IAM authentication
 * for secure access.
 */
@Slf4j
public class MskKafkaClient extends AbstractKafkaClient {

  public interface Factory {
    MskKafkaClient create(@Assisted KafkaConfig kafkaConfig);
  }

  private final String mskClusterArn;
  private final Region awsRegion;

  @Inject
  public MskKafkaClient(@Assisted KafkaConfig kafkaConfig) {
    super(kafkaConfig);
    this.mskClusterArn = kafkaConfig.getMskClusterArn();
    this.awsRegion =
        kafkaConfig.getMskRegion() != null
            ? Region.of(kafkaConfig.getMskRegion())
            : Region.US_EAST_1;
  }

  @Override
  public KafkaType getKafkaType() {
    return KafkaType.MSK;
  }

  @Override
  public Single<String> buildBootstrapServers() {
    // Option 1: Use provided bootstrap servers (if configured)
    if (kafkaConfig.getKafkaBrokersHost() != null && !kafkaConfig.getKafkaBrokersHost().isEmpty()) {
      log.info("Using provided MSK bootstrap servers");
      return Single.just(kafkaConfig.getKafkaBrokersHost());
    }

    // Option 2: Fetch from MSK API
    if (mskClusterArn != null && !mskClusterArn.isEmpty()) {
      log.info("Fetching bootstrap servers from MSK API");
      return fetchBootstrapBrokersFromMsk();
    }

    return Single.error(
        new IllegalStateException(
            "MSK bootstrap servers must be provided via kafkaBrokersHost or mskClusterArn"));
  }

  private Single<String> fetchBootstrapBrokersFromMsk() {
    return ApplicationUtils.executeBlockingCallable(
            () -> {
              try (KafkaClient mskClient =
                  KafkaClient.builder()
                      .region(awsRegion)
                      .credentialsProvider(DefaultCredentialsProvider.create())
                      .build()) {

                GetBootstrapBrokersRequest request =
                    GetBootstrapBrokersRequest.builder().clusterArn(mskClusterArn).build();

                // Try IAM auth first, fallback to plaintext if needed
                String bootstrapBrokers =
                    mskClient.getBootstrapBrokers(request).bootstrapBrokerStringSaslIam();

                if (bootstrapBrokers == null || bootstrapBrokers.isEmpty()) {
                  bootstrapBrokers = mskClient.getBootstrapBrokers(request).bootstrapBrokerString();
                }

                log.info("Fetched MSK bootstrap brokers: {}", bootstrapBrokers);
                return bootstrapBrokers;
              } catch (Exception e) {
                log.error("Error fetching MSK bootstrap brokers", e);
                throw new RuntimeException("Failed to fetch MSK bootstrap brokers", e);
              }
            })
        .toSingle();
  }

  @Override
  public Single<Map<String, Object>> buildAdminClientConfig() {
    log.info("Building AdminClient config for MSK with IAM authentication");

    return buildBootstrapServers()
        .map(
            bootstrapServers -> {
              Map<String, Object> config = new HashMap<>();
              config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
              config.put(
                  AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,
                  ApplicationConstants.KAFKA_REQUEST_TIMEOUT_MS);

              // MSK IAM Authentication
              config.put("security.protocol", "SASL_SSL");
              config.put(SaslConfigs.SASL_MECHANISM, "AWS_MSK_IAM");
              config.put(
                  SaslConfigs.SASL_CLIENT_CALLBACK_HANDLER_CLASS,
                  "software.amazon.msk.auth.iam.IAMClientCallbackHandler");

              // SSL configuration
              config.put("ssl.endpoint.identification.algorithm", "https");

              return config;
            });
  }
}
