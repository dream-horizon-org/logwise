package com.logwise.orchestrator.common.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public final class StreamUtils {

  /** Converts a stream to LinkedHashMap. */
  public static <K, V> Collector<Map.Entry<K, V>, ?, LinkedHashMap<K, V>> toLinkedHashMap() {
    return StreamUtils.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue);
  }

  /** Converts a stream to LinkedHashMap. */
  public static <T, K, U> Collector<T, ?, LinkedHashMap<K, U>> toLinkedHashMap(
      Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends U> valueMapper) {
    return Collectors.toMap(
        keyMapper,
        valueMapper,
        (u, v) -> {
          throw new IllegalStateException(String.format("Duplicate key %s", u));
        },
        LinkedHashMap::new);
  }
}
