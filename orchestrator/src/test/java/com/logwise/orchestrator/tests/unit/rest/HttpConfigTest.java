package com.logwise.orchestrator.tests.unit.rest;

import com.logwise.orchestrator.rest.config.HttpConfig;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for HttpConfig. */
public class HttpConfigTest {

  private HttpConfig httpConfig;

  @BeforeMethod
  public void setUp() {
    httpConfig = new HttpConfig();
  }

  @Test
  public void testDefaultValues_AreSetCorrectly() {

    Assert.assertEquals(
        httpConfig.getVertxLoggerDelegateFactoryClassName(),
        "io.vertx.core.logging.SLF4JLogDelegateFactory");
    Assert.assertEquals(httpConfig.getHost(), "0.0.0.0");
    Assert.assertEquals(httpConfig.getCompressionLevel(), 1);
    Assert.assertFalse(httpConfig.isCompressionEnabled());
    Assert.assertEquals(httpConfig.getIdleTimeOut(), 30);
    Assert.assertEquals(httpConfig.getAccessLoggerName(), "HTTP_ACCESS_LOGGER");
    Assert.assertFalse(httpConfig.isAccessLoggerEnable());
    Assert.assertFalse(httpConfig.isLogActivity());
    Assert.assertTrue(httpConfig.isReusePort());
    Assert.assertTrue(httpConfig.isReuseAddress());
    Assert.assertTrue(httpConfig.isTcpFastOpen());
    Assert.assertTrue(httpConfig.isTcpNoDelay());
    Assert.assertTrue(httpConfig.isTcpQuickAck());
    Assert.assertTrue(httpConfig.isTcpKeepAlive());
    Assert.assertFalse(httpConfig.isUseAlpn());
  }

  @Test
  public void testGetPort_WithZeroPort_UsesSystemProperty() {

    String originalPort = System.getProperty("http.default.port");
    try {
      System.setProperty("http.default.port", "9090");
      httpConfig.setPort(0);

      int port = httpConfig.getPort();

      Assert.assertEquals(port, 9090);
    } finally {
      if (originalPort != null) {
        System.setProperty("http.default.port", originalPort);
      } else {
        System.clearProperty("http.default.port");
      }
    }
  }

  @Test
  public void testGetPort_WithPositivePort_ReturnsPort() {

    httpConfig.setPort(8080);

    int port = httpConfig.getPort();

    Assert.assertEquals(port, 8080);
  }

  @Test
  public void testGetPort_WithNoSystemProperty_UsesDefault8080() {

    String originalPort = System.getProperty("http.default.port");
    try {
      System.clearProperty("http.default.port");
      httpConfig.setPort(0);

      int port = httpConfig.getPort();

      Assert.assertEquals(port, 8080);
    } finally {
      if (originalPort != null) {
        System.setProperty("http.default.port", originalPort);
      }
    }
  }

  @Test
  public void testSetters_WorkCorrectly() {

    httpConfig.setVertxLoggerDelegateFactoryClassName("custom.factory");
    httpConfig.setHost("localhost");
    httpConfig.setPort(9090);
    httpConfig.setCompressionLevel(5);
    httpConfig.setCompressionEnabled(true);
    httpConfig.setIdleTimeOut(60);
    httpConfig.setAccessLoggerName("CUSTOM_LOGGER");
    httpConfig.setAccessLoggerEnable(true);
    httpConfig.setLogActivity(true);
    httpConfig.setReusePort(false);
    httpConfig.setReuseAddress(false);
    httpConfig.setTcpFastOpen(false);
    httpConfig.setTcpNoDelay(false);
    httpConfig.setTcpQuickAck(false);
    httpConfig.setTcpKeepAlive(false);
    httpConfig.setUseAlpn(true);

    Assert.assertEquals(httpConfig.getVertxLoggerDelegateFactoryClassName(), "custom.factory");
    Assert.assertEquals(httpConfig.getHost(), "localhost");
    Assert.assertEquals(httpConfig.getPort(), 9090);
    Assert.assertEquals(httpConfig.getCompressionLevel(), 5);
    Assert.assertTrue(httpConfig.isCompressionEnabled());
    Assert.assertEquals(httpConfig.getIdleTimeOut(), 60);
    Assert.assertEquals(httpConfig.getAccessLoggerName(), "CUSTOM_LOGGER");
    Assert.assertTrue(httpConfig.isAccessLoggerEnable());
    Assert.assertTrue(httpConfig.isLogActivity());
    Assert.assertFalse(httpConfig.isReusePort());
    Assert.assertFalse(httpConfig.isReuseAddress());
    Assert.assertFalse(httpConfig.isTcpFastOpen());
    Assert.assertFalse(httpConfig.isTcpNoDelay());
    Assert.assertFalse(httpConfig.isTcpQuickAck());
    Assert.assertFalse(httpConfig.isTcpKeepAlive());
    Assert.assertTrue(httpConfig.isUseAlpn());
  }
}
