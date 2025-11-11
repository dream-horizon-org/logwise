package com.dream11.logcentralorchestrator;

import com.dream11.logcentralorchestrator.common.util.CompletableFutureUtils;
import com.dream11.logcentralorchestrator.common.util.ConfigUtils;
import com.dream11.logcentralorchestrator.common.util.SharedDataUtils;
import com.github.benmanes.caffeine.cache.AsyncCacheLoader;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import java.util.Objects;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class CaffeineCacheFactory {

  private static final String DEFAULT_FOLDER_NAME = "__default__";

  // Allow only Single to be stored in Cache to prevent computations on ForkJoinPool.commonPool()
  public static <K, V> Cache<K, Single<V>> createCache(Vertx vertx, String name) {
    return createCache(vertx, name, DEFAULT_FOLDER_NAME);
  }

  public static <K, V> Cache<K, Single<V>> createCache(
      Vertx vertx, String name, String configType) {
    return SharedDataUtils.getOrCreate(
        vertx, name, () -> buildCaffeine(readCaffeineConfig(configType)).build());
  }

  public static <K, V> Cache<K, Single<V>> createCache(
      Vertx vertx, String name, CaffeineConfig caffeineConfig) {
    return SharedDataUtils.getOrCreate(vertx, name, () -> buildCaffeine(caffeineConfig).build());
  }

  // Allow only Single to be stored in Cache to prevent computations on ForkJoinPool.commonPool()
  public static <K, V> LoadingCache<K, Single<V>> createLoadingCache(
      Vertx vertx, String name, CacheLoader<K, Single<V>> cacheLoader) {
    return createLoadingCache(vertx, name, cacheLoader, DEFAULT_FOLDER_NAME);
  }

  public static <K, V> LoadingCache<K, Single<V>> createLoadingCache(
      Vertx vertx, String name, CacheLoader<K, Single<V>> cacheLoader, String configType) {
    return SharedDataUtils.getOrCreate(
        vertx, name, () -> buildCaffeine(readCaffeineConfig(configType)).build(cacheLoader));
  }

  public static <K, V> LoadingCache<K, Single<V>> createLoadingCache(
      Vertx vertx,
      String name,
      CacheLoader<K, Single<V>> cacheLoader,
      CaffeineConfig caffeineConfig) {
    return SharedDataUtils.getOrCreate(
        vertx, name, () -> buildCaffeine(caffeineConfig).build(cacheLoader));
  }

  public static <K, V> AsyncLoadingCache<K, V> createAsyncLoadingCache(
      Vertx vertx, String name, Function<K, Single<V>> computeFunction) {
    return createAsyncLoadingCache(vertx, name, computeFunction, DEFAULT_FOLDER_NAME);
  }

  public static <K, V> AsyncLoadingCache<K, V> createAsyncLoadingCache(
      Vertx vertx, String name, Function<K, Single<V>> computeFunction, String configType) {

    return SharedDataUtils.getOrCreate(
        vertx,
        name,
        () ->
            buildCaffeine(readCaffeineConfig(configType))
                .buildAsync(buildAsyncCacheLoader(computeFunction)));
  }

  public static <K, V> AsyncLoadingCache<K, V> createAsyncLoadingCache(
      Vertx vertx,
      String name,
      Function<K, Single<V>> computeFunction,
      CaffeineConfig caffeineConfig) {

    return SharedDataUtils.getOrCreate(
        vertx,
        name,
        () -> buildCaffeine(caffeineConfig).buildAsync(buildAsyncCacheLoader(computeFunction)));
  }

  public static <K, V> Cache<K, V> getCache(Vertx vertx, String name) {
    // assuming cache is already present at this time
    return SharedDataUtils.getOrCreate(
        vertx,
        name,
        () -> {
          throw new CacheNotFoundException("Cache with name : " + name + " " + "not found");
        });
  }

  public static <K, V> LoadingCache<K, V> getLoadingCache(Vertx vertx, String name) {
    // assuming cache is already present at this time
    return SharedDataUtils.getOrCreate(
        vertx,
        name,
        () -> {
          throw new CacheNotFoundException("LoadingCache with name : " + name + " " + "not found");
        });
  }

  public static <K, V> AsyncLoadingCache<K, V> getAsyncLoadingCache(Vertx vertx, String name) {
    // assuming cache is already present at this time
    return SharedDataUtils.getOrCreate(
        vertx,
        name,
        () -> {
          throw new CacheNotFoundException(
              "AsyncLoadingCache with name : " + name + " " + "not found");
        });
  }

  private static <K, V> Caffeine<K, V> buildCaffeine(CaffeineConfig caffeineConfig) {
    Caffeine caffeine =
        Caffeine.newBuilder()
            .executor(
                cmd -> {
                  Objects.requireNonNull(Vertx.currentContext());
                  Vertx.currentContext().runOnContext(v -> cmd.run());
                });

    if (caffeineConfig.getRefreshAfterWriteDuration() != null) {
      caffeine.refreshAfterWrite(
          caffeineConfig.getRefreshAfterWriteDuration(),
          caffeineConfig.getRefreshAfterWriteTimeUnit());
    }

    if (caffeineConfig.getExpireAfterWriteDuration() != null) {
      caffeine.expireAfterWrite(
          caffeineConfig.getExpireAfterWriteDuration(),
          caffeineConfig.getExpireAfterWriteTimeUnit());
    }

    if (caffeineConfig.getExpireAfterAccessDuration() != null) {
      caffeine.expireAfterAccess(
          caffeineConfig.getExpireAfterAccessDuration(),
          caffeineConfig.getExpireAfterAccessTimeUnit());
    }

    if (caffeineConfig.getInitialCapacity() != null) {
      caffeine.initialCapacity(caffeineConfig.getInitialCapacity());
    }
    if (caffeineConfig.getMaximumSize() != null) {
      caffeine.maximumSize(caffeineConfig.getMaximumSize());
    }
    if (caffeineConfig.getWeakKeys()) {
      caffeine.weakKeys();
    }
    if (caffeineConfig.getWeakValues()) {
      caffeine.weakValues();
    }
    if (caffeineConfig.getSoftValues()) {
      caffeine.softValues();
    }
    return caffeine;
  }

  private static <K, V> AsyncCacheLoader<K, V> buildAsyncCacheLoader(
      Function<K, Single<V>> computeFunction) {
    return ((key, executor) -> computeFunction.apply(key).to(CompletableFutureUtils::fromSingle));
  }

  private static CaffeineConfig readCaffeineConfig(String configType) {
    String folderName =
        configType.equals(DEFAULT_FOLDER_NAME) ? "caffeine" : "caffeine-" + configType;
    return ConfigUtils.fromConfigFile(
        "config/" + folderName + "/caffeine-%s.conf", CaffeineConfig.class);
  }
}
