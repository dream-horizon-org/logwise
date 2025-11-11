package com.logwise.orchestrator.tests.unit.common;

import com.logwise.orchestrator.common.util.CompletableUtils;
import com.logwise.orchestrator.common.util.MaybeUtils;
import com.logwise.orchestrator.common.util.SingleUtils;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for MaybeUtils, SingleUtils, and CompletableUtils. */
public class ReactiveUtilsTest {

  private static final Logger log = LoggerFactory.getLogger(ReactiveUtilsTest.class);

  @Test
  public void testMaybeUtils_ReadThroughCache_WithCacheHit_ReturnsCachedValue() {

    Maybe<String> cacheHit = Maybe.just("cached-value");
    Maybe<String> source = Maybe.just("source-value");

    Maybe<String> result =
        MaybeUtils.readThroughCache(cacheHit, source, data -> Completable.complete());

    String value = result.blockingGet();
    Assert.assertEquals(value, "cached-value");
  }

  @Test
  public void testMaybeUtils_ReadThroughCache_WithCacheMiss_ReturnsSourceValue() {

    Maybe<String> cacheMiss = Maybe.empty();
    Maybe<String> source = Maybe.just("source-value");

    Maybe<String> result =
        MaybeUtils.readThroughCache(cacheMiss, source, data -> Completable.complete());

    String value = result.blockingGet();
    Assert.assertEquals(value, "source-value");
  }

  @Test
  public void testMaybeUtils_ApplyDebugLogs_WithLogPrefix_ReturnsTransformer() {

    Maybe<String> maybe = Maybe.just("test-value");

    Maybe<String> result = maybe.compose(MaybeUtils.applyDebugLogs(log, "TestPrefix"));

    String value = result.blockingGet();
    Assert.assertEquals(value, "test-value");
  }

  @Test
  public void testMaybeUtils_ApplyDebugLogs_WithoutLogPrefix_ReturnsTransformer() {

    Maybe<String> maybe = Maybe.just("test-value");

    Maybe<String> result = maybe.compose(MaybeUtils.applyDebugLogs(log));

    String value = result.blockingGet();
    Assert.assertEquals(value, "test-value");
  }

  @Test
  public void testSingleUtils_ReadThroughCache_WithCacheHit_ReturnsCachedValue() {

    Maybe<String> cacheHit = Maybe.just("cached-value");
    Single<String> source = Single.just("source-value");

    Single<String> result =
        SingleUtils.readThroughCache(cacheHit, source, data -> Completable.complete());

    String value = result.blockingGet();
    Assert.assertEquals(value, "cached-value");
  }

  @Test
  public void testSingleUtils_ReadThroughCache_WithCacheMiss_ReturnsSourceValue() {

    Maybe<String> cacheMiss = Maybe.empty();
    Single<String> source = Single.just("source-value");

    Single<String> result =
        SingleUtils.readThroughCache(cacheMiss, source, data -> Completable.complete());

    String value = result.blockingGet();
    Assert.assertEquals(value, "source-value");
  }

  @Test
  public void testSingleUtils_ApplyDebugLogs_WithLogPrefix_ReturnsTransformer() {

    Single<String> single = Single.just("test-value");

    Single<String> result = single.compose(SingleUtils.applyDebugLogs(log, "TestPrefix"));

    String value = result.blockingGet();
    Assert.assertEquals(value, "test-value");
  }

  @Test
  public void testSingleUtils_ApplyDebugLogs_WithoutLogPrefix_ReturnsTransformer() {

    Single<String> single = Single.just("test-value");

    Single<String> result = single.compose(SingleUtils.applyDebugLogs(log));

    String value = result.blockingGet();
    Assert.assertEquals(value, "test-value");
  }

  @Test
  public void testCompletableUtils_ApplyDebugLogs_WithLogPrefix_ReturnsTransformer() {

    Completable completable = Completable.complete();

    Completable result = completable.compose(CompletableUtils.applyDebugLogs(log, "TestPrefix"));

    result.blockingAwait(); // Should complete without error
  }

  @Test
  public void testCompletableUtils_ApplyDebugLogs_WithoutLogPrefix_ReturnsTransformer() {

    Completable completable = Completable.complete();

    Completable result = completable.compose(CompletableUtils.applyDebugLogs(log));

    result.blockingAwait(); // Should complete without error
  }

  @Test
  public void testCompletableUtils_ApplyDebugLogs_WithError_LogsError() {

    RuntimeException error = new RuntimeException("Test error");
    Completable completable = Completable.error(error);

    Completable result = completable.compose(CompletableUtils.applyDebugLogs(log, "TestPrefix"));

    try {
      result.blockingAwait();
      Assert.fail("Should have thrown exception");
    } catch (RuntimeException e) {
      Assert.assertEquals(e.getMessage(), "Test error");
    }
  }
}
