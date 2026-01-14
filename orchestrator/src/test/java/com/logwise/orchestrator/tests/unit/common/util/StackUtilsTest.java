package com.logwise.orchestrator.tests.unit.common.util;

import com.logwise.orchestrator.common.util.StackUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

public class StackUtilsTest {

  @Test
  public void testGetCallerName_ReturnsNotNull() {
    // StackUtils.getCallerName() uses Thread.currentThread().getStackTrace()[3]
    // In test context, this may return different values depending on test framework
    // So we just verify it returns a non-null string
    String callerName = StackUtils.getCallerName();
    Assert.assertNotNull(callerName);
    Assert.assertFalse(callerName.isEmpty());
  }

  @Test
  public void testGetCallerName_FromHelperMethod_ReturnsNotNull() {
    String callerName = getCallerNameHelper();
    Assert.assertNotNull(callerName);
    Assert.assertFalse(callerName.isEmpty());
  }

  private String getCallerNameHelper() {
    return StackUtils.getCallerName();
  }
}
