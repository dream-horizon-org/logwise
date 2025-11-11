package com.logwise.orchestrator;

import com.google.inject.Module;
import com.logwise.orchestrator.common.app.AbstractApplication;
import com.logwise.orchestrator.common.app.Deployable;
import com.logwise.orchestrator.common.app.VerticleConfig;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.config.ApplicationConfigProvider;
import com.logwise.orchestrator.module.ClientModule;
import com.logwise.orchestrator.module.MainModule;
import com.logwise.orchestrator.verticle.RestVerticle;
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
