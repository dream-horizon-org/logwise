package com.logwise.orchestrator.common.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class MapUtils {

  /**
   * Return a new LinkedHashMap whose iteration order matches that of original `map`. And selecting
   * only the entries matched by `selector`
   */
  public static <E, R> Map<E, R> filter(Predicate<Map.Entry<E, R>> selector, Map<E, R> map) {
    return map.entrySet().stream()
        .filter(selector)
        .collect(StreamUtils.toLinkedHashMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  public static <E, R> Function<Map<E, R>, Map<E, R>> filter(Predicate<Map.Entry<E, R>> selector) {
    return (map) -> filter(selector, map);
  }

  /**
   * Return a new LinkedHashMap whose iteration order matches that of `keys`. And selecting only the
   * keys present in `keys`
   */
  public static <E, R> Map<E, R> pick(List<E> keys, Map<E, R> map) {
    return keys.stream()
        .filter(map::containsKey)
        .collect(StreamUtils.toLinkedHashMap(Function.identity(), map::get));
  }

  public static <E, R> Function<Map<E, R>, Map<E, R>> pick(List<E> keys) {
    return (map) -> pick(keys, map);
  }

  public static <E, R> List<R> values(Map<E, R> map) {
    return new ArrayList<>(map.values());
  }

  public static <E, R> List<E> keys(Map<E, R> map) {
    return new ArrayList<>(map.keySet());
  }

  public static <E, R> List<Map.Entry<E, R>> entries(Map<E, R> map) {
    return new ArrayList<>(map.entrySet());
  }

  public static <E, R, T> Map<E, T> map(Function<R, T> mapper, Map<E, R> map) {
    return map.entrySet().stream()
        .collect(
            StreamUtils.toLinkedHashMap(
                Map.Entry::getKey, entry -> mapper.apply(entry.getValue())));
  }

  public static <E, R, T> List<T> mapToList(BiFunction<E, R, T> mapper, Map<E, R> map) {
    return map.entrySet().stream()
        .map(entry -> mapper.apply(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  public static <E, R> LinkedHashMap<E, R> sort(
      Comparator<? super Map.Entry<E, R>> comparator, Map<E, R> map) {
    return map.entrySet().stream().sorted(comparator).collect(StreamUtils.toLinkedHashMap());
  }
}
