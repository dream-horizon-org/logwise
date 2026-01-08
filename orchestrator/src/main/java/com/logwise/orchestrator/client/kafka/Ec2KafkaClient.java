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
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;

/**
 * Kafka client implementation for self-managed Kafka on EC2. This is the simplest implementation
 * with basic authentication.
 */
@Slf4j
public class Ec2KafkaClient extends AbstractKafkaClient {

  public interface Factory {
    Ec2KafkaClient create(@Assisted KafkaConfig kafkaConfig);
  }

  @Inject
  public Ec2KafkaClient(@Assisted KafkaConfig kafkaConfig) {
    super(kafkaConfig);
  }

  @Override
  public KafkaType getKafkaType() {
    return KafkaType.EC2;
  }

  @Override
  public Single<String> buildBootstrapServers() {
    // Resolve hostname to IP addresses (current behavior)
    int port =
        kafkaConfig.getKafkaBrokerPort() != null
            ? kafkaConfig.getKafkaBrokerPort()
            : ApplicationConstants.KAFKA_BROKER_PORT;

    return ApplicationUtils.getIpAddresses(kafkaConfig.getKafkaBrokersHost())
        .map(
            ips -> {
              String bootstrapServers =
                  ips.stream().map(ip -> ip + ":" + port).collect(Collectors.joining(","));
              log.info("Built bootstrap servers for EC2 Kafka: {}", bootstrapServers);
              return bootstrapServers;
            });
  }

  @Override
  public Single<Map<String, Object>> buildAdminClientConfig() {
    log.info("Building AdminClient config for EC2 Kafka");

    return buildBootstrapServers()
        .map(
            bootstrapServers -> {
              Map<String, Object> config = new HashMap<>();
              config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
              config.put(
                  AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG,
                  ApplicationConstants.KAFKA_REQUEST_TIMEOUT_MS);

              // Add SSL configuration if provided
              if (kafkaConfig.getSslTruststoreLocation() != null) {
                config.put("security.protocol", "SSL");
                config.put("ssl.truststore.location", kafkaConfig.getSslTruststoreLocation());
                if (kafkaConfig.getSslTruststorePassword() != null) {
                  config.put("ssl.truststore.password", kafkaConfig.getSslTruststorePassword());
                }
              }

              return config;
            });
  }
}
