package com.logwise.spark.guice.modules;

import com.google.inject.AbstractModule;
import com.logwise.spark.clients.LogCentralOrchestratorClient;
import com.logwise.spark.clients.SparkMasterClient;
import com.logwise.spark.clients.impl.FeignClientImpl;
import com.typesafe.config.Config;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MainModule extends AbstractModule {
  private final Config config;

  @Override
  protected void configure() {
    bind(Config.class).toInstance(config);
    bind(LogCentralOrchestratorClient.class).toInstance(getLogCentralOrchestratorClient());
    bind(SparkMasterClient.class).toInstance(getSparkMasterClient());
  }

  private LogCentralOrchestratorClient getLogCentralOrchestratorClient() {
    String url = config.getString("logCentral.orchestrator.url");
    return new FeignClientImpl().createClient(LogCentralOrchestratorClient.class, url);
  }

  private SparkMasterClient getSparkMasterClient() {
    String url = config.getString("spark.master.host");
    return new FeignClientImpl().createClient(SparkMasterClient.class, url);
  }
}
