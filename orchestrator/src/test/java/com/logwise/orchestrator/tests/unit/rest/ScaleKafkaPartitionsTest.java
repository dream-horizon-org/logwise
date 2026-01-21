package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.dto.kafka.ScalingDecision;
import com.logwise.orchestrator.dto.request.ScaleKafkaPartitionsRequest;
import com.logwise.orchestrator.dto.response.ScaleKafkaPartitionsResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.rest.ScaleKafkaPartitions;
import com.logwise.orchestrator.rest.io.Response;
import com.logwise.orchestrator.service.KafkaService;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ScaleKafkaPartitionsTest {

  private ScaleKafkaPartitions scaleKafkaPartitions;
  private KafkaService mockKafkaService;

  @BeforeMethod
  public void setUp() {
    mockKafkaService = mock(KafkaService.class);
    scaleKafkaPartitions = new ScaleKafkaPartitions(mockKafkaService);
  }

  @Test
  public void testScalePartitions_WithValidRequest_ReturnsSuccess() throws Exception {
    String tenantName = "ABC";
    ScaleKafkaPartitionsRequest request = new ScaleKafkaPartitionsRequest();
    List<ScalingDecision> decisions = new ArrayList<>();

    when(mockKafkaService.scaleKafkaPartitions(any(Tenant.class)))
        .thenReturn(Single.just(Collections.emptyMap()));

    CompletionStage<Response<ScaleKafkaPartitionsResponse>> result =
        scaleKafkaPartitions.scalePartitions(tenantName, request);

    Response<ScaleKafkaPartitionsResponse> response = result.toCompletableFuture().get();
    Assert.assertNotNull(response);
    Assert.assertTrue(response.getData().isSuccess());
    Assert.assertEquals(response.getData().getTopicsScaled(), 0);
    verify(mockKafkaService, times(1)).scaleKafkaPartitions(Tenant.ABC);
  }

  @Test
  public void testScalePartitions_WithNullRequest_CreatesDefaultRequest() throws Exception {
    String tenantName = "ABC";

    when(mockKafkaService.scaleKafkaPartitions(any(Tenant.class)))
        .thenReturn(Single.just(Collections.emptyMap()));

    CompletionStage<Response<ScaleKafkaPartitionsResponse>> result =
        scaleKafkaPartitions.scalePartitions(tenantName, null);

    Response<ScaleKafkaPartitionsResponse> response = result.toCompletableFuture().get();
    Assert.assertNotNull(response);
    Assert.assertTrue(response.getData().isSuccess());
  }

  @Test
  public void testScalePartitions_WithError_ReturnsErrorResponse() throws Exception {
    String tenantName = "ABC";
    ScaleKafkaPartitionsRequest request = new ScaleKafkaPartitionsRequest();
    RuntimeException error = new RuntimeException("Test error");

    when(mockKafkaService.scaleKafkaPartitions(any(Tenant.class))).thenReturn(Single.error(error));

    CompletionStage<Response<ScaleKafkaPartitionsResponse>> result =
        scaleKafkaPartitions.scalePartitions(tenantName, request);

    Response<ScaleKafkaPartitionsResponse> response = result.toCompletableFuture().get();
    Assert.assertNotNull(response);
    Assert.assertFalse(response.getData().isSuccess());
    Assert.assertFalse(response.getData().getErrors().isEmpty());
  }

  @Test
  public void testScalePartitions_WithInvalidTenant_ReturnsBadRequest() throws Exception {
    String tenantName = "INVALID";
    ScaleKafkaPartitionsRequest request = new ScaleKafkaPartitionsRequest();

    CompletionStage<Response<ScaleKafkaPartitionsResponse>> result =
        scaleKafkaPartitions.scalePartitions(tenantName, request);

    Response<ScaleKafkaPartitionsResponse> response = result.toCompletableFuture().get();
    Assert.assertNotNull(response);
    Assert.assertFalse(response.getData().isSuccess());
  }
}
