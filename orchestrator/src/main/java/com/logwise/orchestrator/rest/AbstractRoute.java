package com.logwise.orchestrator.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.orchestrator.common.app.AppContext;
import com.logwise.orchestrator.rest.exception.RestException;
import com.logwise.orchestrator.rest.io.Error;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.serviceproxy.ServiceException;
import java.util.*;
import java.util.stream.Stream;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public abstract class AbstractRoute<T> implements Handler<RoutingContext> {

  private static final Error INVALID_PARAM_ERROR = Error.of("MG1000", "Missing Parameters");

  private static final RestException INVALID_REST_EXCEPTION =
      new RestException("Invalid Or Missing Request Params.", INVALID_PARAM_ERROR);

  protected ObjectMapper objectMapper;
  private String path;
  private HttpMethod httpMethod;
  private String produces;
  private String consumes;
  private List<String> requiredHeaders;
  private List<String> requiredQueryParams;
  private List<String> requiredBodyParams;
  private long timeout;

  public AbstractRoute() {
    this.objectMapper = AppContext.getInstance(ObjectMapper.class);
  }

  private void prepareResponse(RoutingContext context, T response, long startTime) {
    if (!context.response().ended()) {
      try {
        setResponseHeader(context.response(), response).end(RestUtil.getString(response));
        log.info(
            "[RESPONSE TIME] Time taken for route: {} : {}",
            path,
            (System.currentTimeMillis() - startTime));
      } catch (JsonProcessingException e) {
        handleError(context, e);
      }
    }
  }

  protected HttpServerResponse setResponseHeader(HttpServerResponse httpResponse, T response) {
    httpResponse.putHeader("content-type", produces);
    return httpResponse;
  }

  protected Single<Request> validateRequest(Request request) {
    try {
      if (!httpMethod.equals(HttpMethod.GET)) {
        validateRequestBody(request.getBody());
      }
      validateRequestHeaders(request.getHeaders());
      validateRequestQueryParams(request.getQueryParams());
      return Single.just(request);
    } catch (Exception e) {
      log.error(
          "Error in request! {}, , headers required : {}, headers received : {}",
          path,
          requiredHeaders,
          request.getHeaders(),
          e);
      return Single.error(e);
    }
  }

  protected void validateRequestBody(final JsonObject jsonObject) throws Exception {
    if (!Optional.ofNullable(getRequiredBodyParams()).orElse(new ArrayList<>()).stream()
        .allMatch(val -> jsonObject.containsKey(val))) {
      throw INVALID_REST_EXCEPTION;
    }
  }

  protected void validateRequestQueryParams(final MultiMap queryParams) throws Exception {
    if (!Optional.ofNullable(getRequiredQueryParams()).orElse(new ArrayList<>()).stream()
        .allMatch(queryParams::contains)) {
      throw INVALID_REST_EXCEPTION;
    }
  }

  protected void validateRequestHeaders(MultiMap headers) throws Exception {
    if (!Optional.ofNullable(getRequiredHeaders()).orElse(new ArrayList<>()).stream()
        .allMatch(headers::contains)) {
      throw INVALID_REST_EXCEPTION;
    }
  }

  private Map<String, String> getMap(MultiMap multiMap) {
    Map<String, String> map = new HashMap<>();
    for (Map.Entry<String, String> entry : multiMap.entries()) {
      map.put(entry.getKey(), entry.getValue());
    }
    return map;
  }

  public abstract Single<T> handle(Request request);

  @Override
  public void handle(final RoutingContext routingContext) {
    final long startTime = System.currentTimeMillis();
    log.info("STATED REQUEST : {}", path);

    final Request request =
        new Request(
            routingContext,
            routingContext.request().headers(),
            routingContext.pathParams(),
            routingContext.queryParams(),
            getBody(routingContext));
    log.info("Path: {}  Request: {}", path, request);

    validateRequest(request)
        .flatMap(this::handle)
        .subscribe(
            success -> prepareResponse(routingContext, success, startTime),
            error -> handleError(routingContext, error));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AbstractRoute<?> that = (AbstractRoute<?>) o;
    return Objects.equals(path, that.path) && httpMethod == that.httpMethod;
  }

  @Override
  public int hashCode() {
    return Objects.hash(path, httpMethod);
  }

  private JsonObject getBody(RoutingContext context) {
    if (Stream.of(HttpMethod.GET).anyMatch(method -> httpMethod == method)) {
      return null;
    } else if ("application/x-www-form-urlencoded"
        .equalsIgnoreCase(context.request().headers().get("Content-Type"))) {
      Map map = getMap(context.request().formAttributes());
      return new JsonObject(map);
    } else {
      return context.getBodyAsJson();
    }
  }

  protected Throwable handleError(Throwable throwable) {
    return throwable;
  }

  private void handleError(RoutingContext context, Throwable throwable) {
    if (!context.response().ended()) {
      log.error("Error in route handler! " + toString(), throwable);
      throwable = this.handleError(throwable);
      if (throwable instanceof RestException) {
        RestException e = ((RestException) throwable);
        context
            .response()
            .putHeader("content-type", "application/json")
            .setStatusCode(e.getHttpStatusCode())
            .end(e.toJson().toString());
      } else if (throwable instanceof ServiceException) {
        ServiceException e = ((ServiceException) throwable);
        context
            .response()
            .putHeader("content-type", "application/json")
            .setStatusCode(e.failureCode())
            .end(e.getMessage());
      } else {
        context
            .response()
            .putHeader("content-type", "application/json")
            .setStatusCode(500)
            .end(throwable.getMessage());
      }
    }
  }
}
