package com.logwise.orchestrator.common.util;

import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.reactivex.SingleHelper;
import java.util.concurrent.CompletableFuture;
import me.escoffier.vertx.completablefuture.VertxCompletableFuture;

public final class CompletableFutureUtils {

  /** Convert a single to completable future */
  public static <T> VertxCompletableFuture<T> fromSingle(Single<T> single) {

    VertxCompletableFuture<T> vertxCompletableFuture = new VertxCompletableFuture<>();
    single.subscribe(
        vertxCompletableFuture::complete, vertxCompletableFuture::completeExceptionally);
    return vertxCompletableFuture;
  }

  /** Convert a completable future to single */
  public static <T> Single<T> toSingle(CompletableFuture<T> completableFuture) {
    return toSingle(VertxCompletableFuture.from(Vertx.currentContext(), completableFuture));
  }

  /** Convert a vertx completable future to single */
  public static <T> Single<T> toSingle(VertxCompletableFuture<T> vertxCompletableFuture) {
    return SingleHelper.toSingle(
        asyncResultHandler -> vertxCompletableFuture.toFuture().onComplete(asyncResultHandler));
  }
}
