package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.common.util.TestCompletableFutureUtils;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.PipelineHealthCheck;
import com.logwise.orchestrator.service.PipelineHealthCheckService;
import com.logwise.orchestrator.setup.BaseTest;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.CompletionStage;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for PipelineHealthCheck REST endpoint. */
public class PipelineHealthCheckTest extends BaseTest {

  private PipelineHealthCheck pipelineHealthCheck;
  private PipelineHealthCheckService mockPipelineHealthCheckService;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    TestCompletableFutureUtils.init(vertx);
    mockPipelineHealthCheckService = mock(PipelineHealthCheckService.class);
    pipelineHealthCheck = new PipelineHealthCheck(mockPipelineHealthCheckService);
  }

  @Test
  public void testCheckPipelineHealth_WithValidTenant_ReturnsHealthStatus() throws Exception {
    String tenantName = "ABC";
    JsonObject mockHealthStatus =
        new JsonObject().put("status", "UP").put("message", "All components healthy");

    when(mockPipelineHealthCheckService.checkCompletePipeline(any(Tenant.class)))
        .thenReturn(Single.just(mockHealthStatus));

    try (MockedStatic<CompletableFutureUtils> mockedFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> result = pipelineHealthCheck.checkPipelineHealth(tenantName);

      JsonObject response = result.toCompletableFuture().get();
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "UP");
      verify(mockPipelineHealthCheckService, times(1)).checkCompletePipeline(Tenant.ABC);
    }
  }

  @Test
  public void testCheckPipelineHealth_WithNullTenant_ReturnsError() throws Exception {
    try (MockedStatic<CompletableFutureUtils> mockedFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> result = pipelineHealthCheck.checkPipelineHealth(null);

      JsonObject response = result.toCompletableFuture().get();
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "ERROR");
      Assert.assertTrue(response.getString("message").contains("X-Tenant-Name header is required"));
      verify(mockPipelineHealthCheckService, never()).checkCompletePipeline(any(Tenant.class));
    }
  }

  @Test
  public void testCheckPipelineHealth_WithEmptyTenant_ReturnsError() throws Exception {
    try (MockedStatic<CompletableFutureUtils> mockedFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> result = pipelineHealthCheck.checkPipelineHealth("");

      JsonObject response = result.toCompletableFuture().get();
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "ERROR");
      verify(mockPipelineHealthCheckService, never()).checkCompletePipeline(any(Tenant.class));
    }
  }

  @Test
  public void testCheckPipelineHealth_WithInvalidTenant_ReturnsError() throws Exception {
    String tenantName = "INVALID";

    try (MockedStatic<CompletableFutureUtils> mockedFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> result = pipelineHealthCheck.checkPipelineHealth(tenantName);

      JsonObject response = result.toCompletableFuture().get();
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "ERROR");
      Assert.assertTrue(response.getString("message").contains("Invalid tenant name"));
      verify(mockPipelineHealthCheckService, never()).checkCompletePipeline(any(Tenant.class));
    }
  }

  @Test
  public void testCheckPipelineHealth_WithServiceError_ReturnsError() throws Exception {
    String tenantName = "ABC";
    RuntimeException error = new RuntimeException("Service error");

    when(mockPipelineHealthCheckService.checkCompletePipeline(any(Tenant.class)))
        .thenReturn(Single.error(error));

    try (MockedStatic<CompletableFutureUtils> mockedFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> result = pipelineHealthCheck.checkPipelineHealth(tenantName);

      try {
        result.toCompletableFuture().get();
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertNotNull(e);
      }
    }
  }
}
