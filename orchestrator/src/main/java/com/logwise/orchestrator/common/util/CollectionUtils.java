package com.logwise.orchestrator.common.util;

import com.google.common.collect.Streams;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.ImmutablePair;

public final class CollectionUtils {

  public static <E, R> List<R> mapToList(Function<E, R> mapper, Collection<E> collection) {
    return collection.stream().map(mapper).collect(Collectors.toList());
  }

  public static <T> T reduce(T initial, BinaryOperator<T> reduceFn, Collection<T> collection) {
    return collection.stream().reduce(initial, reduceFn);
  }

  public static <E, R> Map<E, R> indexBy(Function<R, E> keyMapper, Collection<R> collection) {
    return collection.stream().collect(StreamUtils.toLinkedHashMap(keyMapper, Function.identity()));
  }

  public static <R> Boolean any(Predicate<R> cond, Collection<R> collection) {
    return collection.stream().map(cond::test).reduce(Boolean.FALSE, Boolean::logicalOr);
  }

  public static <R> Boolean all(Predicate<R> cond, Collection<R> collection) {
    return collection.stream().map(cond::test).reduce(Boolean.TRUE, Boolean::logicalAnd);
  }

  public static <E, R> LinkedHashMap<E, R> zipToMap(Collection<E> keys, Collection<R> values) {
    return Streams.zip(keys.stream(), values.stream(), ImmutablePair::new)
        .collect(StreamUtils.toLinkedHashMap(ImmutablePair::getLeft, ImmutablePair::getRight));
  }
}
