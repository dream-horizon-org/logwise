package com.logwise.spark.base;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Base test class with common setup and teardown methods. All test classes can extend this for
 * shared test infrastructure.
 */
public abstract class BaseSparkTest {

  @BeforeMethod
  public void setUp() {
    // Common setup logic can be added here
    // For example: resetting static state, initializing mocks, etc.
  }

  @AfterMethod
  public void tearDown() {
    // Common teardown logic can be added here
    // For example: cleaning up resources, resetting mocks, etc.
  }
}
