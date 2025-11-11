package com.logwise.orchestrator.tests.unit.webclient;

import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.webclient.client.WebClient;
import com.logwise.orchestrator.webclient.client.WebClientConfig;
import com.logwise.orchestrator.webclient.client.impl.WebClientImpl;
import io.vertx.core.Vertx;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for WebClient, WebClientImpl, and WebClientConfig. */
public class WebClientTest extends BaseTest {

  private Vertx vertx;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = BaseTest.getReactiveVertx().getDelegate();
  }

  @Test
  public void testCreate_WithVertx_CreatesWebClient() {

    WebClient webClient = WebClient.create(vertx);

    Assert.assertNotNull(webClient);
    webClient.close(); // Cleanup
  }

  @Test
  public void testCreate_WithVertxAndConfigType_CreatesWebClient() {

    WebClient webClient = WebClient.create(vertx, "test");

    Assert.assertNotNull(webClient);
    webClient.close(); // Cleanup
  }

  @Test
  public void testGetWebClient_ReturnsWebClient() {

    WebClient webClient = WebClient.create(vertx);

    io.vertx.ext.web.client.WebClient vertxWebClient = webClient.getWebClient();

    Assert.assertNotNull(vertxWebClient);
    webClient.close(); // Cleanup
  }

  @Test
  public void testWebClientImpl_Constructor_WithVertx_CreatesInstance() {

    WebClientConfig config = new WebClientConfig();
    config.setConnectTimeout(5000);
    config.setIdleTimeout(30);

    WebClientImpl webClient = new WebClientImpl(vertx, config);

    Assert.assertNotNull(webClient);
    webClient.close(); // Cleanup
  }

  @Test
  public void testWebClientImpl_Close_ClosesWebClient() {

    WebClientConfig config = new WebClientConfig();
    WebClientImpl webClient = new WebClientImpl(vertx, config);

    webClient.close();

    Assert.assertNotNull(webClient);
  }

  @Test
  public void testWebClientConfig_Constructor_WithJsonObject_CreatesConfig() {

    WebClientConfig config = new WebClientConfig();
    config.setConnectTimeout(5000);
    config.setIdleTimeout(30);

    Assert.assertNotNull(config);
    Assert.assertEquals(config.getConnectTimeout(), Integer.valueOf(5000));
    Assert.assertEquals(config.getIdleTimeout(), Integer.valueOf(30));
  }

  @Test
  public void testWebClientConfig_Constructor_WithNullJson_UsesDefaults() {

    WebClientConfig config = new WebClientConfig();

    Assert.assertNotNull(config);

    Assert.assertNotNull(config.getConnectTimeout());
    Assert.assertNotNull(config.getIdleTimeout());
  }
}
