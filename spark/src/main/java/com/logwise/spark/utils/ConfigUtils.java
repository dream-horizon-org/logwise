package com.logwise.spark.utils;

import com.logwise.spark.constants.Constants;
import com.typesafe.config.Config;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ConfigUtils {

  public Map<String, String> getConfigMap(Config config, String key) {
    return config.hasPath(key)
        ? config.getConfig(key).entrySet().stream()
            .collect(
                Collectors.toMap(
                    entry -> ApplicationUtils.removeSurroundingQuotes(entry.getKey()),
                    entry -> entry.getValue().unwrapped().toString()))
        : Collections.emptyMap();
  }

  public Map<String, String> getSparkConfig(Config config) {
    return getConfigMap(config, Constants.CONFIG_KEY_SPARK_CONFIG);
  }

  public Map<String, String> getSparkHadoopConfig(Config config) {
    return getConfigMap(config, Constants.CONFIG_KEY_SPARK_HADOOP_CONFIG);
  }
}
