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
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.concurrent.CompletionStage;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for PipelineHealthCheck REST endpoint. */
public class PipelineHealthCheckTest extends BaseTest {

  private PipelineHealthCheckService mockPipelineHealthCheckService;
  private PipelineHealthCheck pipelineHealthCheck;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    TestCompletableFutureUtils.init(vertx);
    mockPipelineHealthCheckService = mock(PipelineHealthCheckService.class);
    pipelineHealthCheck = new PipelineHealthCheck(mockPipelineHealthCheckService);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCheckPipelineHealth_WithAllComponentsHealthy_ReturnsUpStatus() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();

    JsonObject healthyResponse = createHealthyPipelineResponse(tenantName);
    when(mockPipelineHealthCheckService.checkCompletePipeline(any(Tenant.class)))
        .thenReturn(Single.just(healthyResponse));

    try (MockedStatic<CompletableFutureUtils> mockedCompletableFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedCompletableFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> future = pipelineHealthCheck.checkPipelineHealth(tenantName);

      Thread.sleep(100);
      JsonObject response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "UP");
      Assert.assertEquals(response.getString("message"), "All pipeline components are healthy");
      Assert.assertEquals(response.getString("tenant"), tenantName);
      Assert.assertTrue(response.containsKey("checks"));
      JsonArray checks = response.getJsonArray("checks");
      Assert.assertNotNull(checks);
      Assert.assertEquals(checks.size(), 4);

      verify(mockPipelineHealthCheckService, times(1)).checkCompletePipeline(eq(tenant));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCheckPipelineHealth_WithOneComponentDown_ReturnsDownStatus() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();

    JsonObject unhealthyResponse = createUnhealthyPipelineResponse(tenantName);
    when(mockPipelineHealthCheckService.checkCompletePipeline(any(Tenant.class)))
        .thenReturn(Single.just(unhealthyResponse));

    try (MockedStatic<CompletableFutureUtils> mockedCompletableFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedCompletableFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> future = pipelineHealthCheck.checkPipelineHealth(tenantName);

      Thread.sleep(100);
      JsonObject response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "DOWN");
      Assert.assertEquals(
          response.getString("message"), "One or more pipeline components have issues");
      Assert.assertEquals(response.getString("tenant"), tenantName);
      Assert.assertTrue(response.containsKey("checks"));

