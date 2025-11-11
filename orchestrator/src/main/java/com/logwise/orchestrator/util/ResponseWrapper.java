package com.logwise.orchestrator.util;

import com.logwise.orchestrator.rest.io.Response;
import io.reactivex.Maybe;
import io.reactivex.Single;
import lombok.experimental.UtilityClass;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

@UtilityClass
public class ResponseWrapper {
  @SuppressWarnings("ResultOfMethodCallIgnored")
  public <T> VertxCompletableFuture<Response<T>> fromMaybe(
      Maybe<T> source, T defaultValue, int httpStatusCode) {
    VertxCompletableFuture<Response<T>> future = new VertxCompletableFuture<>();
    source.subscribe(
        value -> future.complete(Response.successfulResponse(value, httpStatusCode)),
        future::completeExceptionally,
        () -> future.complete(Response.successfulResponse(defaultValue, httpStatusCode)));
    return future;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public <T> VertxCompletableFuture<Response<T>> fromSingle(Single<T> source, int httpStatusCode) {
    VertxCompletableFuture<Response<T>> future = new VertxCompletableFuture<>();
    source.subscribe(
        value -> future.complete(Response.successfulResponse(value, httpStatusCode)),
        future::completeExceptionally);
    return future;
  }
}
