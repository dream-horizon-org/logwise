package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.common.util.TestCompletableFutureUtils;
import com.logwise.orchestrator.dao.HealthCheckDao;
import com.logwise.orchestrator.rest.HealthCheck;
import com.logwise.orchestrator.rest.healthcheck.HealthCheckResponse;
import com.logwise.orchestrator.setup.BaseTest;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.CompletionStage;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for HealthCheck REST endpoint. */
public class HealthCheckEndpointTest extends BaseTest {

  private HealthCheckDao mockHealthCheckDao;
  private HealthCheck healthCheck;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    TestCompletableFutureUtils.init(vertx);
    mockHealthCheckDao = mock(HealthCheckDao.class);
    healthCheck = new HealthCheck(mockHealthCheckDao);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testHealthcheck_WithHealthyMysql_ReturnsUpStatus() throws Exception {
    JsonObject mysqlHealth = new JsonObject().put("status", "ok");
    when(mockHealthCheckDao.mysqlHealthCheck()).thenReturn(Single.just(mysqlHealth));

    try (MockedStatic<CompletableFutureUtils> mockedCompletableFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedCompletableFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<HealthCheckResponse> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<HealthCheckResponse> future = healthCheck.healthcheck();

      // Wait a bit for async processing
      Thread.sleep(100);
      HealthCheckResponse response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      String statusJson = response.toJson().toString();
      Assert.assertTrue(statusJson.contains("\"status\":\"UP\""));
      verify(mockHealthCheckDao, times(1)).mysqlHealthCheck();
    }
  }

  @Test
  public void testHealthcheck_WithUnhealthyMysql_ThrowsException() throws Exception {
    RuntimeException error = new RuntimeException("DB connection failed");
    when(mockHealthCheckDao.mysqlHealthCheck()).thenReturn(Single.error(error));

    try (MockedStatic<CompletableFutureUtils> mockedCompletableFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedCompletableFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<HealthCheckResponse> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<HealthCheckResponse> future = healthCheck.healthcheck();

      try {
        // Wait a bit for async processing
        Thread.sleep(100);
        future.toCompletableFuture().get();
        // If health check is DOWN, it throws HealthCheckException
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertNotNull(e);
      }
    }
  }
}
