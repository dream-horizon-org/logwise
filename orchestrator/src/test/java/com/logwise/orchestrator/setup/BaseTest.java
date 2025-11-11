package com.logwise.orchestrator.setup;

import com.logwise.orchestrator.util.TestResponseWrapper;
import io.vertx.core.Vertx;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * Base test class with common setup and teardown methods. All test classes can extend this for
 * shared test infrastructure.
 */
public abstract class BaseTest {

  protected static Vertx vertx;

  @BeforeMethod
  public void setUp() throws Exception {

    if (vertx == null) {
      vertx = Vertx.vertx();
      TestResponseWrapper.init(vertx);
    }
  }

  @AfterMethod
  public void tearDown() {}

  /**
   * Cleanup method to be called after all tests in a class are done. Should be called
   * from @AfterClass if needed.
   */
  public static void cleanup() {
    if (vertx != null) {
      vertx.close();
      vertx = null;
    }
  }

  /** Get reactive Vertx instance for tests. */
  public static io.vertx.reactivex.core.Vertx getReactiveVertx() {
    if (vertx == null) {
      vertx = Vertx.vertx();
    }
    return new io.vertx.reactivex.core.Vertx(vertx);
  }
}
