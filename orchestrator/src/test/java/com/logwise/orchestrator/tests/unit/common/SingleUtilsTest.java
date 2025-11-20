package com.logwise.orchestrator.tests.unit.common;

import com.logwise.orchestrator.common.util.SingleUtils;
import com.logwise.orchestrator.setup.BaseTest;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for SingleUtils. */
public class SingleUtilsTest extends BaseTest {

  private static final Logger log = LoggerFactory.getLogger(SingleUtilsTest.class);

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testReadThroughCache_WithCacheHit_ReturnsCachedValue() {
    Maybe<String> getFromCache = Maybe.just("cached-value");
    Single<String> getFromSource = Single.just("source-value");
    io.reactivex.functions.Function<String, Completable> saveToCache =
        value -> Completable.complete();

    Single<String> result = SingleUtils.readThroughCache(getFromCache, getFromSource, saveToCache);

    Assert.assertNotNull(result);
    String value = result.blockingGet();
    Assert.assertEquals(value, "cached-value");
  }

  @Test
  public void testReadThroughCache_WithCacheMiss_ReturnsSourceValue() {
    Maybe<String> getFromCache = Maybe.empty();
    Single<String> getFromSource = Single.just("source-value");
    io.reactivex.functions.Function<String, Completable> saveToCache =
        value -> Completable.complete();

    Single<String> result = SingleUtils.readThroughCache(getFromCache, getFromSource, saveToCache);

    Assert.assertNotNull(result);
    String value = result.blockingGet();
    Assert.assertEquals(value, "source-value");
  }

  @Test
  public void testReadThroughCache_WithSourceError_PropagatesError() {
    Maybe<String> getFromCache = Maybe.empty();
    Single<String> getFromSource = Single.error(new RuntimeException("Source error"));
    io.reactivex.functions.Function<String, Completable> saveToCache =
        value -> Completable.complete();

    Single<String> result = SingleUtils.readThroughCache(getFromCache, getFromSource, saveToCache);

    Assert.assertNotNull(result);
    try {
      result.blockingGet();
      Assert.fail("Should have thrown exception");
    } catch (RuntimeException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testApplyDebugLogs_WithLogPrefix_ReturnsTransformer() {
    String logPrefix = "TestPrefix";
    Single<String> source = Single.just("test");

    Single<String> result = source.compose(SingleUtils.applyDebugLogs(log, logPrefix));

    Assert.assertNotNull(result);
    String value = result.blockingGet();
    Assert.assertEquals(value, "test");
  }

  @Test
  public void testApplyDebugLogs_WithoutLogPrefix_UsesCallerName() {
    Single<String> source = Single.just("test");

    Single<String> result = source.compose(SingleUtils.applyDebugLogs(log));

    Assert.assertNotNull(result);
    String value = result.blockingGet();
    Assert.assertEquals(value, "test");
  }

  @Test
  public void testApplyDebugLogs_WithError_LogsError() {
    String logPrefix = "TestPrefix";
    RuntimeException error = new RuntimeException("Test error");
    Single<String> source = Single.error(error);

    Single<String> result = source.compose(SingleUtils.applyDebugLogs(log, logPrefix));

    Assert.assertNotNull(result);
    try {
      result.blockingGet();
      Assert.fail("Should have thrown exception");
    } catch (RuntimeException e) {
      Assert.assertNotNull(e);
    }
  }
}
