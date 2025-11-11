package com.logwise.orchestrator.common.util;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ListUtils {
  // TODO: Move ListUtils and MapUtils usages to CollectionUtils when possible

  public static <E, R> List<R> map(Function<E, R> mapper, List<E> list) {
    return CollectionUtils.mapToList(mapper, list);
  }

  public static <E, R> Function<List<E>, List<R>> map(Function<E, R> mapper) {
    return list -> map(mapper, list);
  }

  public static <R> List<R> filter(Predicate<R> selector, List<R> list) {
    return list.stream().filter(selector).collect(Collectors.toList());
  }

  public static <R> Function<List<R>, List<R>> filter(Predicate<R> selector) {
    return list -> filter(selector, list);
  }
}
