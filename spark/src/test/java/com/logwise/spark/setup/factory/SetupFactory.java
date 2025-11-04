package com.logwise.spark.setup.factory;

import com.logwise.spark.setup.Setup;
import com.logwise.spark.setup.impl.BlackBoxSetup;
import com.logwise.spark.setup.impl.UnitTestSetup;
import com.logwise.spark.setup.impl.WhiteBoxSetup;
import com.logwise.tests.TestType;
import javax.validation.constraints.NotNull;

public class SetupFactory {
  public static Setup getSetup(@NotNull TestType testType) {
    switch (testType) {
      case WHITEBOX_TESTS:
        return new WhiteBoxSetup();
      case BLACKBOX_TESTS:
        return new BlackBoxSetup();
      case UNIT_TESTS:
        return new UnitTestSetup();
      default:
        throw new IllegalArgumentException("Invalid test type");
    }
  }
}
