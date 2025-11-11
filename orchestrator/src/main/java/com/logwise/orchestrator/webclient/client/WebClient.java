package com.logwise.orchestrator.webclient.client;

import com.logwise.orchestrator.common.util.ConfigUtils;
import com.logwise.orchestrator.webclient.client.impl.WebClientImpl;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Vertx;

@VertxGen
public interface WebClient extends AutoCloseable {
  /** Creates and configures a new object ready to send http requests from. */
  static WebClient create(Vertx vertx) {
    return create(vertx, null);
  }

  static WebClient create(Vertx vertx, String configType) {
    // TODO: Move to vertx-config based initialization once we start using it
    String folderName = configType == null ? "webclient" : "webclient-" + configType;
    return new WebClientImpl(
        vertx,
        ConfigUtils.fromConfigFile(
            "config/" + folderName + "/webclient-%s.conf", WebClientConfig.class));
  }

  /** Return a WebClient Note: Call this every time you want to send http requests. */
  io.vertx.ext.web.client.WebClient getWebClient();

  /** Close the WebClient. */
  void close();
}
