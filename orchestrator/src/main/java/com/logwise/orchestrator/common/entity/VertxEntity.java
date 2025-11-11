package com.logwise.orchestrator.common.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;
import java.util.Map;
import lombok.val;

public abstract class VertxEntity {

  // TODO:  Ideally make this private to force subclasses to implement a constructor (one that
  // accepts JsonObject)
  public VertxEntity() {}

  public VertxEntity(JsonObject json) {
    // DatabindCodec.mapper().convertValue(json, this.getClass());
  }

  public JsonObject toJson() {
    return toJson(false);
  }

  public JsonObject toJson(boolean skipNull) {
    if (skipNull) {
      val mapper =
          DatabindCodec.mapper().copy().setSerializationInclusion(JsonInclude.Include.NON_NULL);
      val map = mapper.convertValue(this, Map.class);
      return new JsonObject(map);
    } else {
      return JsonObject.mapFrom(this);
    }
  }
}
