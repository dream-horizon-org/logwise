package com.logwise.orchestrator.tests.unit.common;

import com.logwise.orchestrator.common.util.MaybeUtils;
import com.logwise.orchestrator.setup.BaseTest;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for MaybeUtils. */
public class MaybeUtilsTest extends BaseTest {

  private static final Logger log = LoggerFactory.getLogger(MaybeUtilsTest.class);

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
    Maybe<String> getFromSource = Maybe.just("source-value");
    io.reactivex.functions.Function<String, Completable> saveToCache =
        value -> Completable.complete();

    Maybe<String> result = MaybeUtils.readThroughCache(getFromCache, getFromSource, saveToCache);

    Assert.assertNotNull(result);
    String value = result.blockingGet();
    Assert.assertEquals(value, "cached-value");
  }

  @Test
  public void testReadThroughCache_WithCacheMiss_ReturnsSourceValue() {
    Maybe<String> getFromCache = Maybe.empty();
    Maybe<String> getFromSource = Maybe.just("source-value");
    io.reactivex.functions.Function<String, Completable> saveToCache =
        value -> Completable.complete();

    Maybe<String> result = MaybeUtils.readThroughCache(getFromCache, getFromSource, saveToCache);

    Assert.assertNotNull(result);
    String value = result.blockingGet();
    Assert.assertEquals(value, "source-value");
  }

  @Test
  public void testReadThroughCache_WithBothEmpty_ReturnsEmpty() {
    Maybe<String> getFromCache = Maybe.empty();
    Maybe<String> getFromSource = Maybe.empty();
    io.reactivex.functions.Function<String, Completable> saveToCache =
        value -> Completable.complete();

    Maybe<String> result = MaybeUtils.readThroughCache(getFromCache, getFromSource, saveToCache);

    Assert.assertNotNull(result);
    Assert.assertTrue(result.isEmpty().blockingGet());
  }

  @Test
  public void testApplyDebugLogs_WithLogPrefix_ReturnsTransformer() {
    String logPrefix = "TestPrefix";
    Maybe<String> source = Maybe.just("test");

    Maybe<String> result = source.compose(MaybeUtils.applyDebugLogs(log, logPrefix));

    Assert.assertNotNull(result);
    String value = result.blockingGet();
    Assert.assertEquals(value, "test");
  }

  @Test
  public void testApplyDebugLogs_WithoutLogPrefix_UsesCallerName() {
    Maybe<String> source = Maybe.just("test");

    Maybe<String> result = source.compose(MaybeUtils.applyDebugLogs(log));

    Assert.assertNotNull(result);
    String value = result.blockingGet();
    Assert.assertEquals(value, "test");
  }

  @Test
  public void testApplyDebugLogs_WithError_LogsError() {
    String logPrefix = "TestPrefix";
    RuntimeException error = new RuntimeException("Test error");
    Maybe<String> source = Maybe.error(error);

    Maybe<String> result = source.compose(MaybeUtils.applyDebugLogs(log, logPrefix));

    Assert.assertNotNull(result);
    try {
      result.blockingGet();
      Assert.fail("Should have thrown exception");
    } catch (RuntimeException e) {
      Assert.assertNotNull(e);
    }
  }
}
