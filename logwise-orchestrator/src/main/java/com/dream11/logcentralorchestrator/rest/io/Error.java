package com.dream11.logcentralorchestrator.rest.io;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.json.JsonObject;
import lombok.Data;

@Data
public class Error {
  private final String code;
  private final String message;

  @JsonCreator
  Error(@JsonProperty("code") String code, @JsonProperty("message") String message) {
    this.code = code;
    this.message = message;
  }

  public static Error of(String code, String message) {
    return new Error(code, message);
  }

  @JsonIgnore
  public String toJsonString() {
    return new JsonObject()
        .put("error", new JsonObject().put("code", code).put("message", message))
        .encode();
  }
}
