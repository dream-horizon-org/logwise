package com.logwise.orchestrator.client.kafka;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.logwise.orchestrator.config.ApplicationConfig.KafkaConfig;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.enums.KafkaType;
import io.reactivex.Single;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.plain.PlainLoginModule;

/**
 * Kafka client implementation for Confluent Cloud/Platform. Uses API key/secret for authentication.
 */
@Slf4j
public class ConfluentKafkaClient extends AbstractKafkaClient {

  public interface Factory {
    ConfluentKafkaClient create(@Assisted KafkaConfig kafkaConfig);
  }

  private final String apiKey;
  private final String apiSecret;

  @Inject
  public ConfluentKafkaClient(@Assisted KafkaConfig kafkaConfig) {
    super(kafkaConfig);
    this.apiKey = kafkaConfig.getConfluentApiKey();
    this.apiSecret = kafkaConfig.getConfluentApiSecret();

    if (apiKey == null || apiKey.isEmpty() || apiSecret == null || apiSecret.isEmpty()) {
      throw new IllegalStateException("Confluent API key and secret must be provided");
    }
  }

  @Override
  public KafkaType getKafkaType() {
    return KafkaType.CONFLUENT;
  }

  @Override
  public Single<String> buildBootstrapServers() {
    // Confluent provides bootstrap servers directly
    String bootstrapServers = kafkaConfig.getKafkaBrokersHost();
    if (bootstrapServers == null || bootstrapServers.isEmpty()) {
      return Single.error(
          new IllegalStateException(
              "Confluent bootstrap servers must be provided via kafkaBrokersHost"));
    }
    log.info("Using Confluent bootstrap servers: {}", bootstrapServers);
    return Single.just(bootstrapServers);
  }

  @Override
  public Single<Map<String, Object>> buildAdminClientConfig() {
    log.info("Building AdminClient config for Confluent with API key authentication");

    return buildBootstrapServers()
        .map(
            bootstrapServers -> {
              Map<String, Object> config = new HashMap<>();
              config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
              config.put(
                  AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,
                  ApplicationConstants.KAFKA_REQUEST_TIMEOUT_MS);

              // Confluent SASL/PLAIN authentication
              config.put("security.protocol", "SASL_SSL");
              config.put(SaslConfigs.SASL_MECHANISM, "PLAIN");

              // JAAS configuration
              String saslJaasConfig =
                  String.format(
                      "%s required username=\"%s\" password=\"%s\";",
                      PlainLoginModule.class.getName(), apiKey, apiSecret);
              config.put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);

              // SSL configuration for Confluent
              config.put("ssl.endpoint.identification.algorithm", "https");

              return config;
            });
  }
}
