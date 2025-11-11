package com.dream11.logcentralorchestrator.module;

import com.dream11.logcentralorchestrator.common.guice.VertxAbstractModule;
import com.dream11.logcentralorchestrator.common.util.ContextUtils;
import com.dream11.logcentralorchestrator.config.ApplicationConfig;
import com.dream11.logcentralorchestrator.config.ApplicationConfigProvider;
import com.dream11.logcentralorchestrator.mysql.reactivex.client.MysqlClient;
import com.dream11.logcentralorchestrator.webclient.reactivex.client.WebClient;
import io.vertx.reactivex.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainModule extends VertxAbstractModule {

  private Vertx vertx;

  public MainModule(Vertx vertx) {
    super(vertx);
    this.vertx = vertx;
  }

  @Override
  protected void bindConfiguration() {
    log.info("Binding Main Modules Configuration...");
    bind(MysqlClient.class).toProvider(() -> ContextUtils.getInstance(MysqlClient.class));
    bind(WebClient.class).toProvider(() -> ContextUtils.getInstance(WebClient.class));
    bind(ApplicationConfig.class).toInstance(ApplicationConfigProvider.getApplicationConfig());
  }
}
