package com.logwise.spark.base;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Helper class for creating mock Config objects for testing. */
public class MockConfigHelper {

  /**
   * Creates a Config from a map of key-value pairs. Note: For simple key-value pairs, use
   * createNestedConfig instead.
   *
   * @param configMap Map of configuration keys to values
   * @return Config object
   */
  public static Config createConfig(Map<String, Object> configMap) {
    // For flat maps, create a HOCON string
    StringBuilder configString = new StringBuilder();
    for (Map.Entry<String, Object> entry : configMap.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof String) {
        configString.append(key).append("=\"").append(value).append("\"\n");
      } else if (value instanceof List) {
        configString.append(key).append("=[");
        List<?> list = (List<?>) value;
        for (int i = 0; i < list.size(); i++) {
          if (i > 0) configString.append(",");
          configString.append("\"").append(list.get(i)).append("\"");
        }
        configString.append("]\n");
      } else {
        configString.append(key).append("=").append(value).append("\n");
      }
    }
    return ConfigFactory.parseString(configString.toString());
  }

  /**
   * Creates a Config with nested structure.
   *
   * @param configMap Map of configuration keys to values (supports nested maps)
   * @return Config object
   */
  public static Config createNestedConfig(Map<String, Object> configMap) {
    return ConfigFactory.parseMap(configMap);
  }

  /**
   * Creates a minimal test config with common Spark settings.
   *
   * @return Config object with basic Spark configuration
   */
  public static Config createMinimalSparkConfig() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("app.job.name", "PUSH_LOGS_TO_S3");
    configMap.put("logCentral.orchestrator.url", "http://localhost:8081");
    configMap.put("kafka.bootstrap.servers.port", "9092");
    configMap.put("tenant.name", "test-tenant");
    configMap.put("spark.streamingquery.timeout.minutes", 60);
    configMap.put("spark.streams.name", java.util.Arrays.asList("APPLICATION_LOGS_STREAM_TO_S3"));
    return createNestedConfig(configMap);
  }

  /**
   * Creates an empty Config object.
   *
   * @return Empty Config object
   */
  public static Config createEmptyConfig() {
    return ConfigFactory.empty();
  }
}
