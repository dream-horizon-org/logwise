package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.response.SparkMasterJsonResponse;
import com.logwise.orchestrator.dto.response.SparkMasterJsonResponse.Driver;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.ObjectStoreFactory;
import com.logwise.orchestrator.service.PipelineHealthCheckService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.webclient.reactivex.client.WebClient;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PipelineHealthCheckServiceTest extends BaseTest {

  private PipelineHealthCheckService service;
  private WebClient mockWebClient;
  private ObjectMapper mockObjectMapper;
  private io.vertx.reactivex.ext.web.client.WebClient mockRxWebClient;
  private ApplicationConfig.TenantConfig mockTenantConfig;
  private ApplicationConfig.VectorConfig mockVectorConfig;
  private ApplicationConfig.SparkConfig mockSparkConfig;
  private ApplicationConfig.KafkaConfig mockKafkaConfig;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockWebClient = mock(WebClient.class);
    mockObjectMapper = mock(ObjectMapper.class);
    mockRxWebClient = mock(io.vertx.reactivex.ext.web.client.WebClient.class);
    mockTenantConfig = mock(ApplicationConfig.TenantConfig.class);
    mockVectorConfig = mock(ApplicationConfig.VectorConfig.class);
    mockSparkConfig = mock(ApplicationConfig.SparkConfig.class);
    mockKafkaConfig = mock(ApplicationConfig.KafkaConfig.class);

    when(mockWebClient.getWebClient()).thenReturn(mockRxWebClient);
    when(mockTenantConfig.getName()).thenReturn("ABC");
    when(mockTenantConfig.getVector()).thenReturn(mockVectorConfig);
    when(mockTenantConfig.getSpark()).thenReturn(mockSparkConfig);
    when(mockTenantConfig.getKafka()).thenReturn(mockKafkaConfig);
    when(mockVectorConfig.getHost()).thenReturn("vector-host");
    when(mockVectorConfig.getApiPort()).thenReturn(8686);
    when(mockSparkConfig.getSparkMasterHost()).thenReturn("spark-host");
    when(mockSparkConfig.getSubscribePattern()).thenReturn("logs.*");
    when(mockSparkConfig.getLogsDir()).thenReturn("/logs");
    when(mockKafkaConfig.getKafkaBrokersHost()).thenReturn("kafka-host");
    when(mockKafkaConfig.getKafkaBrokerPort()).thenReturn(9092);

    service = new PipelineHealthCheckService(mockWebClient, mockObjectMapper);
  }

  @Test
  public void testCheckVectorHealth_With200Response_ReturnsUp() throws Exception {
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    when(mockRxWebClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.rxSend()).thenReturn(Single.just(mockResponse));

    Single<JsonObject> result = service.checkVectorHealth(mockTenantConfig);

    JsonObject response = result.blockingGet();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getString("status"), "UP");
    Assert.assertEquals(response.getString("message"), "Vector is healthy");
  }

  @Test
  public void testCheckVectorHealth_WithNon200Response_ReturnsDown() throws Exception {
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(500);

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    when(mockRxWebClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.rxSend()).thenReturn(Single.just(mockResponse));

    Single<JsonObject> result = service.checkVectorHealth(mockTenantConfig);

    JsonObject response = result.blockingGet();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getString("status"), "DOWN");
  }

  @Test
  public void testCheckVectorHealth_WithError_ReturnsDown() throws Exception {
    RuntimeException error = new RuntimeException("Connection error");

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    when(mockRxWebClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.rxSend()).thenReturn(Single.error(error));

    Single<JsonObject> result = service.checkVectorHealth(mockTenantConfig);

    JsonObject response = result.blockingGet();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getString("status"), "DOWN");
    Assert.assertTrue(response.getString("message").contains("Vector health check failed"));
  }

  @Test
  public void testCheckKafkaHealth_ReturnsUp() throws Exception {
    Single<JsonObject> result = service.checkKafkaHealth(mockTenantConfig);

    JsonObject response = result.blockingGet();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getString("status"), "UP");
    Assert.assertEquals(response.getString("host"), "kafka-host");
    Assert.assertEquals(response.getInteger("port"), Integer.valueOf(9092));
  }

  @Test
  public void testCheckKafkaHealth_WithNullPort_ReturnsUpWithoutPort() throws Exception {
    when(mockKafkaConfig.getKafkaBrokerPort()).thenReturn(null);

    Single<JsonObject> result = service.checkKafkaHealth(mockTenantConfig);

    JsonObject response = result.blockingGet();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getString("status"), "UP");
    Assert.assertFalse(response.containsKey("port"));
  }

  @Test
  public void testCheckKafkaTopics_ReturnsUnknown() throws Exception {
    Single<JsonObject> result = service.checkKafkaTopics(mockTenantConfig);

    JsonObject response = result.blockingGet();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getString("status"), "UNKNOWN");
    Assert.assertTrue(response.getString("message").contains("Kafka Admin Client"));
    Assert.assertEquals(response.getString("pattern"), "logs.*");
  }

  @Test
  public void testCheckSparkHealth_WithRunningDriver_ReturnsUp() throws Exception {
    Driver driver = new Driver();
    driver.setState("RUNNING");
    SparkMasterJsonResponse sparkResponse = new SparkMasterJsonResponse();
    sparkResponse.setActivedrivers(Arrays.asList(driver));

    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
    when(mockResponse.bodyAsString()).thenReturn("{\"activedrivers\":[{\"state\":\"RUNNING\"}]}");

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    when(mockRxWebClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.rxSend()).thenReturn(Single.just(mockResponse));
    when(mockObjectMapper.readValue(anyString(), eq(SparkMasterJsonResponse.class)))
        .thenReturn(sparkResponse);

    Single<JsonObject> result = service.checkSparkHealth(mockTenantConfig);

    JsonObject response = result.blockingGet();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getString("status"), "UP");
    Assert.assertEquals(response.getString("message"), "Spark driver is running");
    Assert.assertEquals(response.getInteger("drivers"), Integer.valueOf(1));
  }

  @Test
  public void testCheckSparkHealth_WithNoRunningDriver_ReturnsDown() throws Exception {
    Driver driver = new Driver();
    driver.setState("FINISHED");
    SparkMasterJsonResponse sparkResponse = new SparkMasterJsonResponse();
    sparkResponse.setActivedrivers(Arrays.asList(driver));

    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
    when(mockResponse.bodyAsString()).thenReturn("{\"activedrivers\":[{\"state\":\"FINISHED\"}]}");

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    when(mockRxWebClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.rxSend()).thenReturn(Single.just(mockResponse));
    when(mockObjectMapper.readValue(anyString(), eq(SparkMasterJsonResponse.class)))
        .thenReturn(sparkResponse);

    Single<JsonObject> result = service.checkSparkHealth(mockTenantConfig);

    JsonObject response = result.blockingGet();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getString("status"), "DOWN");
    Assert.assertEquals(response.getString("message"), "No running Spark driver found");
  }

  @Test
  public void testCheckSparkHealth_WithParseError_ReturnsDown() throws Exception {
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
    when(mockResponse.bodyAsString()).thenReturn("invalid json");

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    when(mockRxWebClient.getAbs(anyString())).thenReturn(mockRequest);
    when(mockRequest.rxSend()).thenReturn(Single.just(mockResponse));
    when(mockObjectMapper.readValue(anyString(), eq(SparkMasterJsonResponse.class)))
        .thenThrow(new RuntimeException("Parse error"));

    Single<JsonObject> result = service.checkSparkHealth(mockTenantConfig);

    JsonObject response = result.blockingGet();
    Assert.assertNotNull(response);
    Assert.assertEquals(response.getString("status"), "DOWN");
    Assert.assertTrue(response.getString("message").contains("Failed to parse Spark response"));
  }

  @Test
  public void testCheckS3Logs_WithEmptyPrefixes_ReturnsWarning() throws Exception {
    Tenant tenant = Tenant.ABC;
    ObjectStoreClient mockObjectStoreClient = mock(ObjectStoreClient.class);

    when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
        .thenReturn(Single.just(Collections.emptyList()));

    try (MockedStatic<ObjectStoreFactory> mockedFactory = mockStatic(ObjectStoreFactory.class)) {
      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      Single<JsonObject> result = service.checkS3Logs(tenant, mockTenantConfig);

      JsonObject response = result.blockingGet();
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "WARNING");
      Assert.assertTrue(response.getString("message").contains("No log prefixes"));
    }
  }

  @Test
  public void testCheckS3Logs_WithRecentLogs_ReturnsUp() throws Exception {
    Tenant tenant = Tenant.ABC;
    ObjectStoreClient mockObjectStoreClient = mock(ObjectStoreClient.class);
    List<String> prefixes = Arrays.asList("logs/service1/", "logs/service2/");

    // Create object keys with current hour
    int currentHour = java.time.LocalDateTime.now().getHour();
    String hourPattern = String.format("hour=%02d", currentHour);
    List<String> objects =
        Arrays.asList(
            "logs/service1/" + hourPattern + "/file1.log",
            "logs/service2/" + hourPattern + "/file2.log");

    when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
        .thenReturn(Single.just(prefixes));
    when(mockObjectStoreClient.listObjects(anyString())).thenReturn(Single.just(objects));

    try (MockedStatic<ObjectStoreFactory> mockedFactory = mockStatic(ObjectStoreFactory.class)) {
      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      Single<JsonObject> result = service.checkS3Logs(tenant, mockTenantConfig);

      JsonObject response = result.blockingGet();
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "UP");
      Assert.assertTrue(response.getString("message").contains("Recent logs found"));
    }
  }

  @Test
  public void testCheckS3Logs_WithNoRecentLogs_ReturnsWarning() throws Exception {
    Tenant tenant = Tenant.ABC;
    ObjectStoreClient mockObjectStoreClient = mock(ObjectStoreClient.class);
    List<String> prefixes = Arrays.asList("logs/service1/");

    // Create object keys with old hour (not current hour)
    int oldHour = (java.time.LocalDateTime.now().getHour() + 1) % 24;
    String hourPattern = String.format("hour=%02d", oldHour);
    List<String> objects = Arrays.asList("logs/service1/" + hourPattern + "/file1.log");

    when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
        .thenReturn(Single.just(prefixes));
    when(mockObjectStoreClient.listObjects(anyString())).thenReturn(Single.just(objects));

    try (MockedStatic<ObjectStoreFactory> mockedFactory = mockStatic(ObjectStoreFactory.class)) {
      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      Single<JsonObject> result = service.checkS3Logs(tenant, mockTenantConfig);

      JsonObject response = result.blockingGet();
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "WARNING");
      Assert.assertTrue(response.getString("message").contains("No recent logs"));
    }
  }

  @Test
  public void testCheckCompletePipeline_WithAllUp_ReturnsUp() throws Exception {
    Tenant tenant = Tenant.ABC;

    // Mock all health checks to return UP
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockVectorResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
    when(mockVectorResponse.statusCode()).thenReturn(200);

    Driver driver = new Driver();
    driver.setState("RUNNING");
    SparkMasterJsonResponse sparkResponse = new SparkMasterJsonResponse();
    sparkResponse.setActivedrivers(Arrays.asList(driver));

    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockSparkResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
    when(mockSparkResponse.bodyAsString())
        .thenReturn("{\"activedrivers\":[{\"state\":\"RUNNING\"}]}");

    ObjectStoreClient mockObjectStoreClient = mock(ObjectStoreClient.class);
    List<String> prefixes = Arrays.asList("logs/service1/");
    int currentHour = java.time.LocalDateTime.now().getHour();
    String hourPattern = String.format("hour=%02d", currentHour);
    List<String> objects = Arrays.asList("logs/service1/" + hourPattern + "/file1.log");

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockVectorRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockSparkRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    when(mockRxWebClient.getAbs(contains("/health"))).thenReturn(mockVectorRequest);
    when(mockRxWebClient.getAbs(contains("/json"))).thenReturn(mockSparkRequest);
    when(mockVectorRequest.rxSend()).thenReturn(Single.just(mockVectorResponse));
    when(mockSparkRequest.rxSend()).thenReturn(Single.just(mockSparkResponse));
    when(mockObjectMapper.readValue(anyString(), eq(SparkMasterJsonResponse.class)))
        .thenReturn(sparkResponse);
    when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
        .thenReturn(Single.just(prefixes));
    when(mockObjectStoreClient.listObjects(anyString())).thenReturn(Single.just(objects));

    try (MockedStatic<ObjectStoreFactory> mockedFactory = mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<JsonObject> result = service.checkCompletePipeline(tenant);

      JsonObject response = result.blockingGet();
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "UP");
      Assert.assertTrue(response.getString("message").contains("All pipeline components"));
    }
  }

  @Test
  public void testCheckCompletePipeline_WithError_ReturnsDown() throws Exception {
    Tenant tenant = Tenant.ABC;
    RuntimeException error = new RuntimeException("Config error");

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
      mockedConfig.when(() -> ApplicationConfigUtil.getTenantConfig(tenant)).thenThrow(error);

      Single<JsonObject> result = service.checkCompletePipeline(tenant);

      JsonObject response = result.blockingGet();
      Assert.assertNotNull(response);
      Assert.assertEquals(response.getString("status"), "DOWN");
      Assert.assertTrue(response.getString("message").contains("Pipeline health check failed"));
    }
  }
}
