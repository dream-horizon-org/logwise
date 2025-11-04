package com.logwise.spark.guice.modules;

import com.logwise.spark.clients.*;
import com.logwise.spark.clients.impl.FeignClientImpl;
import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MainModule extends AbstractModule {
  private final Config config;

  @Override
  protected void configure() {
    bind(Config.class).toInstance(config);
    bind(FeignClient.class).to(FeignClientImpl.class);
    bind(SparkMasterClient.class).toInstance(getSparkMasterClient());
    bind(KafkaManagerClient.class).toInstance(getKafkaManagerClient());
    bind(LogCentralOrchestratorClient.class).toInstance(getLogCentralOrchestratorClient());
  }

  private SparkMasterClient getSparkMasterClient() {
    String url = config.getString("spark.master.host");
    return new FeignClientImpl().createClient(SparkMasterClient.class, url);
  }

  private KafkaManagerClient getKafkaManagerClient() {
    String url = config.getString("kafka.manager.host");
    return new FeignClientImpl().createClient(KafkaManagerClient.class, url);
  }

  private LogCentralOrchestratorClient getLogCentralOrchestratorClient() {
    String url = config.getString("logCentral.orchestrator.url");
    return new FeignClientImpl().createClient(LogCentralOrchestratorClient.class, url);
  }
}
