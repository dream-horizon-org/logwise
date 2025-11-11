package com.logwise.orchestrator.common.guice;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.logwise.orchestrator.common.util.ConfigProvider;
import io.vertx.reactivex.core.Vertx;

public class DefaultModule extends VertxAbstractModule {

  private ObjectMapper objectMapper;
  private final Vertx vertx;

  public DefaultModule(Vertx vertx) {
    super(vertx);
    this.vertx = vertx;
  }

  @Override
  protected void bindConfiguration() {
    bind(Vertx.class).toInstance(this.vertx);
    bind(io.vertx.core.Vertx.class).toInstance(this.vertx.getDelegate());
    bind(ObjectMapper.class).toInstance(getObjectMapper());
    bind(ConfigProvider.class).toInstance(getConfigProvider());
  }

  protected ObjectMapper getObjectMapper() {
    if (objectMapper == null) {
      objectMapper = new ObjectMapper();
      objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
      objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
      objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
      objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
    return objectMapper;
  }

  private ConfigProvider getConfigProvider() {
    return new ConfigProvider(getObjectMapper());
  }
}
