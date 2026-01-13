package com.logwise.orchestrator.tests.unit.rest;

import static org.mockito.ArgumentMatchers.*;
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
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ScaleKafkaPartitionsRequest request = new ScaleKafkaPartitionsRequest();

    ScalingDecision decision = ScalingDecision.builder()
        .topic("test-topic")
        .currentPartitions(3)
        .newPartitions(6)
        .reason("lag threshold exceeded")
        .factors(Collections.singletonList("lag"))
        .build();

    List<ScalingDecision> decisions = Collections.singletonList(decision);

    when(mockKafkaService.scaleKafkaPartitions(eq(tenant)))
        .thenReturn(Single.just(decisions));

    CompletionStage<Response<ScaleKafkaPartitionsResponse>> future =
        scaleKafkaPartitions.scalePartitions(tenantName, request);

    Response<ScaleKafkaPartitionsResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getData());
    Assert.assertTrue(response.getData().isSuccess());
    Assert.assertEquals(response.getData().getTopicsScaled(), 1);
    Assert.assertEquals(response.getData().getScalingDecisions().size(), 1);
    Assert.assertEquals(response.getHttpStatusCode(), 200);
    verify(mockKafkaService, times(1)).scaleKafkaPartitions(eq(tenant));
  }

  @Test
  public void testScalePartitions_WithNullRequest_CreatesDefaultRequest() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();

    when(mockKafkaService.scaleKafkaPartitions(eq(tenant)))
        .thenReturn(Single.just(Collections.emptyList()));

    CompletionStage<Response<ScaleKafkaPartitionsResponse>> future =
        scaleKafkaPartitions.scalePartitions(tenantName, null);

    Response<ScaleKafkaPartitionsResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getData());
    Assert.assertTrue(response.getData().isSuccess());
    Assert.assertEquals(response.getHttpStatusCode(), 200);
  }

  @Test
  public void testScalePartitions_WithNoScalingNeeded_ReturnsEmptyDecisions() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ScaleKafkaPartitionsRequest request = new ScaleKafkaPartitionsRequest();

    when(mockKafkaService.scaleKafkaPartitions(eq(tenant)))
        .thenReturn(Single.just(Collections.emptyList()));

    CompletionStage<Response<ScaleKafkaPartitionsResponse>> future =
        scaleKafkaPartitions.scalePartitions(tenantName, request);

    Response<ScaleKafkaPartitionsResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getData());
    Assert.assertTrue(response.getData().isSuccess());
    Assert.assertEquals(response.getData().getTopicsScaled(), 0);
    Assert.assertTrue(response.getData().getScalingDecisions().isEmpty());
    Assert.assertEquals(response.getHttpStatusCode(), 200);
  }

  @Test
  public void testScalePartitions_WithServiceError_ReturnsErrorResponse() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ScaleKafkaPartitionsRequest request = new ScaleKafkaPartitionsRequest();

    RuntimeException serviceError = new RuntimeException("Kafka service error");
    when(mockKafkaService.scaleKafkaPartitions(eq(tenant)))
        .thenReturn(Single.error(serviceError));

    CompletionStage<Response<ScaleKafkaPartitionsResponse>> future =
        scaleKafkaPartitions.scalePartitions(tenantName, request);

    Response<ScaleKafkaPartitionsResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getData());
    Assert.assertFalse(response.getData().isSuccess());
    Assert.assertEquals(response.getData().getTopicsScaled(), 0);
    Assert.assertFalse(response.getData().getErrors().isEmpty());
    Assert.assertEquals(response.getHttpStatusCode(), 500);
  }

  @Test
  public void testScalePartitions_WithInvalidTenant_ReturnsBadRequest() throws Exception {
    String tenantName = "INVALID_TENANT";
    ScaleKafkaPartitionsRequest request = new ScaleKafkaPartitionsRequest();

    CompletionStage<Response<ScaleKafkaPartitionsResponse>> future =
        scaleKafkaPartitions.scalePartitions(tenantName, request);

    Response<ScaleKafkaPartitionsResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getData());
    Assert.assertFalse(response.getData().isSuccess());
    Assert.assertEquals(response.getHttpStatusCode(), 400);
  }

  @Test
  public void testScalePartitions_WithMultipleDecisions_ReturnsAllDecisions() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();
    ScaleKafkaPartitionsRequest request = new ScaleKafkaPartitionsRequest();

    List<ScalingDecision> decisions = new ArrayList<>();
    decisions.add(ScalingDecision.builder()
        .topic("topic1")
        .currentPartitions(3)
        .newPartitions(6)
        .reason("lag")
        .factors(Collections.singletonList("lag"))
        .build());
    decisions.add(ScalingDecision.builder()
        .topic("topic2")
        .currentPartitions(5)
        .newPartitions(10)
        .reason("size")
        .factors(Collections.singletonList("size"))
        .build());

    when(mockKafkaService.scaleKafkaPartitions(eq(tenant)))
        .thenReturn(Single.just(decisions));

    CompletionStage<Response<ScaleKafkaPartitionsResponse>> future =
        scaleKafkaPartitions.scalePartitions(tenantName, request);

    Response<ScaleKafkaPartitionsResponse> response = future.toCompletableFuture().get();

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getData());
    Assert.assertTrue(response.getData().isSuccess());
    Assert.assertEquals(response.getData().getTopicsScaled(), 2);
    Assert.assertEquals(response.getData().getScalingDecisions().size(), 2);
    Assert.assertEquals(response.getHttpStatusCode(), 200);
  }
}

