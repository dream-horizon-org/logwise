package com.logwise.orchestrator.common.util;

public final class StackUtils {

  public static String getCallerName() {
    return Thread.currentThread().getStackTrace()[3].getMethodName();
  }
}
