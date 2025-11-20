package com.logwise.orchestrator.tests.unit.webclient;

import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.webclient.client.WebClientConfig;
import com.logwise.orchestrator.webclient.client.impl.WebClientImpl;
import io.vertx.reactivex.core.Vertx;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for WebClientImpl. */
public class WebClientImplTest extends BaseTest {

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
  public void testWebClientImpl_WithValidConfig_CreatesWebClient() {
    WebClientConfig config = new WebClientConfig();
    config.setConnectTimeout(5000);
    config.setLogActivity(false);
    config.setMaxPoolSize(10);
    config.setKeepAlive(true);
    config.setKeepAliveTimeout(30);
    config.setPipelining(false);
    config.setPipeliningLimit(1);
    config.setTcpKeepAlive(true);
    config.setHttpProtocolVersion("HTTP_1_1");
    config.setHttp2KeepAliveTimeout(30);
    config.setHttp2MaxPoolSize(10);
    config.setIdleTimeout(30);
    config.setHttp2MultiplexingLimit(1);
    config.setUseAlpn(false);
    config.setPort(80);
    config.setMaxWaitQueueSize(10);
    config.setUseProxy(false);

    WebClientImpl webClientImpl = new WebClientImpl(vertx.getDelegate(), config);

    Assert.assertNotNull(webClientImpl);
    Assert.assertNotNull(webClientImpl.getWebClient());
  }

  @Test
  public void testWebClientImpl_WithProxyConfig_CreatesWebClientWithProxy() {
    WebClientConfig config = new WebClientConfig();
    config.setConnectTimeout(5000);
    config.setLogActivity(false);
    config.setMaxPoolSize(10);
    config.setKeepAlive(true);
    config.setKeepAliveTimeout(30);
    config.setPipelining(false);
    config.setPipeliningLimit(1);
    config.setTcpKeepAlive(true);
    config.setHttpProtocolVersion("HTTP_1_1");
    config.setHttp2KeepAliveTimeout(30);
    config.setHttp2MaxPoolSize(10);
    config.setIdleTimeout(30);
    config.setHttp2MultiplexingLimit(1);
    config.setUseAlpn(false);
    config.setPort(80);
    config.setMaxWaitQueueSize(10);
    config.setUseProxy(true);
    config.setProxyHost("proxy.example.com");
    config.setProxyPort(8080);
    config.setType(io.vertx.core.net.ProxyType.HTTP);
    config.setUseProxyAuthentication(false);

    WebClientImpl webClientImpl = new WebClientImpl(vertx.getDelegate(), config);

    Assert.assertNotNull(webClientImpl);
    Assert.assertNotNull(webClientImpl.getWebClient());
  }

  @Test
  public void testWebClientImpl_WithProxyAuth_CreatesWebClientWithAuth() {
    WebClientConfig config = new WebClientConfig();
    config.setConnectTimeout(5000);
    config.setLogActivity(false);
    config.setMaxPoolSize(10);
    config.setKeepAlive(true);
    config.setKeepAliveTimeout(30);
    config.setPipelining(false);
    config.setPipeliningLimit(1);
    config.setTcpKeepAlive(true);
    config.setHttpProtocolVersion("HTTP_1_1");
    config.setHttp2KeepAliveTimeout(30);
    config.setHttp2MaxPoolSize(10);
    config.setIdleTimeout(30);
    config.setHttp2MultiplexingLimit(1);
    config.setUseAlpn(false);
    config.setPort(80);
    config.setMaxWaitQueueSize(10);
    config.setUseProxy(true);
    config.setProxyHost("proxy.example.com");
    config.setProxyPort(8080);
    config.setType(io.vertx.core.net.ProxyType.HTTP);
    config.setUseProxyAuthentication(true);
    config.setUserName("user");
    config.setPassword("pass");

    WebClientImpl webClientImpl = new WebClientImpl(vertx.getDelegate(), config);

    Assert.assertNotNull(webClientImpl);
    Assert.assertNotNull(webClientImpl.getWebClient());
  }

  @Test
  public void testClose_ClosesWebClient() {
    WebClientConfig config = new WebClientConfig();
    config.setConnectTimeout(5000);
    config.setLogActivity(false);
    config.setMaxPoolSize(10);
    config.setKeepAlive(true);
    config.setKeepAliveTimeout(30);
    config.setPipelining(false);
    config.setPipeliningLimit(1);
    config.setTcpKeepAlive(true);
    config.setHttpProtocolVersion("HTTP_1_1");
    config.setHttp2KeepAliveTimeout(30);
    config.setHttp2MaxPoolSize(10);
    config.setIdleTimeout(30);
    config.setHttp2MultiplexingLimit(1);
    config.setUseAlpn(false);
    config.setPort(80);
    config.setMaxWaitQueueSize(10);
    config.setUseProxy(false);

    WebClientImpl webClientImpl = new WebClientImpl(vertx.getDelegate(), config);
    Assert.assertNotNull(webClientImpl.getWebClient());

    webClientImpl.close();

    // After close, webClient should be null
    Assert.assertNull(webClientImpl.getWebClient());
  }
}
