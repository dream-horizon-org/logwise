package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for MonitorSparkJob REST endpoint. */
public class MonitorSparkJobTest extends BaseTest {

  private MonitorSparkJob monitorSparkJob;
  private SparkService mockSparkService;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockSparkService = mock(SparkService.class);
    monitorSparkJob = new MonitorSparkJob(mockSparkService);
  }

  @Test
  public void testHandle_WithValidRequest_ReturnsSuccess() throws Exception {
    String tenantName = "ABC";
    MonitorSparkJobRequest request = new MonitorSparkJobRequest();
    request.setDriverCores(4);
    request.setDriverMemoryInGb(8);

    when(mockSparkService.monitorSparkJob(any(Tenant.class), anyInt(), anyInt()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> result =
        monitorSparkJob.handle(tenantName, request);

    Response<DefaultSuccessResponse> response = result.toCompletableFuture().get();
    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getData());
    Assert.assertTrue(response.getData().getMessage().contains("Successfully monitored"));
    verify(mockSparkService, times(1)).monitorSparkJob(Tenant.ABC, 4, 8);
  }

  @Test
  public void testHandle_WithNullRequest_UsesDefaults() throws Exception {
    String tenantName = "ABC";

    when(mockSparkService.monitorSparkJob(any(Tenant.class), any(), any()))
        .thenReturn(Completable.complete());

    CompletionStage<Response<DefaultSuccessResponse>> result =
        monitorSparkJob.handle(tenantName, null);

    Response<DefaultSuccessResponse> response = result.toCompletableFuture().get();
    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getData());
    verify(mockSparkService, times(1)).monitorSparkJob(eq(Tenant.ABC), any(), any());
  }

  @Test
  public void testHandle_WithInvalidTenant_ThrowsException() {
    String tenantName = "INVALID";
    MonitorSparkJobRequest request = new MonitorSparkJobRequest();

    try {
      monitorSparkJob.handle(tenantName, request);
      Assert.fail("Should throw exception for invalid tenant");
    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }
}
