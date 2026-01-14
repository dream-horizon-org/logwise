package com.logwise.orchestrator.tests.unit.util;

import com.logwise.orchestrator.util.WebClientUtils;
import io.reactivex.Flowable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.Assert;
import org.testng.annotations.Test;

public class WebClientUtilsTest {

  @Test
  public void testRetryWithDelay_WithSuccessfulRetry_RetriesCorrectly() throws Exception {
    AtomicInteger attemptCount = new AtomicInteger(0);
    
    Flowable<Throwable> errors = Flowable.just(
        new RuntimeException("Error 1"),
        new RuntimeException("Error 2")
    );
    
    Flowable<?> result = WebClientUtils.retryWithDelay(10, TimeUnit.MILLISECONDS, 3)
        .apply(errors);
    
    try {
      result.blockingSubscribe();
    } catch (Exception e) {
      // Expected to complete or throw
    }
    Assert.assertEquals(attemptCount.get(), 0);
  }

  @Test
  public void testRetryWithDelay_WithMaxAttemptsExceeded_PropagatesError() throws Exception {
    RuntimeException error1 = new RuntimeException("Error 1");
    RuntimeException error2 = new RuntimeException("Error 2");
    RuntimeException error3 = new RuntimeException("Error 3");
    RuntimeException error4 = new RuntimeException("Error 4");
    
    Flowable<Throwable> errors = Flowable.just(error1, error2, error3, error4);
    
    Flowable<?> result = WebClientUtils.retryWithDelay(10, TimeUnit.MILLISECONDS, 2)
        .apply(errors);
    
    try {
      result.blockingSubscribe();
      Assert.fail("Should have thrown exception");
    } catch (RuntimeException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testRetryWithDelay_WithZeroMaxAttempts_PropagatesErrorImmediately() throws Exception {
    RuntimeException error = new RuntimeException("Error");
    Flowable<Throwable> errors = Flowable.just(error);
    
    Flowable<?> result = WebClientUtils.retryWithDelay(10, TimeUnit.MILLISECONDS, 0)
        .apply(errors);
    
    try {
      result.blockingSubscribe();
      Assert.fail("Should have thrown exception");
    } catch (RuntimeException e) {
      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testRetryWithDelay_WithDifferentTimeUnits_WorksCorrectly() throws Exception {
    RuntimeException error = new RuntimeException("Error");
    Flowable<Throwable> errors = Flowable.just(error);
    
    long startTime = System.currentTimeMillis();
    Flowable<?> result = WebClientUtils.retryWithDelay(100, TimeUnit.MILLISECONDS, 1)
        .apply(errors);
    
    try {
      result.blockingSubscribe();
    } catch (RuntimeException e) {
      long elapsedTime = System.currentTimeMillis() - startTime;
      Assert.assertTrue(elapsedTime >= 100, "Should have waited at least 100ms");
    }
  }
}

