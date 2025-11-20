package com.logwise.orchestrator.common.util;

import io.reactivex.Single;
import io.vertx.core.Vertx;
import java.util.concurrent.CompletableFuture;
import lombok.experimental.UtilityClass;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

/**
 * Test version of CompletableFutureUtils that creates VertxCompletableFuture using a test Vertx
 * instance. This allows tests to run without requiring a Vertx context in the test thread.
 */
@UtilityClass
public class TestCompletableFutureUtils {
  private static Vertx testVertx;

  /** Initialize the test Vertx instance. Should be called in @BeforeClass. */
  public static void init(Vertx vertx) {
    testVertx = vertx;
  }

  /** Convert a single to completable future */
  public static <T> VertxCompletableFuture<T> fromSingle(Single<T> single) {
    CompletableFuture<T> future = new CompletableFuture<>();
    single.subscribe(future::complete, future::completeExceptionally);
    return VertxCompletableFuture.from(testVertx.getOrCreateContext(), future);
  }
}
