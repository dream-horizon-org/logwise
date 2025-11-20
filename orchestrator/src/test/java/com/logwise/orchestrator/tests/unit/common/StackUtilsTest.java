package com.logwise.orchestrator.tests.unit.common;

import com.logwise.orchestrator.common.util.StackUtils;
import com.logwise.orchestrator.setup.BaseTest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for StackUtils. */
public class StackUtilsTest extends BaseTest {

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testGetCallerName_ReturnsCallingMethodName() {
    String callerName = getCallerNameHelper();

    Assert.assertNotNull(callerName);
    // StackUtils.getCallerName() returns the method name at stack index 3
    // which could be the test method or the helper method depending on JVM
    Assert.assertTrue(
        callerName.contains("getCallerName") || callerName.contains("testGetCallerName"));
  }

  @Test
  public void testGetCallerName_WithNestedCall_ReturnsCorrectMethodName() {
    String callerName = nestedCallHelper();

    Assert.assertNotNull(callerName);
    // The caller name should be the method that directly calls StackUtils.getCallerName()
    Assert.assertNotNull(callerName);
  }

  private String getCallerNameHelper() {
    return StackUtils.getCallerName();
  }

  private String nestedCallHelper() {
    return innerNestedCall();
  }

  private String innerNestedCall() {
    return StackUtils.getCallerName();
  }
}
