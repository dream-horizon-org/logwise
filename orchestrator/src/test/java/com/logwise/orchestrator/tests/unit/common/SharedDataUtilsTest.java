package com.logwise.orchestrator.tests.unit.common;

import com.logwise.orchestrator.common.util.SharedDataUtils;
import com.logwise.orchestrator.setup.BaseTest;
import io.vertx.reactivex.core.Vertx;
import java.util.UUID;
import java.util.function.Supplier;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for SharedDataUtils. */
public class SharedDataUtilsTest extends BaseTest {

  private Vertx vertx;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = BaseTest.getReactiveVertx();
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testGetOrCreate_WithNewName_CreatesInstance() {
    // Use unique name to avoid conflicts with other tests
    String name = "test-instance-" + UUID.randomUUID();
    Supplier<String> supplier = () -> "new-instance";

    String result = SharedDataUtils.getOrCreate(vertx, name, supplier);

    Assert.assertNotNull(result);
    Assert.assertEquals(result, "new-instance");
  }

  @Test
  public void testGetOrCreate_WithExistingName_ReturnsExistingInstance() {
    // Use unique name to avoid conflicts with other tests
    String name = "existing-instance-" + UUID.randomUUID();
    Supplier<String> supplier1 = () -> "first";
    Supplier<String> supplier2 = () -> "second";

    String result1 = SharedDataUtils.getOrCreate(vertx, name, supplier1);
    String result2 = SharedDataUtils.getOrCreate(vertx, name, supplier2);

    Assert.assertNotNull(result1);
    Assert.assertNotNull(result2);
    Assert.assertEquals(result1, result2);
    Assert.assertEquals(result1, "first"); // Should return first created instance
  }

  @Test
  public void testSetInstance_WithInstance_StoresInstance() {
    // Use a unique wrapper class to avoid conflicts with other tests
    SetInstanceTestWrapper testInstance =
        new SetInstanceTestWrapper("test-value-" + UUID.randomUUID());
    Vertx vertx = BaseTest.getReactiveVertx();

    SetInstanceTestWrapper result = SharedDataUtils.setInstance(vertx, testInstance);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getValue(), testInstance.getValue());
  }

  @Test
  public void testGetInstance_WithExistingInstance_ReturnsInstance() {
    // Use a unique wrapper class to avoid conflicts with other tests
    GetInstanceTestWrapper testInstance =
        new GetInstanceTestWrapper("test-value-" + UUID.randomUUID());
    Vertx vertx = BaseTest.getReactiveVertx();

    SharedDataUtils.setInstance(vertx, testInstance);
    GetInstanceTestWrapper result =
        SharedDataUtils.getInstance(vertx, GetInstanceTestWrapper.class);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getValue(), testInstance.getValue());
  }

  // Helper classes for testing setInstance/getInstance - using different classes to avoid conflicts
  static class SetInstanceTestWrapper {
    private final String value;

    public SetInstanceTestWrapper(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  static class GetInstanceTestWrapper {
    private final String value;

    public GetInstanceTestWrapper(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  @Test
  public void testGetInstance_WithNonExistentInstance_ThrowsException() {
    Vertx vertx = BaseTest.getReactiveVertx();
    vertx.getOrCreateContext();

    // Use a class that hasn't been set (use a more unique class)
    try {
      SharedDataUtils.getInstance(vertx, java.util.Date.class);
      Assert.fail("Should have thrown RuntimeException");
    } catch (RuntimeException e) {
      Assert.assertNotNull(e);
      Assert.assertTrue(e.getMessage().contains("Cannot find default instance"));
    }
  }
}
