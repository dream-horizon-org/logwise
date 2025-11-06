package com.logwise.spark.guice.modules;

import com.google.inject.AbstractModule;
import com.typesafe.config.Config;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MainModule extends AbstractModule {
  private final Config config;

  @Override
  protected void configure() {
    bind(Config.class).toInstance(config);
  }
}
