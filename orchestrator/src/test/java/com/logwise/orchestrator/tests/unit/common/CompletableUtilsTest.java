package com.logwise.orchestrator.tests.unit.common;

import com.logwise.orchestrator.common.util.CompletableUtils;
import com.logwise.orchestrator.setup.BaseTest;
import io.reactivex.Completable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for CompletableUtils. */
public class CompletableUtilsTest extends BaseTest {

  private static final Logger log = LoggerFactory.getLogger(CompletableUtilsTest.class);

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testApplyDebugLogs_WithLogPrefix_ReturnsTransformer() {
    String logPrefix = "TestPrefix";
    Completable source = Completable.complete();

    Completable result = source.compose(CompletableUtils.applyDebugLogs(log, logPrefix));

    Assert.assertNotNull(result);
    result.blockingAwait();
  }

  @Test
  public void testApplyDebugLogs_WithoutLogPrefix_UsesCallerName() {
    Completable source = Completable.complete();

    Completable result = source.compose(CompletableUtils.applyDebugLogs(log));

    Assert.assertNotNull(result);
    result.blockingAwait();
  }

  @Test
  public void testApplyDebugLogs_WithError_LogsError() {
    String logPrefix = "TestPrefix";
    RuntimeException error = new RuntimeException("Test error");
    Completable source = Completable.error(error);

    Completable result = source.compose(CompletableUtils.applyDebugLogs(log, logPrefix));

    Assert.assertNotNull(result);
    try {
      result.blockingAwait();
      Assert.fail("Should have thrown exception");
    } catch (RuntimeException e) {
      Assert.assertNotNull(e);
    }
  }
}
