package com.logwise.orchestrator.common.util;

import io.reactivex.CompletableTransformer;
import java.util.concurrent.atomic.AtomicLong;
import lombok.val;
import org.slf4j.Logger;

public final class CompletableUtils {

  /** Operator which adds debug logs to a Maybe. */
  public static CompletableTransformer applyDebugLogs(Logger log, String logPrefix) {
    AtomicLong startTime = new AtomicLong();
    return observable ->
        observable
            .doOnSubscribe(
                disposable -> {
                  startTime.set(System.currentTimeMillis());
                  log.debug("{} Subscribed", logPrefix);
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

  public static CompletableTransformer applyDebugLogs(Logger log) {
    val logPrefix = StackUtils.getCallerName();
    return applyDebugLogs(log, logPrefix);
  }
}
