package com.logwise.orchestrator.tests.unit;

import com.logwise.orchestrator.CacheNotFoundException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for CacheNotFoundException. */
public class CacheNotFoundExceptionTest {

  @Test
  public void testConstructor_WithMessage_CreatesException() {

    String message = "Cache not found for key: test-key";

    CacheNotFoundException exception = new CacheNotFoundException(message);

    Assert.assertNotNull(exception);
    Assert.assertEquals(exception.getMessage(), message);
    Assert.assertNull(exception.getCause());
  }

  @Test
  public void testConstructor_WithEmptyMessage_CreatesException() {

    String message = "";

    CacheNotFoundException exception = new CacheNotFoundException(message);

    Assert.assertNotNull(exception);
    Assert.assertEquals(exception.getMessage(), message);
  }

  @Test
  public void testConstructor_WithNullMessage_CreatesException() {

    String message = null;

    CacheNotFoundException exception = new CacheNotFoundException(message);

    Assert.assertNotNull(exception);
    Assert.assertNull(exception.getMessage());
  }
}
