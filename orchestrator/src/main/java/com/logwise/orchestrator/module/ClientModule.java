package com.logwise.orchestrator.module;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.client.impl.ObjectStoreAwsImpl;
import com.logwise.orchestrator.client.kafka.ConfluentKafkaClient;
import com.logwise.orchestrator.client.kafka.Ec2KafkaClient;
import com.logwise.orchestrator.client.kafka.MskKafkaClient;
import com.logwise.orchestrator.common.guice.VertxAbstractModule;
import com.logwise.orchestrator.config.ApplicationConfigProvider;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import io.vertx.reactivex.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientModule extends VertxAbstractModule {
  public ClientModule(Vertx vertx) {
    super(vertx);
  }

  @Override
  protected void bindConfiguration() {
    log.info("Binding Client Modules Configuration...");
    bindObjectStoreClients();
    bindKafkaClients();
  }

  private void bindObjectStoreClients() {
    log.info("Binding Object Stores Clients...");
    ApplicationConfigProvider.getApplicationConfig()
        .getTenants()
        .forEach(
            tenantConfig -> {
              String injectorName =
                  ApplicationConstants.OBJECT_STORE_INJECTOR_NAME.apply(tenantConfig.getName());
              if (ApplicationConfigUtil.isAwsObjectStore(tenantConfig)) {
                bind(ObjectStoreClient.class)
                    .annotatedWith(Names.named(injectorName))
                    .toInstance(new ObjectStoreAwsImpl());
              } else {
                log.warn(
                    "Only AWS object store is supported. Skipping tenant: {}",
                    tenantConfig.getName());
              }
            });
  }

  private void bindKafkaClients() {
    log.info("Binding Kafka Clients...");
    // Bind factory interfaces for each Kafka client type
    install(
        new FactoryModuleBuilder()
            .implement(Ec2KafkaClient.class, Ec2KafkaClient.class)
            .build(Ec2KafkaClient.Factory.class));

    install(
        new FactoryModuleBuilder()
            .implement(MskKafkaClient.class, MskKafkaClient.class)
            .build(MskKafkaClient.Factory.class));

    install(
        new FactoryModuleBuilder()
            .implement(ConfluentKafkaClient.class, ConfluentKafkaClient.class)
            .build(ConfluentKafkaClient.Factory.class));

    // KafkaClientFactory will be automatically bound by Guice
  }
}
