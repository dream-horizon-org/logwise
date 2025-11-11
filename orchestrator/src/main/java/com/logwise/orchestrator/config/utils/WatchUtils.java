package com.logwise.orchestrator.config.utils;

import com.logwise.orchestrator.config.client.ConfigOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.consul.ConsulClientOptions;
import io.vertx.ext.consul.KeyValue;
import io.vertx.ext.consul.Watch;
import io.vertx.ext.consul.WatchResult;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public final class WatchUtils {

  public static Watch<KeyValue> setConsulKeyWatch(
      Vertx vertx, String key, Long timeoutSeconds, Handler<WatchResult<KeyValue>> handler) {
    ConfigOptions configOptions =
        ConfigUtils.fromConfigFile("config/config-client/config-%s.conf", ConfigOptions.class);
    ConsulClientOptions options =
        new ConsulClientOptions(configOptions.retrieveConsulClientOptions())
            .setTimeout(timeoutSeconds * 1000);
    log.debug("ConsulClientOptions: {} \nKey: {}", options.toJson().toString(), key);
    return Watch.key(key, vertx, options).setHandler(handler).start();
  }

  public static Watch<KeyValue> setConsulKeyWatch(
      Vertx vertx, String key, Handler<WatchResult<KeyValue>> handler) {
    return setConsulKeyWatch(vertx, key, 45L, handler); // 45 seconds timeout
  }
}
