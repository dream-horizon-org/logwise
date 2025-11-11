package com.dream11.logcentralorchestrator.common.util;

public final class StackUtils {

  public static String getCallerName() {
    return Thread.currentThread().getStackTrace()[3].getMethodName();
  }
}