      verify(mockPipelineHealthCheckService, times(1)).checkCompletePipeline(eq(tenant));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCheckPipelineHealth_WithMissingTenantHeader_ReturnsError() throws Exception {
    try (MockedStatic<CompletableFutureUtils> mockedCompletableFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedCompletableFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> future = pipelineHealthCheck.checkPipelineHealth(null);

      Thread.sleep(100);
      JsonObject response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "ERROR");
      Assert.assertEquals(response.getString("message"), "X-Tenant-Name header is required");
      verify(mockPipelineHealthCheckService, never()).checkCompletePipeline(any(Tenant.class));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCheckPipelineHealth_WithEmptyTenantHeader_ReturnsError() throws Exception {
    try (MockedStatic<CompletableFutureUtils> mockedCompletableFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedCompletableFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> future = pipelineHealthCheck.checkPipelineHealth("");

      Thread.sleep(100);
      JsonObject response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "ERROR");
      Assert.assertEquals(response.getString("message"), "X-Tenant-Name header is required");
      verify(mockPipelineHealthCheckService, never()).checkCompletePipeline(any(Tenant.class));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCheckPipelineHealth_WithInvalidTenantName_ReturnsError() throws Exception {
    String invalidTenantName = "INVALID_TENANT";

    try (MockedStatic<CompletableFutureUtils> mockedCompletableFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedCompletableFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> future =
          pipelineHealthCheck.checkPipelineHealth(invalidTenantName);

      Thread.sleep(100);
      JsonObject response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "ERROR");
      Assert.assertTrue(response.getString("message").contains("Invalid tenant name"));
      verify(mockPipelineHealthCheckService, never()).checkCompletePipeline(any(Tenant.class));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCheckPipelineHealth_WithServiceError_ReturnsErrorResponse() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();

    // The service should handle errors and return a DOWN status, not propagate the
    // error
    JsonObject errorResponse = new JsonObject();
    errorResponse.put("status", "DOWN");
    errorResponse.put("message", "Pipeline health check failed: Service error");
    errorResponse.put("error", "RuntimeException");

    when(mockPipelineHealthCheckService.checkCompletePipeline(any(Tenant.class)))
        .thenReturn(Single.just(errorResponse));

    try (MockedStatic<CompletableFutureUtils> mockedCompletableFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedCompletableFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> future = pipelineHealthCheck.checkPipelineHealth(tenantName);

      Thread.sleep(100);
      JsonObject response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "DOWN");
      Assert.assertTrue(response.getString("message").contains("Pipeline health check failed"));

      verify(mockPipelineHealthCheckService, times(1)).checkCompletePipeline(eq(tenant));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCheckPipelineHealth_WithWarningStatus_ReturnsWarning() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();

    JsonObject warningResponse = createWarningPipelineResponse(tenantName);
    when(mockPipelineHealthCheckService.checkCompletePipeline(any(Tenant.class)))
        .thenReturn(Single.just(warningResponse));

    try (MockedStatic<CompletableFutureUtils> mockedCompletableFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedCompletableFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> future = pipelineHealthCheck.checkPipelineHealth(tenantName);

      Thread.sleep(100);
      JsonObject response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "DOWN");
      // When one component has WARNING, overall status should be DOWN
      Assert.assertTrue(response.containsKey("checks"));

      verify(mockPipelineHealthCheckService, times(1)).checkCompletePipeline(eq(tenant));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCheckPipelineHealth_WithDifferentTenant_CallsServiceWithCorrectTenant()
      throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();

    JsonObject healthyResponse = createHealthyPipelineResponse(tenantName);
    when(mockPipelineHealthCheckService.checkCompletePipeline(any(Tenant.class)))
        .thenReturn(Single.just(healthyResponse));

    try (MockedStatic<CompletableFutureUtils> mockedCompletableFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedCompletableFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> future = pipelineHealthCheck.checkPipelineHealth(tenantName);

      Thread.sleep(100);
      JsonObject response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      verify(mockPipelineHealthCheckService, times(1)).checkCompletePipeline(eq(tenant));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testCheckPipelineHealth_ResponseContainsAllComponents() throws Exception {
    Tenant tenant = Tenant.ABC;
    String tenantName = tenant.getValue();

    JsonObject healthyResponse = createHealthyPipelineResponse(tenantName);
    when(mockPipelineHealthCheckService.checkCompletePipeline(any(Tenant.class)))
        .thenReturn(Single.just(healthyResponse));

    try (MockedStatic<CompletableFutureUtils> mockedCompletableFutureUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedCompletableFutureUtils
          .when(() -> CompletableFutureUtils.fromSingle(any(Single.class)))
          .thenAnswer(
              invocation -> {
                Single<JsonObject> single = invocation.getArgument(0);
                return TestCompletableFutureUtils.fromSingle(single);
              });

      CompletionStage<JsonObject> future = pipelineHealthCheck.checkPipelineHealth(tenantName);

      Thread.sleep(100);
      JsonObject response = future.toCompletableFuture().get();

      Assert.assertNotNull(response);
      JsonArray checks = response.getJsonArray("checks");
      Assert.assertNotNull(checks);
      Assert.assertEquals(checks.size(), 4);

      // Verify all components are present
      boolean hasVector = false;
      boolean hasKafka = false;
      boolean hasSpark = false;
      boolean hasS3 = false;

      for (int i = 0; i < checks.size(); i++) {
        JsonObject check = checks.getJsonObject(i);
        String component = check.getString("component");
        if ("vector".equals(component)) hasVector = true;
        if ("kafka".equals(component)) hasKafka = true;
        if ("spark".equals(component)) hasSpark = true;
        if ("s3".equals(component)) hasS3 = true;
      }

      Assert.assertTrue(hasVector, "Response should contain vector component");
      Assert.assertTrue(hasKafka, "Response should contain kafka component");
      Assert.assertTrue(hasSpark, "Response should contain spark component");
      Assert.assertTrue(hasS3, "Response should contain s3 component");
    }
  }

  // Helper methods to create test responses

  private JsonObject createHealthyPipelineResponse(String tenantName) {
    JsonObject response = new JsonObject();
    response.put("status", "UP");
    response.put("message", "All pipeline components are healthy");
    response.put("tenant", tenantName);

    JsonArray checks = new JsonArray();
    checks.add(createComponentCheck("vector", "UP", "Vector is healthy"));
    checks.add(createComponentCheck("kafka", "UP", "Kafka broker is reachable"));
    checks.add(createComponentCheck("spark", "UP", "Spark driver is running"));
    checks.add(createComponentCheck("s3", "UP", "Recent logs found in S3"));

    response.put("checks", checks);
    return response;
  }

  private JsonObject createUnhealthyPipelineResponse(String tenantName) {
    JsonObject response = new JsonObject();
    response.put("status", "DOWN");
    response.put("message", "One or more pipeline components have issues");
    response.put("tenant", tenantName);

    JsonArray checks = new JsonArray();
    checks.add(createComponentCheck("vector", "UP", "Vector is healthy"));
    checks.add(createComponentCheck("kafka", "UP", "Kafka broker is reachable"));
    checks.add(createComponentCheck("spark", "DOWN", "No running Spark driver found"));
    checks.add(createComponentCheck("s3", "UP", "Recent logs found in S3"));

    response.put("checks", checks);
    return response;
  }

  private JsonObject createWarningPipelineResponse(String tenantName) {
    JsonObject response = new JsonObject();
    response.put("status", "DOWN");
    response.put("message", "One or more pipeline components have issues");
    response.put("tenant", tenantName);

    JsonArray checks = new JsonArray();
    checks.add(createComponentCheck("vector", "UP", "Vector is healthy"));
    checks.add(createComponentCheck("kafka", "UP", "Kafka broker is reachable"));
    checks.add(createComponentCheck("spark", "UP", "Spark driver is running"));
    checks.add(createComponentCheck("s3", "WARNING", "No recent logs found in S3 (last hour)"));

    response.put("checks", checks);
    return response;
  }

  private JsonObject createComponentCheck(String component, String status, String message) {
    JsonObject check = new JsonObject();
    check.put("component", component);
    JsonObject checkDetails = new JsonObject();
    checkDetails.put("status", status);
    checkDetails.put("message", message);
    check.put("check", checkDetails);
    return check;
  }
}
