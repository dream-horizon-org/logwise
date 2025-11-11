package com.dream11.logcentralorchestrator.common.util;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigParseOptions;
import com.typesafe.config.ConfigResolveOptions;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public final class ConfigUtils {

  public static ConfigRetriever getRetriever(Vertx vertx, String confFilePathFormat) {
    ConfigFactory.invalidateCaches();
    ConfigStoreOptions hoconStore =
        new ConfigStoreOptions().setType("file").setFormat("d11hocon").setOptional(true);
    ConfigStoreOptions defaultStore =
        new ConfigStoreOptions(hoconStore)
            .setConfig(new JsonObject().put("path", String.format(confFilePathFormat, "default")));
    ConfigStoreOptions environmentStore =
        new ConfigStoreOptions(hoconStore)
            .setConfig(
                new JsonObject()
                    .put("path", String.format(confFilePathFormat, getAppEnvironment())));
    int scanPeriod = Integer.parseInt(System.getProperty("d11.config.scanInterval", "0"));
    return ConfigRetriever.create(
        vertx,
        new ConfigRetrieverOptions()
            .addStore(defaultStore)
            .addStore(environmentStore)
            .setScanPeriod(scanPeriod * 1000L));
  }

  public static Config fromConfigFile(@NonNull String confFilePathFormat) {
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

  public static <T> T fromConfigFile(@NonNull String confFilePathFormat, Class<T> klass) {
    Config config = fromConfigFile(confFilePathFormat);
    T typedConfig = ConfigBeanFactory.create(config, klass);
    log.debug("Loaded Config: {}", typedConfig);
    return typedConfig;
  }

  private static String getAppEnvironment() {
    return System.getProperty("app.environment", "dev");
  }
}
