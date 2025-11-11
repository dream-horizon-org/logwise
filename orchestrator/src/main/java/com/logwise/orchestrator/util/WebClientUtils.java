package com.logwise.orchestrator.util;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.experimental.UtilityClass;

@UtilityClass
public class WebClientUtils {
  public Function<? super Flowable<Throwable>, Flowable<?>> retryWithDelay(
      int delay, TimeUnit delayTimeUnit, int maxAttempts) {
    AtomicInteger retryCount = new AtomicInteger();
    return errors ->
        errors.flatMap(
            err -> {
              if (retryCount.getAndIncrement() < maxAttempts) {
                return Flowable.timer(delay, delayTimeUnit);
              }
              return Flowable.error(err);
            });
  }
}
