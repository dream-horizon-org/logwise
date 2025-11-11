package com.dream11.logcentralorchestrator.common.lib;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import io.vertx.config.spi.ConfigProcessor;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import java.io.Reader;
import java.io.StringReader;

public class D11HoconProcessor implements ConfigProcessor {
  @Override
  public String name() {
    return "d11hocon";
  }

  @Override
  public void process(
      Vertx vertx,
      JsonObject configuration,
      Buffer input,
      Handler<AsyncResult<JsonObject>> handler) {
    vertx.executeBlocking(
        future -> {
          try (Reader reader = new StringReader(input.toString("UTF-8"))) {
            Config conf =
                ConfigFactory.systemProperties()
                    .withFallback(ConfigFactory.parseReader(reader))
                    .resolve();
            String output =
                conf.root()
                    .render(
                        ConfigRenderOptions.concise()
                            .setJson(true)
                            .setComments(false)
                            .setFormatted(false));
            String processedOutput =
                output.replaceAll(
                    ":\"([1-9][0-9]*)\"", ":$1"); // Convert string values to numbers when possible
            JsonObject json = new JsonObject(processedOutput);
            future.complete(json);
          } catch (Exception e) {
            future.fail(e);
          }
        },
        handler);
  }
}
