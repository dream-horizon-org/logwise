package com.logwise.spark.configs;

import com.logwise.spark.constants.Constants;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ApplicationConfig {
  private Config system;
  private Config environment;
  private Config properties;

  private static ApplicationConfig init(String... configargs) {
    ApplicationConfig applicationConfig = new ApplicationConfig();
    ConfigFactory.invalidateCaches();
    applicationConfig.system = ConfigFactory.systemProperties();
    applicationConfig.environment = ConfigFactory.systemEnvironment();
    applicationConfig.properties = new Builder().build(configargs);
    return applicationConfig;
  }

  public static Config getConfig(String... args) {
    return init(args).getConfigProperties();
  }

  public Config getSystemProperties() {
    return system;
  }

  public Config getSystemEnvironment() {
    return environment;
  }

  public Config getConfigProperties() {
    return properties;
  }

  private static class Builder {
    private Builder() {
    }

    private Config build(String... configargs) {

      Config appConfig = ConfigFactory.empty();

      for (String conf : configargs) {
        Config argsConfig = ConfigFactory.parseString(conf);
        appConfig = appConfig.withFallback(argsConfig);
      }

      // Provide default value for X-Tenant-Name if not set in system properties or
      // environment
      // This must be in the config tree before file config is loaded so substitutions
      // can resolve
      String defaultTenantName = System.getProperty("X-Tenant-Name",
          System.getenv().getOrDefault("X-Tenant-Name", "default-tenant"));
      Config defaultTenantConfig = ConfigFactory.parseString(
          String.format("X-Tenant-Name = \"%s\"", defaultTenantName));
      appConfig = appConfig.withFallback(defaultTenantConfig);

      Config configFromDefaultConfFile = ConfigFactory.parseResources(
          String.format(
              "%s%sapplication.conf", Constants.APPLICATION_CONFIG_DIR, File.separator))
          .withFallback(appConfig);

      appConfig = appConfig.withFallback(configFromDefaultConfFile);
      appConfig = appConfig.resolve();

      return appConfig;
    }
  }
}
