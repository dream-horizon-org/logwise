package com.logwise.orchestrator.tests.unit.common;

import com.logwise.orchestrator.common.util.ContextUtils;
import com.logwise.orchestrator.common.util.SharedDataUtils;
import com.logwise.orchestrator.setup.BaseTest;
import io.vertx.reactivex.core.Context;
import io.vertx.reactivex.core.Vertx;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ContextUtils and SharedDataUtils. */
public class ContextUtilsTest extends BaseTest {

  private Vertx vertx;
  private Context context;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = BaseTest.getReactiveVertx();
    context = vertx.getOrCreateContext();
  }

  @Test
  public void testSetInstance_WithContextAndKey_SetsInstance() {

    String testValue = "test-value";
    String key = "test-key";

    ContextUtils.setInstance(context, testValue, key);
    String retrieved = ContextUtils.getInstance(context, String.class, key);

    Assert.assertEquals(retrieved, testValue);
  }

  @Test
  public void testSetInstance_WithContext_WithoutKey_UsesDefaultKey() {

    String testValue = "test-value-default";

    ContextUtils.<String>setInstance(context, testValue);
    String retrieved = ContextUtils.getInstance(context, String.class);

    Assert.assertEquals(retrieved, testValue);
  }

  @Test
  public void testGetInstance_WithKey_RetrievesInstance() {

    Integer testValue = 42;
    String key = "number-key";
    ContextUtils.setInstance(context, testValue, key);

    Integer retrieved = ContextUtils.getInstance(context, Integer.class, key);

    Assert.assertEquals(retrieved, testValue);
  }

  @Test
  public void testGetInstances_WithMultipleInstances_ReturnsMap() throws Exception {

    String value1 = "value1";
    String value2 = "value2";

    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    final Map<String, String>[] resultHolder = new Map[1];
    final Exception[] exceptionHolder = new Exception[1];

    context.runOnContext(
        v -> {
          try {

            ContextUtils.setInstance(context, value1, "key1");
            ContextUtils.setInstance(context, value2, "key2");

            Map<String, String> instances = ContextUtils.getInstances(String.class);
            resultHolder[0] = instances;
          } catch (Exception e) {
            exceptionHolder[0] = e;
          } finally {
            latch.countDown();
          }
        });

    latch.await();

    if (exceptionHolder[0] != null) {
      throw exceptionHolder[0];
    }
    Map<String, String> instances = resultHolder[0];
    Assert.assertNotNull(instances);
    Assert.assertTrue(instances.size() >= 2);
    Assert.assertEquals(instances.get("key1"), value1);
    Assert.assertEquals(instances.get("key2"), value2);
  }

  @Test
  public void testGetOrCreate_WithSupplier_CreatesInstance() {

    String name = "test-instance";
    String expectedValue = "created-value";

    String result = SharedDataUtils.getOrCreate(vertx, name, () -> expectedValue);

    Assert.assertEquals(result, expectedValue);
  }

  @Test
  public void testGetOrCreate_WithExistingName_ReturnsExistingInstance() {

    String name = "shared-instance";
    String firstValue = "first";
    String secondValue = "second";

    String first = SharedDataUtils.getOrCreate(vertx, name, () -> firstValue);
    String second = SharedDataUtils.getOrCreate(vertx, name, () -> secondValue);

    Assert.assertEquals(first, firstValue);
    Assert.assertEquals(second, firstValue); // Should return existing, not create new
  }

  @Test
  public void testSetInstance_WithVertx_SetsInstance() {

    String instance = "test-instance";

    String result = SharedDataUtils.setInstance(vertx, instance);
    String retrieved = SharedDataUtils.getInstance(vertx, String.class);

    Assert.assertEquals(result, instance);
    Assert.assertEquals(retrieved, instance);
  }

  @Test
  public void testGetInstance_WithExistingInstance_ReturnsInstance() {

    Integer instance = 100;
    SharedDataUtils.setInstance(vertx, instance);

    Integer retrieved = SharedDataUtils.getInstance(vertx, Integer.class);

    Assert.assertEquals(retrieved, instance);
  }

  @Test
  public void testGetInstance_WithoutExistingInstance_ThrowsException() {

    try {
      SharedDataUtils.getInstance(vertx, Double.class);
      Assert.fail("Should have thrown RuntimeException");
    } catch (RuntimeException e) {
      Assert.assertTrue(e.getMessage().contains("Cannot find default instance"));
    }
  }
}
