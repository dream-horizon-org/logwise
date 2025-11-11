package com.logwise.orchestrator.module;

import com.logwise.orchestrator.common.guice.VertxAbstractModule;
import com.logwise.orchestrator.common.util.ContextUtils;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.config.ApplicationConfigProvider;
import com.logwise.orchestrator.mysql.reactivex.client.MysqlClient;
import com.logwise.orchestrator.webclient.reactivex.client.WebClient;
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
