package com.logwise.orchestrator.common.util;

import io.vertx.core.impl.ContextInternal;
import io.vertx.reactivex.core.Context;
import io.vertx.reactivex.core.Vertx;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ContextUtils {
  private static final String CONTEXT_INSTANCE_PREFIX = "__vertx.contextUtils.";
  private static final String CONTEXT_CLASS_PREFIX = "__class.";
  private static final String CONTEXT_DEFAULT_KEY = "__default.";
  private static final String DELIMETER = "#";

  /** Shared across verticle instance. */
  public static <T> T getInstance(Context context, Class<T> clazz, String key) {
    return context.get(
        CONTEXT_INSTANCE_PREFIX + CONTEXT_CLASS_PREFIX + clazz.getName() + DELIMETER + key);
  }

  public static <T> T getInstance(Context context, Class<T> clazz) {
    return getInstance(context, clazz, CONTEXT_DEFAULT_KEY);
  }

  public static <T> T getInstance(Class<T> clazz, String key) {
    return getInstance(Vertx.currentContext(), clazz, key);
  }

  public static <T> T getInstance(Class<T> clazz) {
    return getInstance(Vertx.currentContext(), clazz);
  }

  public static <T> Map<String, T> getInstances(Class<T> clazz) {
    Map<String, T> dbClientMap = new HashMap<>();
    ((ContextInternal) Vertx.currentContext().getDelegate())
        .contextData()
        .forEach(
            (key, value) -> {
              String tKey = (String) key;
              if (tKey.startsWith(
                  CONTEXT_INSTANCE_PREFIX + CONTEXT_CLASS_PREFIX + clazz.getName() + DELIMETER)) {
                dbClientMap.put(tKey.substring(tKey.lastIndexOf(DELIMETER) + 1), (T) value);
              }
            });
    return dbClientMap;
  }

  /**
   * Accessible from anywhere in this verticle instance. Note: This has to be set from one of the
   * VertxThreads (may cause NullPointerException otherwise) We are intentionally avoiding
   * vertx.getOrCreateContext() to ensure better coding practices
   */
  public static <T> void setInstance(Context context, T object, String key) {
    context.put(
        CONTEXT_INSTANCE_PREFIX
            + CONTEXT_CLASS_PREFIX
            + object.getClass().getName()
            + DELIMETER
            + key,
        object);
  }

  public static <T> void setInstance(Context context, T object) {
    setInstance(context, object, CONTEXT_DEFAULT_KEY);
  }

  public static <T> void setInstance(T object, String key) {
    setInstance(Vertx.currentContext(), object, key);
  }

  public static <T> void setInstance(T object) {
    setInstance(Vertx.currentContext(), object);
  }
}
