package com.dream11.logcentralorchestrator;

import com.dream11.logcentralorchestrator.common.app.AbstractApplication;
import com.dream11.logcentralorchestrator.common.app.Deployable;
import com.dream11.logcentralorchestrator.common.app.VerticleConfig;
import com.dream11.logcentralorchestrator.config.*;
import com.dream11.logcentralorchestrator.module.ClientModule;
import com.dream11.logcentralorchestrator.module.MainModule;
import com.dream11.logcentralorchestrator.verticle.RestVerticle;
import com.google.inject.Module;
import io.vertx.reactivex.core.Vertx;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

@Slf4j
public class MainApplication extends AbstractApplication {

  @NonFinal private ApplicationConfig applicationConfig;

  public MainApplication() {
    super();
    initApplicationConfig();
  }

  public static void main(String[] args) {
    try {
      MainApplication app = new MainApplication();
      app.rxStartApplication().subscribe();
    } catch (Exception e) {
      log.error("Failed to start application", e);
      throw e;
    }
  }

  @Override
  protected Module[] getGoogleGuiceModules(Vertx vertx) {
    return ArrayUtils.toArray(new MainModule(vertx), new ClientModule(vertx));
  }

  @Override
  protected Deployable[] getVerticlesToDeploy(Vertx vertx) {
    return ArrayUtils.toArray(
        new Deployable(
            VerticleConfig.builder().instances(getEventLoopSize()).verticleType(0).build(),
            RestVerticle.class));
  }

  @Override
  protected void initDDClient() {
    // Datadog client initialization removed
  }

  protected void initApplicationConfig() {
    ApplicationConfigProvider.initConfig();
    this.applicationConfig = ApplicationConfigProvider.getApplicationConfig();
  }
}
