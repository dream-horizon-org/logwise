package com.logwise.orchestrator.common.app;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.AbstractVerticle;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * This class wraps Vert.x Verticle class and Verticle Deployment Configuration. <br>
 * Ref: <a href="https://vertx.io/docs/vertx-core/java/#_verticle_types">Verticle Types</a> <br>
 * Ref: <a
 * href="https://vertx.io/docs/vertx-core/java/#_deploying_verticles_programmatically">Deploying
 * Verticles Programmatically</a>
 */
@Data
@Slf4j
public class Deployable {

  private AbstractVerticle verticle;
  private VerticleConfig config = VerticleConfig.DEFAULT_CONFIG;
  private Class<? extends AbstractVerticle> verticleClass;
  private String name;

  @Deprecated
  public Deployable(AbstractVerticle verticle) {
    this.verticle = verticle;
    this.config = VerticleConfig.DEFAULT_CONFIG;
  }

  @Deprecated
  public Deployable(AbstractVerticle verticle, VerticleConfig config) {
    this.verticle = verticle;
    this.config = config;
  }

  public Deployable(Class<? extends AbstractVerticle> verticleClass) {
    this.verticleClass = verticleClass;
  }

  public Deployable(VerticleConfig config, Class<? extends AbstractVerticle> verticleClass) {
    this.config = config;
    this.verticleClass = verticleClass;
  }

  @JsonCreator
  public Deployable(
      @JsonProperty("verticleConfig") VerticleConfig config,
      @JsonProperty("verticleClass") String verticleClass,
      @JsonProperty("verticalName") String name)
      throws ClassNotFoundException {
    this.config = config;
    Class clazz = Class.forName(verticleClass);
    if (clazz.isAssignableFrom(AbstractVerticle.class)) {
      this.verticleClass = clazz;
    } else {
      log.error(
          "Invalid Verticle Class Name: {}, Not Extending io.vertx.core.AbstractVerticle.class",
          verticleClass);
      throw new RuntimeException(
          "Invalid Verticle Class Name:"
              + verticleClass
              + ", Not Extending io.vertx.core.AbstractVerticle.class");
    }
    this.name = name;
  }

  public AbstractVerticle getVerticle() {
    return verticle = AppContext.getInstance().getInstance(verticleClass);
  }
}
