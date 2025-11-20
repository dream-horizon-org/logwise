package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.dto.request.MonitorSparkJobRequest;
import com.logwise.orchestrator.dto.response.DefaultSuccessResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.MonitorSparkJob;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.SparkService;
import com.logwise.orchestrator.setup.BaseTest;
import io.reactivex.Completable;
import java.util.concurrent.CompletionStage;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for MonitorSparkJob REST endpoint. */
public class MonitorSparkJobEndpointTest extends BaseTest {

  private SparkService mockSparkService;
  private MonitorSparkJob monitorSparkJob;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockSparkService = mock(SparkService.class);
    monitorSparkJob = new MonitorSparkJob(mockSparkService);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testHandle_WithValidRequest_ReturnsSuccessResponse() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    MonitorSparkJobRequest request = new MonitorSparkJobRequest();
    request.setDriverCores(2);
    request.setDriverMemoryInGb(4);

    when(mockSparkService.monitorSparkJob(any(Tenant.class), anyInt(), anyInt()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> future =
        monitorSparkJob.handle(tenantName, request);

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getData());
    Assert.assertNotNull(response.getData().getMessage());
    Assert.assertTrue(response.getData().getMessage().contains(tenantName));
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    verify(mockSparkService, times(1)).monitorSparkJob(eq(tenant), eq(2), eq(4));
  }

  @Test
  public void testHandle_WithNullRequest_CreatesDefaultRequest() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();

    when(mockSparkService.monitorSparkJob(any(Tenant.class), any(), any()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> future =
        monitorSparkJob.handle(tenantName, null);

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    verify(mockSparkService, times(1)).monitorSparkJob(eq(tenant), any(), any());
  }

  @Test
  public void testHandle_WithDifferentTenant_CallsServiceWithCorrectTenant() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    MonitorSparkJobRequest request = new MonitorSparkJobRequest();
    request.setDriverCores(4);
    request.setDriverMemoryInGb(8);

    when(mockSparkService.monitorSparkJob(any(Tenant.class), anyInt(), anyInt()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> future =
        monitorSparkJob.handle(tenantName, request);

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    verify(mockSparkService, times(1)).monitorSparkJob(eq(tenant), eq(4), eq(8));
  }

  @Test
  public void testHandle_WithServiceError_StillReturnsSuccess() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    MonitorSparkJobRequest request = new MonitorSparkJobRequest();
    request.setDriverCores(2);
    request.setDriverMemoryInGb(4);
    RuntimeException error = new RuntimeException("Service error");

    when(mockSparkService.monitorSparkJob(any(Tenant.class), anyInt(), anyInt()))
        .thenReturn(Completable.error(error));

    // The endpoint subscribes to the service but doesn't wait for completion
    // So it should still return success
    CompletionStage<Response<DefaultSuccessResponse>> future =
        monitorSparkJob.handle(tenantName, request);

    Response<DefaultSuccessResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertEquals(response.getHttpStatusCode(), 200);
  }
}
