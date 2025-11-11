package com.logwise.orchestrator.tests.unit.module;

import com.logwise.orchestrator.module.ClientModule;
import com.logwise.orchestrator.module.MainModule;
import com.logwise.orchestrator.setup.BaseTest;
import io.vertx.reactivex.core.Vertx;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ClientModule and MainModule. */
public class ModuleTest extends BaseTest {

  private Vertx vertx;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = BaseTest.getReactiveVertx();
  }

  @Test
  public void testClientModule_Constructor_CreatesInstance() {

    ClientModule module = new ClientModule(vertx);

    Assert.assertNotNull(module);
  }

  @Test
  public void testClientModule_Instance_Created() {

    ClientModule module = new ClientModule(vertx);

    Assert.assertNotNull(module);
  }

  @Test
  public void testMainModule_Constructor_CreatesInstance() {

    MainModule module = new MainModule(vertx);

    Assert.assertNotNull(module);
  }
}
