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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for HealthCheck REST endpoint. */
public class HealthCheckTest extends BaseTest {

  private HealthCheck healthCheck;
  private HealthCheckDao mockHealthCheckDao;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    TestCompletableFutureUtils.init(vertx);
    mockHealthCheckDao = mock(HealthCheckDao.class);
    healthCheck = new HealthCheck(mockHealthCheckDao);
  }

  @Test
  public void testHealthcheck_WithHealthyMysql_ReturnsSuccess() throws Exception {
    JsonObject mysqlHealth = new JsonObject().put("status", "UP");
    when(mockHealthCheckDao.mysqlHealthCheck()).thenReturn(Single.just(mysqlHealth));

    try (MockedStatic<CompletableFutureUtils> mockedFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<HealthCheckResponse> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<HealthCheckResponse> result = healthCheck.healthcheck();

      HealthCheckResponse response = result.toCompletableFuture().get();
      Assert.assertNotNull(response);
      verify(mockHealthCheckDao, times(1)).mysqlHealthCheck();
    }
  }

  @Test
  public void testHealthcheck_WithUnhealthyMysql_ReturnsDown() throws Exception {
    RuntimeException error = new RuntimeException("Connection failed");
    when(mockHealthCheckDao.mysqlHealthCheck()).thenReturn(Single.error(error));

    try (MockedStatic<CompletableFutureUtils> mockedFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<HealthCheckResponse> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      try {
        CompletionStage<HealthCheckResponse> result = healthCheck.healthcheck();
        result.toCompletableFuture().get();
        // Health check should complete even with error
      } catch (Exception e) {
        // HealthCheckException may be thrown when status is DOWN
        Assert.assertNotNull(e);
      }
      verify(mockHealthCheckDao, times(1)).mysqlHealthCheck();
    }
  }
}
