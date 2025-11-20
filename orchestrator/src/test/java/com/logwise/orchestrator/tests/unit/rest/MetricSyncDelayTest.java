package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.dto.response.LogSyncDelayResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.MetricSyncDelay;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.MetricsService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.ResponseWrapper;
import com.logwise.orchestrator.util.TestResponseWrapper;
import io.reactivex.Single;
import java.util.concurrent.CompletionStage;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for MetricSyncDelay REST endpoint. */
public class MetricSyncDelayTest extends BaseTest {

  private MetricsService mockMetricsService;
  private MetricSyncDelay metricSyncDelay;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockMetricsService = mock(MetricsService.class);
    metricSyncDelay = new MetricSyncDelay(mockMetricsService);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testGetObjectStoreSyncDelay_WithValidTenant_ReturnsSuccessResponse()
      throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    LogSyncDelayResponse delayResponse =
        LogSyncDelayResponse.builder().appLogsDelayMinutes(30).build();

    when(mockMetricsService.computeLogSyncDelay(any(Tenant.class)))
        .thenReturn(Single.just(delayResponse));

    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<LogSyncDelayResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<LogSyncDelayResponse>> future =
          metricSyncDelay.getObjectStoreSyncDelay(tenantName);

      Response<LogSyncDelayResponse> response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      Assert.assertNotNull(response.getData());
      Assert.assertEquals(response.getData().getAppLogsDelayMinutes(), Integer.valueOf(30));
      Assert.assertEquals(response.getHttpStatusCode(), 200);
      verify(mockMetricsService, times(1)).computeLogSyncDelay(eq(tenant));
    }
  }

  @Test
  public void testGetObjectStoreSyncDelay_WithDifferentTenant_CallsServiceWithCorrectTenant()
      throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    LogSyncDelayResponse delayResponse =
        LogSyncDelayResponse.builder().appLogsDelayMinutes(45).build();

    when(mockMetricsService.computeLogSyncDelay(any(Tenant.class)))
        .thenReturn(Single.just(delayResponse));

    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<LogSyncDelayResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<LogSyncDelayResponse>> future =
          metricSyncDelay.getObjectStoreSyncDelay(tenantName);

      Response<LogSyncDelayResponse> response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      verify(mockMetricsService, times(1)).computeLogSyncDelay(eq(tenant));
    }
  }

  @Test
  public void testGetObjectStoreSyncDelay_WithServiceError_PropagatesError() {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    RuntimeException error = new RuntimeException("Service error");

    when(mockMetricsService.computeLogSyncDelay(any(Tenant.class))).thenReturn(Single.error(error));

    try (MockedStatic<ResponseWrapper> mockedResponseWrapper =
        Mockito.mockStatic(ResponseWrapper.class)) {
      mockedResponseWrapper
          .when(() -> ResponseWrapper.fromSingle(any(Single.class), anyInt()))
          .thenAnswer(
              invocation -> {
                Single<LogSyncDelayResponse> single = invocation.getArgument(0);
                int statusCode = invocation.getArgument(1);
                return TestResponseWrapper.fromSingle(single, statusCode);
              });

      CompletionStage<Response<LogSyncDelayResponse>> future =
          metricSyncDelay.getObjectStoreSyncDelay(tenantName);

      try {
        future.toCompletableFuture().get();
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertNotNull(e);
      }
    }
  }
}
