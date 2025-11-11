package com.dream11.logcentralorchestrator.rest.exception;

import com.dream11.logcentralorchestrator.rest.io.Error;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;

public class RestException extends RuntimeException {

  private static final Map<String, String> oldErrorMap = new HashMap<>();
  private final Error error;
  private int httpStatusCode = 400;
  // Remove once all services start using standard error response
  @Setter private boolean oldErrorKeys = false;

  public RestException(Throwable cause, Error error, int httpStatusCode) {
    super(cause);
    this.error = error;
    this.httpStatusCode = httpStatusCode;
  }

  public RestException(String message, Error error, int httpStatusCode) {
    super(message);
    this.error = error;
    this.httpStatusCode = httpStatusCode;
  }

  public RestException(Throwable cause, Error error, int httpStatusCode, boolean oldErrorKeys) {
    super(cause);
    this.error = error;
    this.httpStatusCode = httpStatusCode;
    this.oldErrorKeys = oldErrorKeys;
  }

  public RestException(String message, Error error, int httpStatusCode, boolean oldErrorKeys) {
    super(message);
    this.error = error;
    this.httpStatusCode = httpStatusCode;
    this.oldErrorKeys = oldErrorKeys;
  }

  public RestException(Throwable cause, Error error) {
    super(cause);
    this.error = error;
  }

  public RestException(String message, Error error) {
    super(message);
    this.error = error;
  }

  public RestException(Throwable cause, Error error, boolean oldErrorKeys) {
    super(cause);
    this.error = error;
    this.oldErrorKeys = oldErrorKeys;
  }

  public RestException(String message, Error error, boolean oldErrorKeys) {
    super(message);
    this.error = error;
    this.oldErrorKeys = oldErrorKeys;
  }

  public RestException(RestError restError) {
    super(restError.getErrorMessage());
    this.error = restError.getError();
    this.httpStatusCode = restError.getHttpStatusCode();
  }

  public RestException(RestError restError, Throwable cause) {
    super(cause);
    this.error = restError.getError();
    this.httpStatusCode = restError.getHttpStatusCode();
  }

  public static void mergeIntoOldErrorMap(Map<String, String> map) {
    map.forEach((k, v) -> oldErrorMap.merge(k, v, (v1, v2) -> v2));
  }

  public Error getError() {
    return error;
  }

  public int getHttpStatusCode() {
    return httpStatusCode;
  }

  public JsonObject toJson() {
    JsonObject errorJson =
        new JsonObject()
            .put("message", this.error.getMessage())
            .put("cause", this.getMessage())
            .put("code", this.error.getCode());

    if (this.oldErrorKeys) {
      errorJson
          .put("MsgCode", this.error.getCode())
          .put("MsgActionTitle", "OK")
          .put("MsgTitle", "Error");
      if (oldErrorMap.containsKey(error.getCode())) {
        errorJson.put("MsgText", oldErrorMap.get(this.error.getCode()));
      } else {
        errorJson.put("MsgText", this.error.getMessage());
      }
    }
    return new JsonObject().put("error", errorJson);
  }

  @Override
  public String toString() {
    return this.toJson().toString();
  }
}
