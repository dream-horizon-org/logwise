package com.logwise.orchestrator.tests.unit.config;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.Vertx;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ConfigUtils */
public class ConfigUtilsTest extends com.logwise.orchestrator.setup.BaseTest {

  private Vertx vertx;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = com.logwise.orchestrator.setup.BaseTest.getReactiveVertx().getDelegate();
  }

  @Test
  public void testGetRetriever_WithValidFormat_ReturnsConfigRetriever() {

    ConfigRetriever retriever =
        com.logwise.orchestrator.common.util.ConfigUtils.getRetriever(
            vertx, "config/application/application-%s.conf");

    Assert.assertNotNull(retriever);
  }
}
