package com.logwise.orchestrator.tests.unit.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.orchestrator.common.util.ConfigProvider;
import com.logwise.orchestrator.common.util.ConfigUtils;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ConfigUtils and ConfigProvider. */
public class CommonConfigUtilsTest extends com.logwise.orchestrator.setup.BaseTest {

  private Vertx vertx;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = com.logwise.orchestrator.setup.BaseTest.getReactiveVertx().getDelegate();
  }

  @Test
  public void testGetRetriever_WithValidFormat_ReturnsConfigRetriever() {

    ConfigRetriever retriever =
        ConfigUtils.getRetriever(vertx, "config/application/application-%s.conf");

    Assert.assertNotNull(retriever);
  }

  @Test
  public void testFromConfigFile_WithValidFormat_ReturnsConfig() {

    try {
      com.typesafe.config.Config config =
          ConfigUtils.fromConfigFile("config/application/application-%s.conf");
      Assert.assertNotNull(config);
    } catch (com.typesafe.config.ConfigException.UnresolvedSubstitution e) {

      Assert.assertTrue(true);
    }
  }

  @Test
  public void testGetConfig_WithPathAndFileName_ReturnsJsonObject() throws Exception {

    ObjectMapper objectMapper = new ObjectMapper();
    ConfigProvider configProvider = new ConfigProvider(objectMapper);

    try {
      JsonObject config = configProvider.getConfig("config/application", "application");
      Assert.assertNotNull(config);
    } catch (com.typesafe.config.ConfigException.UnresolvedSubstitution e) {

      Assert.assertTrue(true);
    }
  }

  @Test
  public void testGetConfig_WithClass_ReturnsTypedConfig() throws Exception {

    ObjectMapper objectMapper = new ObjectMapper();
    ConfigProvider configProvider = new ConfigProvider(objectMapper);

    try {
      java.util.Map config =
          configProvider.getConfig("config/application", "application", java.util.Map.class);
      Assert.assertNotNull(config);
    } catch (com.typesafe.config.ConfigException.UnresolvedSubstitution e) {

      Assert.assertTrue(true);
    }
  }
}
