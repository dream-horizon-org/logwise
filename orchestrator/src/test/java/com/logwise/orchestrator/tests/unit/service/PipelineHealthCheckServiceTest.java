package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.response.SparkMasterJsonResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.ObjectStoreFactory;
import com.logwise.orchestrator.service.PipelineHealthCheckService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.webclient.reactivex.client.WebClient;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for PipelineHealthCheckService. */
@SuppressWarnings("unchecked")
public class PipelineHealthCheckServiceTest extends BaseTest {

  private PipelineHealthCheckService pipelineHealthCheckService;
  private WebClient mockWebClient;
  private ObjectMapper objectMapper;
  private io.vertx.reactivex.ext.web.client.WebClient mockVertxWebClient;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockWebClient = mock(WebClient.class);
    objectMapper = new ObjectMapper();
    pipelineHealthCheckService = new PipelineHealthCheckService(mockWebClient, objectMapper);
    mockVertxWebClient = mock(io.vertx.reactivex.ext.web.client.WebClient.class);
    when(mockWebClient.getWebClient()).thenReturn(mockVertxWebClient);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  // ==================== checkVectorHealth Tests ====================

  @Test
  public void testCheckVectorHealth_WithHealthyResponse_ReturnsUp() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.VectorConfig vectorConfig = new ApplicationConfig.VectorConfig();
    vectorConfig.setHost("vector.example.com");
    vectorConfig.setApiPort(8686);
    tenantConfig.setVector(vectorConfig);

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);

    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockVertxWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.rxSend()).thenReturn(Single.just(mockHttpResponse));

    JsonObject result = pipelineHealthCheckService.checkVectorHealth(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "UP");
    Assert.assertEquals(result.getString("message"), "Vector is healthy");
    Assert.assertEquals(result.getInteger("responseCode"), Integer.valueOf(200));
  }

  @Test
  public void testCheckVectorHealth_WithUnhealthyResponse_ReturnsDown() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.VectorConfig vectorConfig = new ApplicationConfig.VectorConfig();
    vectorConfig.setHost("vector.example.com");
    vectorConfig.setApiPort(8686);
    tenantConfig.setVector(vectorConfig);

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);

    when(mockHttpResponse.statusCode()).thenReturn(500);
    when(mockVertxWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.rxSend()).thenReturn(Single.just(mockHttpResponse));

    JsonObject result = pipelineHealthCheckService.checkVectorHealth(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "DOWN");
    Assert.assertTrue(result.getString("message").contains("Vector health check returned 500"));
    Assert.assertEquals(result.getInteger("responseCode"), Integer.valueOf(500));
  }

  @Test
  public void testCheckVectorHealth_WithError_ReturnsDown() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.VectorConfig vectorConfig = new ApplicationConfig.VectorConfig();
    vectorConfig.setHost("vector.example.com");
    vectorConfig.setApiPort(8686);
    tenantConfig.setVector(vectorConfig);

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);

    when(mockVertxWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.rxSend())
        .thenReturn(Single.error(new RuntimeException("Connection refused")));

    JsonObject result = pipelineHealthCheckService.checkVectorHealth(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "DOWN");
    Assert.assertTrue(result.getString("message").contains("Vector health check failed"));
    Assert.assertEquals(result.getString("error"), "RuntimeException");
  }

  @Test
  public void testCheckVectorHealth_WithTimeout_ReturnsDown() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.VectorConfig vectorConfig = new ApplicationConfig.VectorConfig();
    vectorConfig.setHost("vector.example.com");
    vectorConfig.setApiPort(8686);
    tenantConfig.setVector(vectorConfig);

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);

    when(mockVertxWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
    when(mockHttpRequest.rxSend())
        .thenReturn(
            Single
                .<io.vertx.reactivex.ext.web.client.HttpResponse<
                        io.vertx.reactivex.core.buffer.Buffer>>
                    just(mockHttpResponse)
                .delay(10, java.util.concurrent.TimeUnit.SECONDS));

    JsonObject result = pipelineHealthCheckService.checkVectorHealth(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "DOWN");
    Assert.assertEquals(result.getString("message"), "Vector health check timed out");
  }

  // ==================== checkKafkaHealth Tests ====================

  @Test
  public void testCheckKafkaHealth_WithValidConfig_ReturnsUp() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.KafkaConfig kafkaConfig = new ApplicationConfig.KafkaConfig();
    kafkaConfig.setKafkaBrokersHost("kafka.example.com");
    kafkaConfig.setKafkaBrokerPort(9092);
    tenantConfig.setKafka(kafkaConfig);

    JsonObject result = pipelineHealthCheckService.checkKafkaHealth(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "UP");
    Assert.assertEquals(result.getString("message"), "Kafka broker is reachable");
    Assert.assertEquals(result.getString("host"), "kafka.example.com");
    Assert.assertEquals(result.getInteger("port"), Integer.valueOf(9092));
  }

  @Test
  public void testCheckKafkaHealth_WithNullPort_ReturnsUp() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.KafkaConfig kafkaConfig = new ApplicationConfig.KafkaConfig();
    kafkaConfig.setKafkaBrokersHost("kafka.example.com");
    kafkaConfig.setKafkaBrokerPort(null);
    tenantConfig.setKafka(kafkaConfig);

    JsonObject result = pipelineHealthCheckService.checkKafkaHealth(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "UP");
    Assert.assertFalse(result.containsKey("port"));
  }

  // ==================== checkKafkaTopics Tests ====================

  @Test
  public void testCheckKafkaTopics_ReturnsUnknownStatus() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setSubscribePattern("test-topic-*");

    JsonObject result = pipelineHealthCheckService.checkKafkaTopics(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "UNKNOWN");
    Assert.assertTrue(result.getString("message").contains("Kafka topic check requires"));
    Assert.assertEquals(result.getString("pattern"), "test-topic-*");
  }

  // ==================== checkSparkHealth Tests ====================

  @Test
  public void testCheckSparkHealth_WithRunningDriver_ReturnsUp() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setSparkMasterHost("spark-master.example.com");

    SparkMasterJsonResponse sparkResponse = new SparkMasterJsonResponse();
    List<SparkMasterJsonResponse.Driver> drivers = new ArrayList<>();
    SparkMasterJsonResponse.Driver driver = new SparkMasterJsonResponse.Driver();
    driver.setState("RUNNING");
    drivers.add(driver);
    sparkResponse.setActivedrivers(drivers);

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);

    try {
      when(mockHttpResponse.bodyAsString())
          .thenReturn(objectMapper.writeValueAsString(sparkResponse));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    when(mockVertxWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.rxSend()).thenReturn(Single.just(mockHttpResponse));

    JsonObject result = pipelineHealthCheckService.checkSparkHealth(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "UP");
    Assert.assertEquals(result.getString("message"), "Spark driver is running");
    Assert.assertEquals(result.getInteger("drivers"), Integer.valueOf(1));
  }

  @Test
  public void testCheckSparkHealth_WithNoRunningDriver_ReturnsDown() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setSparkMasterHost("spark-master.example.com");

    SparkMasterJsonResponse sparkResponse = new SparkMasterJsonResponse();
    List<SparkMasterJsonResponse.Driver> drivers = new ArrayList<>();
    SparkMasterJsonResponse.Driver driver1 = new SparkMasterJsonResponse.Driver();
    driver1.setState("FINISHED");
    drivers.add(driver1);
    SparkMasterJsonResponse.Driver driver2 = new SparkMasterJsonResponse.Driver();
    driver2.setState("FAILED");
    drivers.add(driver2);
    sparkResponse.setActivedrivers(drivers);

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);

    try {
      when(mockHttpResponse.bodyAsString())
          .thenReturn(objectMapper.writeValueAsString(sparkResponse));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    when(mockVertxWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.rxSend()).thenReturn(Single.just(mockHttpResponse));

    JsonObject result = pipelineHealthCheckService.checkSparkHealth(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "DOWN");
    Assert.assertEquals(result.getString("message"), "No running Spark driver found");
    Assert.assertEquals(result.getInteger("drivers"), Integer.valueOf(2));
  }

  @Test
  public void testCheckSparkHealth_WithEmptyDrivers_ReturnsDown() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setSparkMasterHost("spark-master.example.com");

    SparkMasterJsonResponse sparkResponse = new SparkMasterJsonResponse();
    sparkResponse.setActivedrivers(Collections.emptyList());

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);

    try {
      when(mockHttpResponse.bodyAsString())
          .thenReturn(objectMapper.writeValueAsString(sparkResponse));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    when(mockVertxWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.rxSend()).thenReturn(Single.just(mockHttpResponse));

    JsonObject result = pipelineHealthCheckService.checkSparkHealth(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "DOWN");
    Assert.assertEquals(result.getString("message"), "No running Spark driver found");
    Assert.assertEquals(result.getInteger("drivers"), Integer.valueOf(0));
  }

  @Test
  public void testCheckSparkHealth_WithParseError_ReturnsDown() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setSparkMasterHost("spark-master.example.com");

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);

    when(mockHttpResponse.bodyAsString()).thenReturn("invalid json");
    when(mockVertxWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.rxSend()).thenReturn(Single.just(mockHttpResponse));

    JsonObject result = pipelineHealthCheckService.checkSparkHealth(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "DOWN");
    Assert.assertTrue(result.getString("message").contains("Failed to parse Spark response"));
  }

  @Test
  public void testCheckSparkHealth_WithError_ReturnsDown() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setSparkMasterHost("spark-master.example.com");

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);

    when(mockVertxWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.rxSend())
        .thenReturn(Single.error(new RuntimeException("Connection refused")));

    JsonObject result = pipelineHealthCheckService.checkSparkHealth(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "DOWN");
    Assert.assertTrue(result.getString("message").contains("Spark health check failed"));
  }

  @Test
  public void testCheckSparkHealth_WithTimeout_ReturnsDown() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setSparkMasterHost("spark-master.example.com");

    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);

    when(mockVertxWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
    when(mockHttpRequest.rxSend())
        .thenReturn(
            Single
                .<io.vertx.reactivex.ext.web.client.HttpResponse<
                        io.vertx.reactivex.core.buffer.Buffer>>
                    just(mockHttpResponse)
                .delay(10, java.util.concurrent.TimeUnit.SECONDS));

    JsonObject result = pipelineHealthCheckService.checkSparkHealth(tenantConfig).blockingGet();

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getString("status"), "DOWN");
    Assert.assertEquals(result.getString("message"), "Spark health check timed out");
  }

  // ==================== checkS3Logs Tests ====================

  @Test
  public void testCheckS3Logs_WithRecentLogs_ReturnsUp() {
    Tenant tenant = Tenant.ABC;
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setLogsDir("logs");

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
        Mockito.mockStatic(ObjectStoreFactory.class)) {
      ObjectStoreClient mockObjectStoreClient = mock(ObjectStoreClient.class);

      // Mock listCommonPrefix to return prefixes
      List<String> prefixes = Arrays.asList("logs/service1/", "logs/service2/");
      when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
          .thenReturn(Single.just(prefixes));

      // Mock listObjects to return recent objects (with current hour)
      int currentHour = java.time.LocalDateTime.now().getHour();
      List<String> objects =
          Arrays.asList(
              String.format("logs/service1/env=prod/hour=%02d/minute=00/file1.log", currentHour),
              String.format("logs/service2/env=dev/hour=%02d/minute=30/file2.log", currentHour));
      when(mockObjectStoreClient.listObjects(anyString())).thenReturn(Single.just(objects));

      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      JsonObject result =
          pipelineHealthCheckService.checkS3Logs(tenant, tenantConfig).blockingGet();

      Assert.assertNotNull(result);
      Assert.assertEquals(result.getString("status"), "UP");
      Assert.assertEquals(result.getString("message"), "Recent logs found in S3");
      Assert.assertTrue(result.getInteger("recentObjects") > 0);
      Assert.assertEquals(result.getInteger("totalPrefixes"), Integer.valueOf(2));
    }
  }

  @Test
  public void testCheckS3Logs_WithNoRecentLogs_ReturnsWarning() {
    Tenant tenant = Tenant.ABC;
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setLogsDir("logs");

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
        Mockito.mockStatic(ObjectStoreFactory.class)) {
      ObjectStoreClient mockObjectStoreClient = mock(ObjectStoreClient.class);

      // Mock listCommonPrefix to return prefixes
      List<String> prefixes = Arrays.asList("logs/service1/", "logs/service2/");
      when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
          .thenReturn(Single.just(prefixes));

      // Mock listObjects to return old objects (previous hour)
      int previousHour = (java.time.LocalDateTime.now().getHour() + 23) % 24;
      List<String> objects =
          Arrays.asList(
              String.format("logs/service1/env=prod/hour=%02d/minute=00/file1.log", previousHour),
              String.format("logs/service2/env=dev/hour=%02d/minute=30/file2.log", previousHour));
      when(mockObjectStoreClient.listObjects(anyString())).thenReturn(Single.just(objects));

      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      JsonObject result =
          pipelineHealthCheckService.checkS3Logs(tenant, tenantConfig).blockingGet();

      Assert.assertNotNull(result);
      Assert.assertEquals(result.getString("status"), "WARNING");
      Assert.assertEquals(result.getString("message"), "No recent logs found in S3 (last hour)");
      Assert.assertEquals(result.getInteger("totalPrefixes"), Integer.valueOf(2));
    }
  }

  @Test
  public void testCheckS3Logs_WithNoPrefixes_ReturnsWarning() {
    Tenant tenant = Tenant.ABC;
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setLogsDir("logs");

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
        Mockito.mockStatic(ObjectStoreFactory.class)) {
      ObjectStoreClient mockObjectStoreClient = mock(ObjectStoreClient.class);

      // Mock listCommonPrefix to return empty list
      when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
          .thenReturn(Single.just(Collections.emptyList()));

      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      JsonObject result =
          pipelineHealthCheckService.checkS3Logs(tenant, tenantConfig).blockingGet();

      Assert.assertNotNull(result);
      Assert.assertEquals(result.getString("status"), "WARNING");
      Assert.assertEquals(result.getString("message"), "No log prefixes found in S3");
      Assert.assertEquals(result.getString("logsDir"), "logs");
    }
  }

  @Test
  public void testCheckS3Logs_WithError_ReturnsDown() {
    Tenant tenant = Tenant.ABC;
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setLogsDir("logs");

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
        Mockito.mockStatic(ObjectStoreFactory.class)) {
      ObjectStoreClient mockObjectStoreClient = mock(ObjectStoreClient.class);

      when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
          .thenReturn(Single.error(new RuntimeException("S3 access denied")));

      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      JsonObject result =
          pipelineHealthCheckService.checkS3Logs(tenant, tenantConfig).blockingGet();

      Assert.assertNotNull(result);
      Assert.assertEquals(result.getString("status"), "DOWN");
      Assert.assertTrue(result.getString("message").contains("S3 logs check failed"));
    }
  }

  @Test
  public void testCheckS3Logs_WithTimeout_ReturnsDown() {
    Tenant tenant = Tenant.ABC;
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setLogsDir("logs");

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
        Mockito.mockStatic(ObjectStoreFactory.class)) {
      ObjectStoreClient mockObjectStoreClient = mock(ObjectStoreClient.class);

      when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
          .thenReturn(
              Single.<List<String>>just(Collections.emptyList())
                  .delay(15, java.util.concurrent.TimeUnit.SECONDS));

      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      JsonObject result =
          pipelineHealthCheckService.checkS3Logs(tenant, tenantConfig).blockingGet();

      Assert.assertNotNull(result);
      Assert.assertEquals(result.getString("status"), "DOWN");
      Assert.assertEquals(result.getString("message"), "S3 logs check timed out");
    }
  }

  // ==================== checkCompletePipeline Tests ====================

  @Test
  public void testCheckCompletePipeline_WithAllComponentsUp_ReturnsUp() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      ApplicationConfig.VectorConfig vectorConfig = new ApplicationConfig.VectorConfig();
      vectorConfig.setHost("vector.example.com");
      vectorConfig.setApiPort(8686);
      tenantConfig.setVector(vectorConfig);

      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock Vector health check
      io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
          mockVectorRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
      io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
          mockVectorResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
      when(mockVectorResponse.statusCode()).thenReturn(200);
      when(mockVertxWebClient.getAbs(contains("/health"))).thenReturn(mockVectorRequest);
      when(mockVectorRequest.rxSend()).thenReturn(Single.just(mockVectorResponse));

      // Mock Spark health check
      io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
          mockSparkRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
      io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
          mockSparkResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
      SparkMasterJsonResponse sparkResponse = new SparkMasterJsonResponse();
      List<SparkMasterJsonResponse.Driver> drivers = new ArrayList<>();
      SparkMasterJsonResponse.Driver driver = new SparkMasterJsonResponse.Driver();
      driver.setState("RUNNING");
      drivers.add(driver);
      sparkResponse.setActivedrivers(drivers);
      try {
        when(mockSparkResponse.bodyAsString())
            .thenReturn(objectMapper.writeValueAsString(sparkResponse));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      when(mockVertxWebClient.getAbs(contains("/json"))).thenReturn(mockSparkRequest);
      when(mockSparkRequest.rxSend()).thenReturn(Single.just(mockSparkResponse));

      // Mock S3 logs check
      ObjectStoreClient mockObjectStoreClient = mock(ObjectStoreClient.class);
      List<String> prefixes = Arrays.asList("logs/service1/");
      when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
          .thenReturn(Single.just(prefixes));
      int currentHour = java.time.LocalDateTime.now().getHour();
      List<String> objects =
          Arrays.asList(
              String.format("logs/service1/env=prod/hour=%02d/minute=00/file1.log", currentHour));
      when(mockObjectStoreClient.listObjects(anyString())).thenReturn(Single.just(objects));
      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      JsonObject result = pipelineHealthCheckService.checkCompletePipeline(tenant).blockingGet();

      Assert.assertNotNull(result);
      Assert.assertEquals(result.getString("status"), "UP");
      Assert.assertEquals(result.getString("message"), "All pipeline components are healthy");
      Assert.assertEquals(result.getString("tenant"), "ABC");
      Assert.assertTrue(result.containsKey("checks"));
      io.vertx.core.json.JsonArray checks = result.getJsonArray("checks");
      Assert.assertNotNull(checks);
      Assert.assertEquals(checks.size(), 4);
    }
  }

  @Test
  public void testCheckCompletePipeline_WithOneComponentDown_ReturnsDown() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      ApplicationConfig.VectorConfig vectorConfig = new ApplicationConfig.VectorConfig();
      vectorConfig.setHost("vector.example.com");
      vectorConfig.setApiPort(8686);
      tenantConfig.setVector(vectorConfig);

      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock Vector health check - UP
      io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
          mockVectorRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
      io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
          mockVectorResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
      when(mockVectorResponse.statusCode()).thenReturn(200);
      when(mockVertxWebClient.getAbs(contains("/health"))).thenReturn(mockVectorRequest);
      when(mockVectorRequest.rxSend()).thenReturn(Single.just(mockVectorResponse));

      // Mock Spark health check - DOWN (no running driver)
      io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
          mockSparkRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
      io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
          mockSparkResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
      SparkMasterJsonResponse sparkResponse = new SparkMasterJsonResponse();
      sparkResponse.setActivedrivers(Collections.emptyList());
      try {
        when(mockSparkResponse.bodyAsString())
            .thenReturn(objectMapper.writeValueAsString(sparkResponse));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      when(mockVertxWebClient.getAbs(contains("/json"))).thenReturn(mockSparkRequest);
      when(mockSparkRequest.rxSend()).thenReturn(Single.just(mockSparkResponse));

      // Mock S3 logs check - UP
      ObjectStoreClient mockObjectStoreClient = mock(ObjectStoreClient.class);
      List<String> prefixes = Arrays.asList("logs/service1/");
      when(mockObjectStoreClient.listCommonPrefix(anyString(), anyString()))
          .thenReturn(Single.just(prefixes));
      int currentHour = java.time.LocalDateTime.now().getHour();
      List<String> objects =
          Arrays.asList(
              String.format("logs/service1/env=prod/hour=%02d/minute=00/file1.log", currentHour));
      when(mockObjectStoreClient.listObjects(anyString())).thenReturn(Single.just(objects));
      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      JsonObject result = pipelineHealthCheckService.checkCompletePipeline(tenant).blockingGet();

      Assert.assertNotNull(result);
      Assert.assertEquals(result.getString("status"), "DOWN");
      Assert.assertEquals(
          result.getString("message"), "One or more pipeline components have issues");
      Assert.assertTrue(result.containsKey("checks"));
    }
  }

  @Test
  public void testCheckCompletePipeline_WithError_ReturnsDown() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
        Mockito.mockStatic(ApplicationConfigUtil.class)) {
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenThrow(new RuntimeException("Config error"));

      JsonObject result = pipelineHealthCheckService.checkCompletePipeline(tenant).blockingGet();

      Assert.assertNotNull(result);
      Assert.assertEquals(result.getString("status"), "DOWN");
      Assert.assertTrue(result.getString("message").contains("Pipeline health check failed"));
      Assert.assertEquals(result.getString("error"), "RuntimeException");
    }
  }
}
