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
    applicationConfig.properties = new Builder(applicationConfig).build(configargs);
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
    private final ApplicationConfig applicationConfig;

    private Builder(ApplicationConfig applicationConfig) {
      this.applicationConfig = applicationConfig;
    }

    private Config build(String... configargs) {

      Config appConfig = ConfigFactory.empty();

      for (String conf : configargs) {
        log.info("Loading config from: {}", conf);
        Config argsConfig = parseConfigString(conf);
        log.info("Args config: {}", argsConfig);
        appConfig = appConfig.withFallback(argsConfig);
        log.info("Config loaded: {}", argsConfig);
      }

      Config configFromDefaultConfFile =
          ConfigFactory.parseResources(
                  String.format(
                      "%s%sapplication.conf", Constants.APPLICATION_CONFIG_DIR, File.separator))
              .withFallback(appConfig);

      appConfig = appConfig.withFallback(configFromDefaultConfFile);
      appConfig = appConfig.resolve();

      return appConfig;
    }

    /**
     * Parses a configuration string, handling both HOCON format (key: value) and
     * properties format (key=value). For properties format, converts to HOCON format
     * with proper quoting of values to handle special characters like colons.
     */
    private Config parseConfigString(String conf) {
      // Check if it's properties format (contains =)
      if (conf.contains("=")) {
        // Properties format: key=value
        // Convert to HOCON format: key="value" (quoted to handle special characters)
        int equalsIndex = conf.indexOf('=');
        if (equalsIndex > 0 && equalsIndex < conf.length() - 1) {
          String key = conf.substring(0, equalsIndex).trim();
          String value = conf.substring(equalsIndex + 1).trim();
          // Remove existing quotes if present to avoid double-quoting
          if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
          }
          // Quote the value to handle special characters like colons
          String hoconFormat = String.format("%s=\"%s\"", key, value);
          return ConfigFactory.parseString(hoconFormat);
        }
      }
      // Default: assume HOCON format
      return ConfigFactory.parseString(conf);
    }
  }
}
