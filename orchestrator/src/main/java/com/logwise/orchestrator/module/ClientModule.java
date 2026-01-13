package com.logwise.orchestrator.module;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.logwise.orchestrator.client.AsgClient;
import com.logwise.orchestrator.client.KubernetesClient;
import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.client.VMClient;
import com.logwise.orchestrator.client.impl.AsgClientAwsImpl;
import com.logwise.orchestrator.client.impl.KubernetesClientK8sImpl;
import com.logwise.orchestrator.client.impl.ObjectStoreAwsImpl;
import com.logwise.orchestrator.client.impl.VMClientAwsImpl;
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
    bindSparkVMClients();
    bindSparkAsgClients();
    bindSparkKubernetesClients();
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

  private void bindSparkAsgClients() {
    log.info("Binding Spark ASG Clients...");
    ApplicationConfigProvider.getApplicationConfig()
        .getTenants()
        .forEach(
            tenantConfig -> {
              if (ApplicationConfigUtil.isAwsSparkCluster(tenantConfig)) {
                String injectorName =
                    ApplicationConstants.SPARK_ASG_INJECTOR_NAME.apply(tenantConfig.getName());
                bind(AsgClient.class)
                    .annotatedWith(Names.named(injectorName))
                    .toInstance(new AsgClientAwsImpl());
              }
            });
  }

  private void bindSparkVMClients() {
    log.info("Binding Spark VM Clients...");
    ApplicationConfigProvider.getApplicationConfig()
        .getTenants()
        .forEach(
            tenantConfig -> {
              if (ApplicationConfigUtil.isAwsSparkCluster(tenantConfig)) {
                String sparkVMInjectorName =
                    ApplicationConstants.SPARK_VM_INJECTOR_NAME.apply(tenantConfig.getName());
                bind(VMClient.class)
                    .annotatedWith(Names.named(sparkVMInjectorName))
                    .toInstance(new VMClientAwsImpl());
              }
            });
  }

  private void bindSparkKubernetesClients() {
    log.info("Binding Spark Kubernetes Clients...");
    ApplicationConfigProvider.getApplicationConfig()
        .getTenants()
        .forEach(
            tenantConfig -> {
              String clusterType = tenantConfig.getSpark().getCluster().getClusterType();
              boolean isKubernetes = ApplicationConfigUtil.isKubernetesSparkCluster(tenantConfig);
              log.info(
                  "Tenant: {}, clusterType: {}, isKubernetesSparkCluster: {}, kubernetes config: {}",
                  tenantConfig.getName(),
                  clusterType,
                  isKubernetes,
                  tenantConfig.getSpark().getCluster().getKubernetes() != null);
              if (isKubernetes) {
                String injectorName =
                    ApplicationConstants.SPARK_KUBERNETES_INJECTOR_NAME.apply(
                        tenantConfig.getName());
                log.info(
                    "Binding KubernetesClient for tenant: {} with injector name: {}",
                    tenantConfig.getName(),
                    injectorName);
                bind(KubernetesClient.class)
                    .annotatedWith(Names.named(injectorName))
                    .toInstance(new KubernetesClientK8sImpl());
              } else {
                log.warn(
                    "Skipping Kubernetes client binding for tenant: {} - not a Kubernetes cluster",
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
