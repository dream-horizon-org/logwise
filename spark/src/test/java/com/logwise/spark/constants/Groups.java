package com.logwise.spark.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Groups {

  private static final String TEST_TYPE = "TestType";
  public static final String TEST_TYPE_UNIT = TEST_TYPE + ":Unit";
  public static final String TEST_TYPE_FUNCTIONAL = TEST_TYPE + ":Functional";
  public static final String TEST_TYPE_INTEGRATION = TEST_TYPE + ":Integration";

  public static final String TEST_RUN_TYPE = "TestRunType";
  public static final String TEST_RUN_TYPE_BLACKBOX = TEST_RUN_TYPE + ":BlackBox";
  public static final String TEST_RUN_TYPE_WHITEBOX = TEST_RUN_TYPE + ":WhiteBox";

  private static final String PURPOSE = "Purpose";
  public static final String PURPOSE_POSITIVE_TESTS = PURPOSE + ":Positive-Tests";
  public static final String PURPOSE_NEGATIVE_TESTS = PURPOSE + ":Negative-Tests";
}
