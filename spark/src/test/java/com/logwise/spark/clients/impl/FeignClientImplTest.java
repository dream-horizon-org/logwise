package com.logwise.spark.clients.impl;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.logwise.spark.clients.FeignClient;
import com.logwise.spark.clients.LogCentralOrchestratorClient;
import feign.Feign;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FeignClientImplTest {

  private FeignClientImpl feignClientImpl;

  @BeforeMethod
  public void setUp() {
    feignClientImpl = new FeignClientImpl();
  }

  @Test
  public void testCreateClient_WithValidClassAndUrl_ReturnsClient() {
    String url = "http://localhost:8080";
    LogCentralOrchestratorClient client =
        feignClientImpl.createClient(LogCentralOrchestratorClient.class, url);

    assertNotNull(client);
  }

  @Test
  public void testCreateClient_WithDifferentUrl_ReturnsClient() {
    String url = "http://example.com:9090";
    LogCentralOrchestratorClient client =
        feignClientImpl.createClient(LogCentralOrchestratorClient.class, url);

    assertNotNull(client);
  }

  @Test
  public void testCreateClient_WithHttpsUrl_ReturnsClient() {
    String url = "https://example.com";
    LogCentralOrchestratorClient client =
        feignClientImpl.createClient(LogCentralOrchestratorClient.class, url);

    assertNotNull(client);
  }

  @Test
  public void testCreateClient_WithNullUrl_ThrowsException() {
    try {
      feignClientImpl.createClient(LogCentralOrchestratorClient.class, null);
      fail("Should have thrown exception");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void testCreateClient_WithEmptyUrl_ThrowsException() {
    try {
      feignClientImpl.createClient(LogCentralOrchestratorClient.class, "");
      fail("Should have thrown exception");
    } catch (Exception e) {
      assertNotNull(e);
    }
  }

  @Test
  public void testCreateClient_WithInvalidUrl_CreatesClient() {
    // Feign doesn't validate URLs at creation time, it just creates a client
    // The client will fail when actually used, but creation succeeds
    String url = "invalid-url";
    LogCentralOrchestratorClient client =
        feignClientImpl.createClient(LogCentralOrchestratorClient.class, url);

    assertNotNull(client);
  }
}

