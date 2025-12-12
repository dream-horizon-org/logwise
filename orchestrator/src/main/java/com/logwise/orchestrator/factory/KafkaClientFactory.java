package com.logwise.orchestrator.factory;

import com.google.inject.Inject;
import com.logwise.orchestrator.client.kafka.*;
import com.logwise.orchestrator.config.ApplicationConfig.KafkaConfig;
import com.logwise.orchestrator.enums.KafkaType;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory for creating appropriate Kafka client based on Kafka type. Uses Guice to inject
 * dependencies for each client type.
 */
@Slf4j
public class KafkaClientFactory {

  private final Ec2KafkaClient.Factory ec2Factory;
  private final MskKafkaClient.Factory mskFactory;
  private final ConfluentKafkaClient.Factory confluentFactory;

  @Inject
  public KafkaClientFactory(
      Ec2KafkaClient.Factory ec2Factory,
      MskKafkaClient.Factory mskFactory,
      ConfluentKafkaClient.Factory confluentFactory) {
    this.ec2Factory = ec2Factory;
    this.mskFactory = mskFactory;
    this.confluentFactory = confluentFactory;
  }

  public KafkaClient createKafkaClient(KafkaConfig kafkaConfig) {
    KafkaType kafkaType = kafkaConfig.getKafkaType();

    if (kafkaType == null) {
      throw new IllegalArgumentException("Kafka type cannot be null");
    }

    log.info("Creating Kafka client for type: {}", kafkaType);

    switch (kafkaType) {
      case EC2:
        return ec2Factory.create(kafkaConfig);
      case MSK:
        return mskFactory.create(kafkaConfig);
      case CONFLUENT:
        return confluentFactory.create(kafkaConfig);
      default:
        throw new IllegalArgumentException("Unsupported Kafka type: " + kafkaType);
    }
  }
}
