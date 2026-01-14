package com.logwise.orchestrator.service.example;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.inject.Inject;
import com.logwise.orchestrator.CaffeineCacheFactory;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import lombok.extern.slf4j.Slf4j;

/**
 * Example showing how to use existing CaffeineCacheFactory for synchronous getter/setter operations.
 *
 * <p>Note: The existing createCache() returns Cache<K, Single<V>>, so we wrap/unwrap Single objects.
 * This works fine for synchronous operations - just use Single.just() and blockingGet().
 */
@Slf4j
public class SyncCacheExample {

  // Cache that stores Single<String> values
  private final Cache<String, Single<String>> configCache;

  // Cache that stores Single<MyData> values
  private final Cache<String, Single<MyData>> dataCache;

  @Inject
  public SyncCacheExample(Vertx vertx) {
    // Create cache using existing factory method
    this.configCache = CaffeineCacheFactory.createCache(vertx, "config-cache-example");
    this.dataCache = CaffeineCacheFactory.createCache(vertx, "data-cache-example");
  }

  // ========== Getter Methods ==========

  /**
   * Get configuration value from cache.
   *
   * @param key Configuration key
   * @return Configuration value if found, null otherwise
   */
  public String getConfig(String key) {
    Single<String> single = configCache.getIfPresent(key);
    if (single != null) {
      // Unwrap Single to get the actual value (blocking call)
      return single.blockingGet();
    }
    return null;
  }

  /**
   * Get data from cache.
   *
   * @param key Data key
   * @return MyData if found, null otherwise
   */
  public MyData getData(String key) {
    Single<MyData> single = dataCache.getIfPresent(key);
    if (single != null) {
      return single.blockingGet();
    }
    return null;
  }

  /**
   * Get configuration with default value if not found.
   *
   * @param key Configuration key
   * @param defaultValue Default value to return if not cached
   * @return Configuration value or default
   */
  public String getConfigOrDefault(String key, String defaultValue) {
    Single<String> single = configCache.getIfPresent(key);
    if (single != null) {
      return single.blockingGet();
    }
    return defaultValue;
  }

  // ========== Setter Methods ==========

  /**
   * Store configuration value in cache.
   *
   * @param key Configuration key
   * @param value Configuration value
   */
  public void setConfig(String key, String value) {
    log.info("Caching config: {} = {}", key, value);
    // Wrap value in Single.just() before storing
    configCache.put(key, Single.just(value));
  }

  /**
   * Store data in cache.
   *
   * @param key Data key
   * @param data Data to cache
   */
  public void setData(String key, MyData data) {
    log.info("Caching data for key: {}", key);
    dataCache.put(key, Single.just(data));
  }

  // ========== Update Methods ==========

  /**
   * Update existing configuration or create new entry.
   *
   * @param key Configuration key
   * @param value New configuration value
   */
  public void updateConfig(String key, String value) {
    log.info("Updating config: {} = {}", key, value);
    // put() overwrites existing value or creates new entry
    configCache.put(key, Single.just(value));
  }

  /**
   * Update data in cache.
   *
   * @param key Data key
   * @param data Updated data
   */
  public void updateData(String key, MyData data) {
    log.info("Updating data for key: {}", key);
    dataCache.put(key, Single.just(data));
  }

  // ========== Delete Methods ==========

  /**
   * Remove configuration from cache.
   *
   * @param key Configuration key
   */
  public void removeConfig(String key) {
    log.info("Removing config from cache: {}", key);
    configCache.invalidate(key);
  }

  /**
   * Remove data from cache.
   *
   * @param key Data key
   */
  public void removeData(String key) {
    log.info("Removing data from cache: {}", key);
    dataCache.invalidate(key);
  }

  /**
   * Clear all cached configurations.
   */
  public void clearAllConfigs() {
    log.info("Clearing all config cache");
    configCache.invalidateAll();
  }

  /**
   * Clear all cached data.
   */
  public void clearAllData() {
    log.info("Clearing all data cache");
    dataCache.invalidateAll();
  }

  // ========== Helper Methods ==========

  /**
   * Check if key exists in cache.
   *
   * @param key Configuration key
   * @return true if key exists in cache
   */
  public boolean hasConfig(String key) {
    return configCache.getIfPresent(key) != null;
  }

  /**
   * Get cache statistics.
   *
   * @return Cache statistics as string
   */
  public String getCacheStats() {
    return String.format(
        "Config Cache - Size: %d, Stats: %s%nData Cache - Size: %d, Stats: %s",
        configCache.estimatedSize(),
        configCache.stats(),
        dataCache.estimatedSize(),
        dataCache.stats());
  }

  // ========== Example Data Class ==========

  /** Example data class */
  public static class MyData {
    private String name;
    private int value;

    public MyData(String name, int value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public int getValue() {
      return value;
    }

    public void setValue(int value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "MyData{name='" + name + "', value=" + value + "}";
    }
  }

  // ========== Usage Example ==========

  /**
   * Example usage method showing how to use the cache.
   */
  public void exampleUsage() {
    // Set values
    setConfig("database.url", "jdbc:mysql://localhost:3306/mydb");
    setConfig("database.timeout", "30");

    // Get values
    String dbUrl = getConfig("database.url");
    String timeout = getConfigOrDefault("database.timeout", "60");

    // Store complex objects
    setData("user:123", new MyData("John", 42));
    MyData user = getData("user:123");

    // Update values
    updateConfig("database.timeout", "45");
    updateData("user:123", new MyData("John", 43));

    // Check existence
    if (hasConfig("database.url")) {
      log.info("Database URL is cached");
    }

    // Remove values
    removeConfig("database.timeout");
    removeData("user:123");

    // Get statistics
    log.info("Cache stats: {}", getCacheStats());
  }
}

