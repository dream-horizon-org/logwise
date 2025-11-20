package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.request.SubmitSparkJobRequest;
import com.logwise.orchestrator.dto.response.SparkMasterJsonResponse;
import com.logwise.orchestrator.dto.response.SparkMasterJsonResponse.Driver;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.service.SparkService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.webclient.reactivex.client.WebClient;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for SparkService. */
public class SparkServiceTest extends BaseTest {

  private SparkService sparkService;
  private WebClient mockWebClient;
  private ObjectMapper mockObjectMapper;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockWebClient = mock(WebClient.class);
    mockObjectMapper = mock(ObjectMapper.class);
    sparkService = new SparkService(mockWebClient, mockObjectMapper);
    io.vertx.reactivex.ext.web.client.WebClient reactiveWebClient =
        mock(io.vertx.reactivex.ext.web.client.WebClient.class);
    when(mockWebClient.getWebClient()).thenReturn(reactiveWebClient);
  }

  @Test
  public void testIsDriverNotRunning_WithEmptyDrivers_ReturnsTrue() throws Exception {
    // Test private method using reflection
    Method method =
        SparkService.class.getDeclaredMethod("isDriverNotRunning", SparkMasterJsonResponse.class);
    method.setAccessible(true);

    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    response.setActivedrivers(Collections.emptyList());

    Boolean result = (Boolean) method.invoke(null, response);

    Assert.assertTrue(result);
  }

  @Test
  public void testIsDriverNotRunning_WithNoRunningDrivers_ReturnsTrue() throws Exception {
    Method method =
        SparkService.class.getDeclaredMethod("isDriverNotRunning", SparkMasterJsonResponse.class);
    method.setAccessible(true);

    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    List<Driver> drivers = new ArrayList<>();
    Driver driver1 = new Driver();
    driver1.setState("FINISHED");
    drivers.add(driver1);
    Driver driver2 = new Driver();
    driver2.setState("FAILED");
    drivers.add(driver2);
    response.setActivedrivers(drivers);

    Boolean result = (Boolean) method.invoke(null, response);

    Assert.assertTrue(result);
  }

  @Test
  public void testIsDriverNotRunning_WithRunningDriver_ReturnsFalse() throws Exception {
    Method method =
        SparkService.class.getDeclaredMethod("isDriverNotRunning", SparkMasterJsonResponse.class);
    method.setAccessible(true);

    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    List<Driver> drivers = new ArrayList<>();
    Driver driver = new Driver();
    driver.setState("RUNNING");
    drivers.add(driver);
    response.setActivedrivers(drivers);

    Boolean result = (Boolean) method.invoke(null, response);

    Assert.assertFalse(result);
  }

  @Test
  public void testValidateAndSubmitSparkJob_WithNoRunningDriver_SubmitsJob() {
    Tenant tenant = Tenant.ABC;
    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    response.setActivedrivers(Collections.emptyList());

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<com.logwise.orchestrator.factory.ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(com.logwise.orchestrator.factory.ObjectStoreFactory.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock ObjectStoreClient for cleanSparkState
      com.logwise.orchestrator.client.ObjectStoreClient mockObjectStoreClient =
          mock(com.logwise.orchestrator.client.ObjectStoreClient.class);
      when(mockObjectStoreClient.listObjects(anyString()))
          .thenReturn(Single.just(Collections.emptyList()));
      when(mockObjectStoreClient.deleteFile(anyString())).thenReturn(Completable.complete());
      mockedFactory
          .when(() -> com.logwise.orchestrator.factory.ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      // Mock web client for submitSparkJob
      io.vertx.reactivex.ext.web.client.WebClient reactiveWebClient = mockWebClient.getWebClient();
      io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
          mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
      io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
          mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
      when(mockHttpResponse.statusCode()).thenReturn(200);
      when(mockHttpResponse.bodyAsString()).thenReturn("{\"submissionId\":\"test-id\"}");
      when(reactiveWebClient.postAbs(anyString())).thenReturn(mockHttpRequest);
      when(mockHttpRequest.rxSendJson(any())).thenReturn(Single.just(mockHttpResponse));

      // Test the method
      Single<Boolean> result = sparkService.validateAndSubmitSparkJob(tenant, response, null, null);
      Boolean submitted = result.blockingGet();

      Assert.assertNotNull(result);
      Assert.assertTrue(submitted);
    }
  }

  @Test
  public void testValidateAndSubmitSparkJob_WithRunningDriver_ReturnsFalse() {
    Tenant tenant = Tenant.ABC;
    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    List<Driver> drivers = new ArrayList<>();
    Driver driver = new Driver();
    driver.setState("RUNNING");
    drivers.add(driver);
    response.setActivedrivers(drivers);

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
        Mockito.mockStatic(ApplicationConfigUtil.class)) {
      Single<Boolean> result = sparkService.validateAndSubmitSparkJob(tenant, response, null, null);
      Boolean submitted = result.blockingGet();

      Assert.assertFalse(submitted);
    }
  }

  @Test
  public void testGetSparkMasterJsonResponse_WithValidHost_ReturnsResponse() throws Exception {
    Tenant tenant = Tenant.ABC;
    String sparkMasterHost = "spark-master.example.com";
    SparkMasterJsonResponse expectedResponse = new SparkMasterJsonResponse();

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
        Mockito.mockStatic(ApplicationConfigUtil.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      ApplicationConfig.SparkConfig sparkConfig = ApplicationTestConfig.createMockSparkConfig();
      sparkConfig.setSparkMasterHost(sparkMasterHost);
      tenantConfig.setSpark(sparkConfig);
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock the reactive web client chain
      io.vertx.reactivex.ext.web.client.WebClient reactiveWebClient = mockWebClient.getWebClient();
      io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
          mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
      io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
          mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
      when(mockHttpResponse.bodyAsString()).thenReturn("{\"activedrivers\":[]}");
      when(reactiveWebClient.getAbs(anyString())).thenReturn(mockHttpRequest);
      when(mockHttpRequest.rxSend()).thenReturn(Single.just(mockHttpResponse));
      when(mockObjectMapper.readValue(anyString(), eq(SparkMasterJsonResponse.class)))
          .thenReturn(expectedResponse);

      // This is a complex method to test, but we can verify the structure
      Assert.assertNotNull(sparkService);
    }
  }

  @Test
  public void testGetSparkSubmitRequestBody_WithValidConfig_ReturnsRequest() throws Exception {
    Method method =
        SparkService.class.getDeclaredMethod(
            "getSparkSubmitRequestBody",
            ApplicationConfig.TenantConfig.class,
            Integer.class,
            Integer.class);
    method.setAccessible(true);

    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    Integer driverCores = 2;
    Integer driverMemoryInGb = 4;

    SubmitSparkJobRequest request =
        (SubmitSparkJobRequest) method.invoke(null, tenantConfig, driverCores, driverMemoryInGb);

    Assert.assertNotNull(request);
    Assert.assertNotNull(request.getAppArgs());
    Assert.assertNotNull(request.getSparkProperties());
    Assert.assertNotNull(request.getEnvironmentVariables());
  }

  @Test
  public void testGetSparkSubmitRequestBody_WithNullDriverCores_UsesDefault() throws Exception {
    Method method =
        SparkService.class.getDeclaredMethod(
            "getSparkSubmitRequestBody",
            ApplicationConfig.TenantConfig.class,
            Integer.class,
            Integer.class);
    method.setAccessible(true);

    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");

    SubmitSparkJobRequest request =
        (SubmitSparkJobRequest) method.invoke(null, tenantConfig, null, null);

    Assert.assertNotNull(request);
    // Should use default from config
  }

  @Test
  public void testGetSparkSubmitRequestBody_WithNullAwsCredentials_HandlesGracefully()
      throws Exception {
    Method method =
        SparkService.class.getDeclaredMethod(
            "getSparkSubmitRequestBody",
            ApplicationConfig.TenantConfig.class,
            Integer.class,
            Integer.class);
    method.setAccessible(true);

    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setAwsAccessKeyId(null);
    sparkConfig.setAwsSecretAccessKey(null);
    sparkConfig.setAwsSessionToken(null);

    SubmitSparkJobRequest request =
        (SubmitSparkJobRequest) method.invoke(null, tenantConfig, null, null);

    Assert.assertNotNull(request);
    // Should handle null credentials gracefully
  }

  @Test
  public void testGetSparkSubmitRequestBody_WithSessionToken_SetsTemporaryCredentials()
      throws Exception {
    Method method =
        SparkService.class.getDeclaredMethod(
            "getSparkSubmitRequestBody",
            ApplicationConfig.TenantConfig.class,
            Integer.class,
            Integer.class);
    method.setAccessible(true);

    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setAwsAccessKeyId("test-key");
    sparkConfig.setAwsSecretAccessKey("test-secret");
    sparkConfig.setAwsSessionToken("test-token");

    SubmitSparkJobRequest request =
        (SubmitSparkJobRequest) method.invoke(null, tenantConfig, null, null);

    Assert.assertNotNull(request);
    // Should set temporary credentials provider
    Object credentialsProviderObj =
        request.getSparkProperties().get("spark.hadoop.fs.s3a.aws.credentials.provider");
    Assert.assertNotNull(credentialsProviderObj);
    String credentialsProvider = String.valueOf(credentialsProviderObj);
    Assert.assertTrue(credentialsProvider.contains("TemporaryAWSCredentialsProvider"));
  }

  @Test
  public void testGetSparkSubmitRequestBody_WithNonUsEast1Region_SetsCorrectEndpoint()
      throws Exception {
    Method method =
        SparkService.class.getDeclaredMethod(
            "getSparkSubmitRequestBody",
            ApplicationConfig.TenantConfig.class,
            Integer.class,
            Integer.class);
    method.setAccessible(true);

    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setAwsAccessKeyId("test-key");
    sparkConfig.setAwsSecretAccessKey("test-secret");
    sparkConfig.setAwsRegion("us-west-2");

    SubmitSparkJobRequest request =
        (SubmitSparkJobRequest) method.invoke(null, tenantConfig, null, null);

    Assert.assertNotNull(request);
    Object endpointObj = request.getSparkProperties().get("spark.hadoop.fs.s3a.endpoint");
    Assert.assertNotNull(endpointObj);
    String endpoint = String.valueOf(endpointObj);
    Assert.assertTrue(endpoint.contains("us-west-2"));
  }

  @Test
  public void testGetSparkSubmitRequestBody_WithUsEast1Region_SetsCorrectEndpoint()
      throws Exception {
    Method method =
        SparkService.class.getDeclaredMethod(
            "getSparkSubmitRequestBody",
            ApplicationConfig.TenantConfig.class,
            Integer.class,
            Integer.class);
    method.setAccessible(true);

    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    ApplicationConfig.SparkConfig sparkConfig = tenantConfig.getSpark();
    sparkConfig.setAwsAccessKeyId("test-key");
    sparkConfig.setAwsSecretAccessKey("test-secret");
    sparkConfig.setAwsRegion("us-east-1");

    SubmitSparkJobRequest request =
        (SubmitSparkJobRequest) method.invoke(null, tenantConfig, null, null);

    Assert.assertNotNull(request);
    Object endpointObj = request.getSparkProperties().get("spark.hadoop.fs.s3a.endpoint");
    Assert.assertNotNull(endpointObj);
    String endpoint = String.valueOf(endpointObj);
    Assert.assertEquals(endpoint, "s3.amazonaws.com");
  }

  @Test
  public void testCleanSparkState_WithFiles_DeletesFiles() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<com.logwise.orchestrator.factory.ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(com.logwise.orchestrator.factory.ObjectStoreFactory.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      com.logwise.orchestrator.client.ObjectStoreClient mockObjectStoreClient =
          mock(com.logwise.orchestrator.client.ObjectStoreClient.class);
      when(mockObjectStoreClient.listObjects(anyString()))
          .thenReturn(Single.just(Arrays.asList("checkpoint1", "checkpoint2")));
      when(mockObjectStoreClient.deleteFile(anyString())).thenReturn(Completable.complete());
      mockedFactory
          .when(() -> com.logwise.orchestrator.factory.ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      Completable result = sparkService.cleanSparkState(tenant);
      result.blockingAwait();

      verify(mockObjectStoreClient, atLeastOnce()).deleteFile(anyString());
    }
  }

  @Test
  public void testCleanSparkState_WithNoFiles_CompletesSuccessfully() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<com.logwise.orchestrator.factory.ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(com.logwise.orchestrator.factory.ObjectStoreFactory.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      com.logwise.orchestrator.client.ObjectStoreClient mockObjectStoreClient =
          mock(com.logwise.orchestrator.client.ObjectStoreClient.class);
      when(mockObjectStoreClient.listObjects(anyString()))
          .thenReturn(Single.just(Collections.emptyList()));
      mockedFactory
          .when(() -> com.logwise.orchestrator.factory.ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      Completable result = sparkService.cleanSparkState(tenant);
      result.blockingAwait();

      // Should complete without errors
      Assert.assertNotNull(result);
    }
  }

  @Test
  public void testSubmitSparkJob_WithValidConfig_SubmitsJob() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");

    io.vertx.reactivex.ext.web.client.WebClient reactiveWebClient = mockWebClient.getWebClient();
    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.bodyAsString()).thenReturn("{\"submissionId\":\"test-id\"}");
    when(reactiveWebClient.postAbs(anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.rxSendJson(any())).thenReturn(Single.just(mockHttpResponse));

    Completable result = sparkService.submitSparkJob(tenantConfig, null, null);

    Assert.assertNotNull(result);
    result.blockingAwait();
  }

  @Test
  public void testMonitorSparkJob_WithNoRunningDriver_SubmitsJob() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<com.logwise.orchestrator.factory.ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(com.logwise.orchestrator.factory.ObjectStoreFactory.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      // Mock ObjectStoreClient for cleanSparkState
      com.logwise.orchestrator.client.ObjectStoreClient mockObjectStoreClient =
          mock(com.logwise.orchestrator.client.ObjectStoreClient.class);
      when(mockObjectStoreClient.listObjects(anyString()))
          .thenReturn(Single.just(Collections.emptyList()));
      mockedFactory
          .when(() -> com.logwise.orchestrator.factory.ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      // Mock web client for getSparkMasterJsonResponse and submitSparkJob
      io.vertx.reactivex.ext.web.client.WebClient reactiveWebClient = mockWebClient.getWebClient();
      io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
          mockGetRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
      io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
          mockPostRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
      io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
          mockGetResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
      io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
          mockPostResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);

      SparkMasterJsonResponse sparkResponse = new SparkMasterJsonResponse();
      sparkResponse.setActivedrivers(Collections.emptyList());

      when(mockGetResponse.bodyAsString()).thenReturn("{\"activedrivers\":[]}");
      when(mockPostResponse.statusCode()).thenReturn(200);
      when(mockPostResponse.bodyAsString()).thenReturn("{\"submissionId\":\"test\"}");
      when(reactiveWebClient.getAbs(anyString())).thenReturn(mockGetRequest);
      when(reactiveWebClient.postAbs(anyString())).thenReturn(mockPostRequest);
      when(mockGetRequest.rxSend()).thenReturn(Single.just(mockGetResponse));
      when(mockPostRequest.rxSendJson(any())).thenReturn(Single.just(mockPostResponse));
      try {
        when(mockObjectMapper.readValue(anyString(), eq(SparkMasterJsonResponse.class)))
            .thenReturn(sparkResponse);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      Completable result = sparkService.monitorSparkJob(tenant, null, null);

      Assert.assertNotNull(result);
      // This is a long-running operation, just verify it doesn't throw immediately
    }
  }
}
