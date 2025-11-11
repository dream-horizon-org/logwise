package com.dream11.logcentralorchestrator.common.util;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.MaybeTransformer;
import io.reactivex.functions.Function;
import java.util.concurrent.atomic.AtomicLong;
import lombok.val;
import org.slf4j.Logger;

public final class MaybeUtils {

  /** Simple read through cache abstraction. */
  public static <T> Maybe<T> readThroughCache(
      Maybe<T> getFromCache, Maybe<T> getFromSource, Function<T, Completable> saveToCache) {
    return Maybe.concat(
            getFromCache,
            getFromSource.flatMapSingleElement(
                data -> saveToCache.apply(data).toSingle(() -> data)))
        .firstElement();
  }

  /** Operator which adds debug logs to a Maybe. */
  public static <T> MaybeTransformer<T, T> applyDebugLogs(Logger log, String logPrefix) {
    AtomicLong startTime = new AtomicLong();
    return observable ->
        observable
            .doOnSubscribe(
                disposable -> {
                  startTime.set(System.currentTimeMillis());
                  log.debug("{} Subscribed", logPrefix);
                })
            .doOnSuccess(
                result -> {
                  long elapsedTime = System.currentTimeMillis() - startTime.get();
                  log.debug("{} Received after {}ms {}", logPrefix, elapsedTime, result);
                })
            .doOnComplete(
                () -> {
                  long elapsedTime = System.currentTimeMillis() - startTime.get();
                  log.debug("{} Completed after {}ms", logPrefix, elapsedTime);
                })
            .doOnError(
                err -> {
                  long elapsedTime = System.currentTimeMillis() - startTime.get();
                  log.error(
                      "{} Error after {}ms {}", logPrefix, elapsedTime, err.getMessage(), err);
                });
  }

  /** Operator which adds debug logs to a Maybe. */
  public static <T> MaybeTransformer<T, T> applyDebugLogs(Logger log) {
    val logPrefix = StackUtils.getCallerName();
    return applyDebugLogs(log, logPrefix);
  }
}
