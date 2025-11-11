package com.logwise.orchestrator.common.util;

import io.vertx.core.shareddata.Shareable;
import io.vertx.reactivex.core.Vertx;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public final class SharedDataUtils {

  private static final String SHARED_DATA_MAP_NAME = "__vertx.sharedDataUtils";
  private static final String SHARED_DATA__CLASS_PREFIX = "__class.";
  private static final String SHARED_DATA_DEFAULT_KEY = "__default.";

  /**
   * Returns a singleton shared object across vert.x instance Note: It is your responsibility to
   * ensure T returned by supplier is thread-safe
   */
  public static <T> T getOrCreate(Vertx vertx, String name, Supplier<T> supplier) {
    val singletons = vertx.getDelegate().sharedData().getLocalMap(SHARED_DATA_MAP_NAME);
    // LocalMap is internally backed by a ConcurrentMap
    return ((ThreadSafe<T>) singletons.computeIfAbsent(name, k -> new ThreadSafe(supplier.get())))
        .getObject();
  }

  public static <T> T getOrCreate(String name, Supplier<T> supplier) {
    return getOrCreate(Vertx.currentContext().owner(), name, supplier);
  }

  /**
   * Helper wrapper on getOrCreate to setInstance Note: Doesn't reset the instance if already exists
   */
  public static <T> T setInstance(Vertx vertx, T instance) {
    return getOrCreate(
        vertx,
        SHARED_DATA__CLASS_PREFIX + SHARED_DATA_DEFAULT_KEY + instance.getClass().getName(),
        () -> instance);
  }

  /** Helper wrapper on getOrCreate to getInstance */
  public static <T> T getInstance(Vertx vertx, Class<T> clazz) {
    return getOrCreate(
        vertx,
        SHARED_DATA__CLASS_PREFIX + SHARED_DATA_DEFAULT_KEY + clazz.getName(),
        () -> {
          throw new RuntimeException("Cannot find default instance of " + clazz.getName());
        });
  }

  static class ThreadSafe<T> implements Shareable {
    @Getter T object;

    public ThreadSafe(T object) {
      this.object = object;
    }
  }
}
