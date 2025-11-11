package com.dream11.logcentralorchestrator.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfigProvider {

  private ObjectMapper objectMapper;

  public JsonObject getConfig(String path, String fileName) throws IOException {
    return new JsonObject(getConfig(path, fileName, Map.class));
  }

  public <T> T getConfig(String path, String fileName, Class<T> clazz) throws IOException {
    Config defaultConfig = ConfigFactory.load(String.format("%s/%s-default.conf", path, fileName));
    String resourcePath =
        String.format(
            "%s/%s-%s.conf", path, fileName, System.getProperty("app.environment", "default"));
    Config config =
        ConfigFactory.parseResources(resourcePath).withFallback(defaultConfig).resolve();
    return objectMapper.readValue(config.root().render(ConfigRenderOptions.concise()), clazz);
  }
}
