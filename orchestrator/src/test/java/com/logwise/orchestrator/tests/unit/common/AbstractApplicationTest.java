package com.logwise.orchestrator.tests.unit.common;

import com.google.inject.Module;
import com.logwise.orchestrator.common.app.AbstractApplication;
import com.logwise.orchestrator.common.app.Deployable;
import com.logwise.orchestrator.setup.BaseTest;
import io.vertx.reactivex.core.Vertx;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for AbstractApplication. */
public class AbstractApplicationTest extends BaseTest {

  private TestApplication testApplication;
  private Vertx vertx;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = BaseTest.getReactiveVertx();
    testApplication = new TestApplication();
  }

  @Test
  public void testAbstractApplication_CanBeCreated() {

    Assert.assertNotNull(testApplication);
    Assert.assertNotNull(AbstractApplication.NUM_OF_CORES);
  }

  private static class TestApplication extends AbstractApplication {
    @Override
    protected Module[] getGoogleGuiceModules(Vertx vertx) {
      return new Module[0];
    }

    @Override
    protected Deployable[] getVerticlesToDeploy(Vertx vertx) {
      return new Deployable[0];
    }
  }

  private static class TestVerticle extends io.vertx.core.AbstractVerticle {}
}
