package com.logwise.orchestrator.common.util;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.SingleTransformer;
import io.reactivex.functions.Function;
import java.util.concurrent.atomic.AtomicLong;
import lombok.val;
import org.slf4j.Logger;

public final class SingleUtils {

  /** Simple read through cache abstraction. */
  public static <T> Single<T> readThroughCache(
      Maybe<T> getFromCache, Single<T> getFromSource, Function<T, Completable> saveToCache) {
    return MaybeUtils.readThroughCache(getFromCache, getFromSource.toMaybe(), saveToCache)
        .toSingle();
  }

  /** Operator which adds debug logs to a Single. */
  public static <T> SingleTransformer<T, T> applyDebugLogs(Logger log, String logPrefix) {
    AtomicLong startTime = new AtomicLong();
    return single ->
        single
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
            .doOnError(
                err -> {
                  long elapsedTime = System.currentTimeMillis() - startTime.get();
                  log.error(
                      "{} Error after {}ms {}", logPrefix, elapsedTime, err.getMessage(), err);
                });
  }

  /** Operator which adds debug logs to a Single. */
  public static <T> SingleTransformer<T, T> applyDebugLogs(Logger log) {
    val logPrefix = StackUtils.getCallerName();
    return applyDebugLogs(log, logPrefix);
  }
}
