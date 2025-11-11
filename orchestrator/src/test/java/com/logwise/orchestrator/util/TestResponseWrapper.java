package com.logwise.orchestrator.util;

import com.logwise.orchestrator.rest.io.Response;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import java.util.concurrent.CompletableFuture;
import lombok.experimental.UtilityClass;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

/**
 * Test version of ResponseWrapper that creates VertxCompletableFuture using a test Vertx instance.
 * This allows tests to run without requiring a Vertx context in the test thread.
 */
@UtilityClass
public class TestResponseWrapper {
  private static Vertx testVertx;

  /** Initialize the test Vertx instance. Should be called in @BeforeClass. */
  public static void init(Vertx vertx) {
    testVertx = vertx;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public <T> VertxCompletableFuture<Response<T>> fromMaybe(
      Maybe<T> source, T defaultValue, int httpStatusCode) {
    CompletableFuture<Response<T>> future = new CompletableFuture<>();
    source.subscribe(
        value -> future.complete(Response.successfulResponse(value, httpStatusCode)),
        future::completeExceptionally,
        () -> future.complete(Response.successfulResponse(defaultValue, httpStatusCode)));

    return VertxCompletableFuture.from(testVertx.getOrCreateContext(), future);
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public <T> VertxCompletableFuture<Response<T>> fromSingle(Single<T> source, int httpStatusCode) {
    CompletableFuture<Response<T>> future = new CompletableFuture<>();
    source.subscribe(
        value -> future.complete(Response.successfulResponse(value, httpStatusCode)),
        future::completeExceptionally);

    return VertxCompletableFuture.from(testVertx.getOrCreateContext(), future);
  }
}
