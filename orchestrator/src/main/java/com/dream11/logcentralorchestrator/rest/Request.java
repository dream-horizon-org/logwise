package com.dream11.logcentralorchestrator.rest;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@AllArgsConstructor
@Slf4j
public class Request {

  RoutingContext routingContext;
  MultiMap headers;
  Map<String, String> pathParams;
  MultiMap queryParams;
  JsonObject body;

  /** Returns the first header with name. */
  public String getHeader(String name) {
    return headers != null ? headers.get(name) : null;
  }

  public String getPathParam(String name) {
    return this.pathParams != null ? pathParams.get(name) : null;
  }

  /** Returns the first query param with name. */
  public String getQueryParam(String name) {
    return queryParams != null ? queryParams.get(name) : null;
  }

  public Object getBodyParam(String name) {
    return body != null ? body.getValue(name) : null;
  }

  public Vertx vertx() {
    return this.getRoutingContext().vertx();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("Request{");
    sb.append("body=").append(body != null ? body.toString() : "no body");
    sb.append(", headers=")
        .append(headers != null ? headers.toString().replace("\n", ", ") : "no headers");
    sb.append(", pathParams=")
        .append(pathParams != null ? pathParams.toString() : "no path params");
    sb.append(", queryParams=")
        .append(
            queryParams != null ? queryParams.toString().replace("\n", ", ") : "no query params");
    sb.append('}');
    return sb.toString();
  }
}
