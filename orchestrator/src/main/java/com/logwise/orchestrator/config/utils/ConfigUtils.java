package com.logwise.orchestrator.config.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public final class ConfigUtils {

  public static <T> T fromConfigFile(@NonNull String confFilePathFormat, Class<T> klass) {
    Config config = fromConfigFile(confFilePathFormat);
    T typedConfig = ConfigBeanFactory.create(config, klass);
    log.debug("Loaded Config: {}", typedConfig);
    return typedConfig;
  }

  private static Config fromConfigFile(@NonNull String confFilePathFormat) {
    ConfigFactory.invalidateCaches();
    val envFile = String.format(confFilePathFormat, getAppEnvironment());
    val defaultFile = String.format(confFilePathFormat, "default");
    Config config =
        ConfigFactory.load(envFile)
            .withFallback(
                ConfigFactory.load(
                    defaultFile,
                    ConfigParseOptions.defaults().setAllowMissing(true),
                    ConfigResolveOptions.defaults()))
            .resolve();
    log.debug("Loading config from file {} : {}", confFilePathFormat, config);
    return config;
  }

  private static String getAppEnvironment() {
    return System.getProperty("app.environment", "dev");
  }
}
