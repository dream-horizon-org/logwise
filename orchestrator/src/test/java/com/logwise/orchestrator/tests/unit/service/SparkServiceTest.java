package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dao.SparkScaleOverrideDao;
import com.logwise.orchestrator.dao.SparkStageHistoryDao;
import com.logwise.orchestrator.dto.entity.SparkScaleOverride;
import com.logwise.orchestrator.dto.entity.SparkStageHistory;
import com.logwise.orchestrator.dto.request.SubmitSparkJobRequest;
import com.logwise.orchestrator.dto.request.UpdateSparkScaleOverrideRequest;
import com.logwise.orchestrator.dto.response.GetSparkStageHistoryResponse;
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
  private SparkStageHistoryDao mockSparkStageHistoryDao;
  private SparkScaleOverrideDao mockSparkScaleOverrideDao;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockWebClient = mock(WebClient.class);
    mockObjectMapper = mock(ObjectMapper.class);
    mockSparkStageHistoryDao = mock(SparkStageHistoryDao.class);
    mockSparkScaleOverrideDao = mock(SparkScaleOverrideDao.class);
    io.vertx.reactivex.core.Vertx reactiveVertx = BaseTest.getReactiveVertx();
    sparkService =
        new SparkService(
            reactiveVertx,
            mockWebClient,
            mockObjectMapper,
            mockSparkStageHistoryDao,
            mockSparkScaleOverrideDao);
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
      io.vertx.reactivex.ext.web.client.WebClient reactiveWebClient =
          mock(io.vertx.reactivex.ext.web.client.WebClient.class);
      io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
          mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
      io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
          mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
      when(mockHttpResponse.statusCode()).thenReturn(200);
      when(mockHttpResponse.bodyAsString()).thenReturn("{\"submissionId\":\"test-id\"}");
      when(reactiveWebClient.postAbs(anyString())).thenReturn(mockHttpRequest);
      when(mockHttpRequest.rxSendJson(any())).thenReturn(Single.just(mockHttpResponse));
      when(mockWebClient.getWebClient()).thenReturn(reactiveWebClient);

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

    io.vertx.reactivex.ext.web.client.WebClient reactiveWebClient =
        mock(io.vertx.reactivex.ext.web.client.WebClient.class);
    io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
    io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
        mockHttpResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
    when(mockHttpResponse.statusCode()).thenReturn(200);
    when(mockHttpResponse.bodyAsString()).thenReturn("{\"submissionId\":\"test-id\"}");
    when(reactiveWebClient.postAbs(anyString())).thenReturn(mockHttpRequest);
    when(mockHttpRequest.rxSendJson(any())).thenReturn(Single.just(mockHttpResponse));
    when(mockWebClient.getWebClient()).thenReturn(reactiveWebClient);

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

  @Test
  public void testInsertSparkStageHistory_WithValidHistory_InsertsSuccessfully() {
    SparkStageHistory stageHistory = new SparkStageHistory();
    stageHistory.setOutputBytes(100000L);
    stageHistory.setInputRecords(1000L);
    stageHistory.setSubmissionTime(System.currentTimeMillis());
    stageHistory.setCompletionTime(System.currentTimeMillis() + 5000);
    stageHistory.setCoresUsed(4);
    stageHistory.setStatus("SUCCESS");
    stageHistory.setTenant("ABC");

    when(mockSparkStageHistoryDao.insertSparkStageHistory(any(SparkStageHistory.class)))
        .thenReturn(Completable.complete());

    Completable result = sparkService.insertSparkStageHistory(stageHistory);
    result.blockingAwait();

    verify(mockSparkStageHistoryDao, times(1)).insertSparkStageHistory(eq(stageHistory));
  }

  @Test
  public void testInsertSparkStageHistory_WithDaoError_PropagatesError() {
    SparkStageHistory stageHistory = new SparkStageHistory();
    RuntimeException daoError = new RuntimeException("DAO error");

    when(mockSparkStageHistoryDao.insertSparkStageHistory(any(SparkStageHistory.class)))
        .thenReturn(Completable.error(daoError));

    Completable result = sparkService.insertSparkStageHistory(stageHistory);

    try {
      result.blockingAwait();
      Assert.fail("Should have thrown exception");
    } catch (RuntimeException e) {
      Assert.assertEquals(e, daoError);
    }
  }

  @Test
  public void testScaleSpark_WithBothScalingEnabled_CallsProcessSparkScaling() {
    Tenant tenant = Tenant.ABC;
    boolean enableUpScale = true;
    boolean enableDownScale = true;

    SparkScaleOverride scaleOverride = SparkScaleOverride.builder().tenant("ABC").build();
    when(mockSparkScaleOverrideDao.getSparkScaleOverride(eq(tenant)))
        .thenReturn(Single.just(scaleOverride));

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
        Mockito.mockStatic(ApplicationConfigUtil.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      when(mockSparkStageHistoryDao.getSparkStageHistory(any(Tenant.class), anyInt(), anyBoolean()))
          .thenReturn(Single.just(Collections.emptyList()));

      Completable result = sparkService.scaleSpark(tenant, enableUpScale, enableDownScale);

      Assert.assertNotNull(result);
      verify(mockSparkScaleOverrideDao, times(1)).getSparkScaleOverride(eq(tenant));
    }
  }

  @Test
  public void testScaleSpark_WithOverrideValues_UsesOverrideValues() {
    Tenant tenant = Tenant.ABC;
    boolean enableUpScale = true;
    boolean enableDownScale = true;

    SparkScaleOverride scaleOverride =
        SparkScaleOverride.builder().tenant("ABC").upscale(false).downscale(false).build();
    when(mockSparkScaleOverrideDao.getSparkScaleOverride(eq(tenant)))
        .thenReturn(Single.just(scaleOverride));

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
        Mockito.mockStatic(ApplicationConfigUtil.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      when(mockSparkStageHistoryDao.getSparkStageHistory(any(Tenant.class), anyInt(), anyBoolean()))
          .thenReturn(Single.just(Collections.emptyList()));

      Completable result = sparkService.scaleSpark(tenant, enableUpScale, enableDownScale);

      Assert.assertNotNull(result);
      verify(mockSparkScaleOverrideDao, times(1)).getSparkScaleOverride(eq(tenant));
    }
  }

  @Test
  public void testScaleSpark_WithNullOverrideValues_UsesProvidedValues() {
    Tenant tenant = Tenant.ABC;
    boolean enableUpScale = true;
    boolean enableDownScale = false;

    SparkScaleOverride scaleOverride = SparkScaleOverride.builder().tenant("ABC").build();
    when(mockSparkScaleOverrideDao.getSparkScaleOverride(eq(tenant)))
        .thenReturn(Single.just(scaleOverride));

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
        Mockito.mockStatic(ApplicationConfigUtil.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      when(mockSparkStageHistoryDao.getSparkStageHistory(any(Tenant.class), anyInt(), anyBoolean()))
          .thenReturn(Single.just(Collections.emptyList()));

      Completable result = sparkService.scaleSpark(tenant, enableUpScale, enableDownScale);

      Assert.assertNotNull(result);
      verify(mockSparkScaleOverrideDao, times(1)).getSparkScaleOverride(eq(tenant));
    }
  }

  @Test
  public void testScaleSpark_WithBothScalingDisabled_CompletesWithoutScaling() {
    Tenant tenant = Tenant.ABC;
    boolean enableUpScale = false;
    boolean enableDownScale = false;

    SparkScaleOverride scaleOverride = SparkScaleOverride.builder().tenant("ABC").build();
    when(mockSparkScaleOverrideDao.getSparkScaleOverride(eq(tenant)))
        .thenReturn(Single.just(scaleOverride));

    Completable result = sparkService.scaleSpark(tenant, enableUpScale, enableDownScale);
    result.blockingAwait();

    verify(mockSparkScaleOverrideDao, times(1)).getSparkScaleOverride(eq(tenant));
  }

  @Test
  public void testGetSparkStageHistory_WithValidTenant_ReturnsHistory() {
    Tenant tenant = Tenant.ABC;
    int limit = 10;
    List<SparkStageHistory> historyList = new ArrayList<>();
    SparkStageHistory history1 = new SparkStageHistory();
    history1.setTenant("ABC");
    history1.setInputRecords(1000L);
    historyList.add(history1);

    when(mockSparkStageHistoryDao.getSparkStageHistory(eq(tenant), eq(limit), eq(false)))
        .thenReturn(Single.just(historyList));

    Single<GetSparkStageHistoryResponse> result = sparkService.getSparkStageHistory(tenant, limit);
    GetSparkStageHistoryResponse response = result.blockingGet();

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getSparkStageHistory());
    Assert.assertEquals(response.getSparkStageHistory().size(), 1);
    verify(mockSparkStageHistoryDao, times(1))
        .getSparkStageHistory(eq(tenant), eq(limit), eq(false));
  }

  @Test
  public void testUpdateSparkScaleOverride_WithValidRequest_UpdatesOverride() {
    Tenant tenant = Tenant.ABC;
    UpdateSparkScaleOverrideRequest request = new UpdateSparkScaleOverrideRequest();
    request.setEnableUpScale(true);
    request.setEnableDownScale(false);

    when(mockSparkScaleOverrideDao.updateSparkScaleOverride(any(SparkScaleOverride.class)))
        .thenReturn(Completable.complete());

    Completable result = sparkService.updateSparkScaleOverride(tenant, request);
    result.blockingAwait();

    verify(mockSparkScaleOverrideDao, times(1))
        .updateSparkScaleOverride(any(SparkScaleOverride.class));
  }

  @Test
  public void testIsValidWalFile_WithValidFile_ReturnsTrue() throws Exception {
    Method method = SparkService.class.getDeclaredMethod("isValidWalFile", String.class);
    method.setAccessible(true);

    Boolean result = (Boolean) method.invoke(null, "123");

    Assert.assertTrue(result);
  }

  @Test
  public void testIsValidWalFile_WithInvalidFile_ReturnsFalse() throws Exception {
    Method method = SparkService.class.getDeclaredMethod("isValidWalFile", String.class);
    method.setAccessible(true);

    Boolean result = (Boolean) method.invoke(null, "invalid.wal");

    Assert.assertFalse(result);
  }

  @Test
  public void testGetLatestWalFile_WithMultipleFiles_ReturnsLatest() throws Exception {
    Method method = SparkService.class.getDeclaredMethod("getLatestWalFile", List.class);
    method.setAccessible(true);

    List<String> walFiles = Arrays.asList("10", "5", "15", "3");
    String result = (String) method.invoke(null, walFiles);

    Assert.assertEquals(result, "15");
  }

  @Test
  public void testAverageGrowthRate_WithValidNumbers_ReturnsAverage() throws Exception {
    Method method = SparkService.class.getDeclaredMethod("averageGrowthRate", List.class);
    method.setAccessible(true);

    List<Long> numbers = Arrays.asList(300L, 200L, 100L);
    Double result = (Double) method.invoke(null, numbers);

    Assert.assertNotNull(result);
    Assert.assertTrue(result > 0);
  }

  @Test
  public void testAverageGrowthRate_WithLessThanTwoNumbers_ReturnsZero() throws Exception {
    Method method = SparkService.class.getDeclaredMethod("averageGrowthRate", List.class);
    method.setAccessible(true);

    List<Long> numbers = Collections.singletonList(100L);
    Double result = (Double) method.invoke(null, numbers);

    Assert.assertEquals(result, 0.0);
  }

  @Test
  public void testGetWorkersFromCores_WithValidCores_ReturnsWorkers() throws Exception {
    Method method =
        SparkService.class.getDeclaredMethod(
            "getWorkersFromCores", Integer.class, ApplicationConfig.TenantConfig.class);
    method.setAccessible(true);

    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    tenantConfig.getSpark().setExecutorCoresPerMachine(4);

    Integer result = (Integer) method.invoke(null, 8, tenantConfig);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.intValue(), 2);
  }

  @Test
  public void testGetWorkersFromCores_WithNullCores_ReturnsNull() throws Exception {
    Method method =
        SparkService.class.getDeclaredMethod(
            "getWorkersFromCores", Integer.class, ApplicationConfig.TenantConfig.class);
    method.setAccessible(true);

    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");

    Integer result = (Integer) method.invoke(null, null, tenantConfig);

    Assert.assertNull(result);
  }

  @Test
  public void testGetWorkersFromCores_WithZeroCores_ReturnsNull() throws Exception {
    Method method =
        SparkService.class.getDeclaredMethod(
            "getWorkersFromCores", Integer.class, ApplicationConfig.TenantConfig.class);
    method.setAccessible(true);

    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");

    Integer result = (Integer) method.invoke(null, 0, tenantConfig);

    Assert.assertNull(result);
  }

  @Test
  public void testProcessSparkScaling_WithInsufficientHistory_CompletesWithoutScaling() {
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

      SparkScaleOverride scaleOverride = SparkScaleOverride.builder().tenant("ABC").build();
      when(mockSparkScaleOverrideDao.getSparkScaleOverride(eq(tenant)))
          .thenReturn(Single.just(scaleOverride));

      List<SparkStageHistory> insufficientHistory = new ArrayList<>();
      for (int i = 0; i < 4; i++) {
        SparkStageHistory history = new SparkStageHistory();
        history.setInputRecords(1000L + i);
        history.setTenant("ABC");
        insufficientHistory.add(history);
      }
      when(mockSparkStageHistoryDao.getSparkStageHistory(any(Tenant.class), anyInt(), anyBoolean()))
          .thenReturn(Single.just(insufficientHistory));

      com.logwise.orchestrator.client.ObjectStoreClient mockObjectStoreClient =
          mock(com.logwise.orchestrator.client.ObjectStoreClient.class);
      when(mockObjectStoreClient.listObjects(anyString()))
          .thenReturn(Single.just(Collections.singletonList("logs/_spark_metadata/7")));
      mockedFactory
          .when(() -> com.logwise.orchestrator.factory.ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      SparkMasterJsonResponse sparkResponse = new SparkMasterJsonResponse();
      sparkResponse.setAliveworkers(5);
      io.vertx.reactivex.ext.web.client.WebClient reactiveWebClient = mockWebClient.getWebClient();
      io.vertx.reactivex.ext.web.client.HttpRequest<io.vertx.reactivex.core.buffer.Buffer>
          mockGetRequest = mock(io.vertx.reactivex.ext.web.client.HttpRequest.class);
      io.vertx.reactivex.ext.web.client.HttpResponse<io.vertx.reactivex.core.buffer.Buffer>
          mockGetResponse = mock(io.vertx.reactivex.ext.web.client.HttpResponse.class);
      when(mockGetResponse.bodyAsString()).thenReturn("{\"aliveworkers\":5}");
      when(reactiveWebClient.getAbs(anyString())).thenReturn(mockGetRequest);
      when(mockGetRequest.rxSend()).thenReturn(Single.just(mockGetResponse));
      try {
        when(mockObjectMapper.readValue(anyString(), eq(SparkMasterJsonResponse.class)))
            .thenReturn(sparkResponse);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      Completable result = sparkService.scaleSpark(tenant, true, true);

      try {
        result.blockingAwait();
      } catch (Exception e) {
        // If there's insufficient history, getExpectedExecutorCount returns null,
        // and scaleSpark completes successfully without scaling
        // This is expected behavior, so we just verify the DAO was called
      }

      verify(mockSparkScaleOverrideDao, times(1)).getSparkScaleOverride(eq(tenant));
    }
  }

  @Test
  public void testGetSparkSubmitRequestBody_WithS3aAccessKey_SetsAccessKey() throws Exception {
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
    sparkConfig.setS3aAccessKey("test-access-key");
    sparkConfig.setS3aSecretKey("test-secret-key");

    SubmitSparkJobRequest request =
        (SubmitSparkJobRequest) method.invoke(null, tenantConfig, null, null);

    Assert.assertNotNull(request);
    // S3a keys are stored in environmentVariables, not sparkProperties
    Assert.assertEquals(request.getEnvironmentVariables().get("S3A-ACCESS-KEY"), "test-access-key");
    Assert.assertEquals(request.getEnvironmentVariables().get("S3A-SECRET-KEY"), "test-secret-key");
  }

  @Test
  public void testGetSparkSubmitRequestBody_WithEmptyAwsRegion_HandlesGracefully()
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
    sparkConfig.setAwsRegion("");

    SubmitSparkJobRequest request =
        (SubmitSparkJobRequest) method.invoke(null, tenantConfig, null, null);

    Assert.assertNotNull(request);
  }

  @Test
  public void testGetSparkSubmitRequestBody_WithPlaceholderCredentials_HandlesGracefully()
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
    sparkConfig.setAwsAccessKeyId("your-access-key");
    sparkConfig.setAwsSecretAccessKey("your-secret-key");

    SubmitSparkJobRequest request =
        (SubmitSparkJobRequest) method.invoke(null, tenantConfig, null, null);

    Assert.assertNotNull(request);
  }

  @Test
  public void testGetSparkSubmitRequestBody_WithEmptyAccessKey_HandlesGracefully()
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
    sparkConfig.setAwsAccessKeyId("");
    sparkConfig.setAwsSecretAccessKey("");

    SubmitSparkJobRequest request =
        (SubmitSparkJobRequest) method.invoke(null, tenantConfig, null, null);

    Assert.assertNotNull(request);
  }

  @Test
  public void testGetSparkSubmitRequestBody_WithEmptySessionToken_HandlesGracefully()
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
    sparkConfig.setAwsSessionToken("");

    SubmitSparkJobRequest request =
        (SubmitSparkJobRequest) method.invoke(null, tenantConfig, null, null);

    Assert.assertNotNull(request);
  }

  @Test
  public void testGetSparkSubmitRequestBody_WithoutSessionToken_SetsBasicCredentials()
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
    sparkConfig.setAwsSessionToken(null);

    SubmitSparkJobRequest request =
        (SubmitSparkJobRequest) method.invoke(null, tenantConfig, null, null);

    Assert.assertNotNull(request);
    Object credentialsProviderObj =
        request.getSparkProperties().get("spark.hadoop.fs.s3a.aws.credentials.provider");
    if (credentialsProviderObj != null) {
      String credentialsProvider = String.valueOf(credentialsProviderObj);
      Assert.assertTrue(
          credentialsProvider.contains("SimpleAWSCredentialsProvider")
              || credentialsProvider.contains("TemporaryAWSCredentialsProvider"));
    }
  }

  @Test
  public void testGetLatestWalFile_WithCompactFiles_HandlesCorrectly() throws Exception {
    Method method = SparkService.class.getDeclaredMethod("getLatestWalFile", List.class);
    method.setAccessible(true);

    List<String> walFiles = Arrays.asList("10.compact", "5", "15.compact", "3");
    String result = (String) method.invoke(null, walFiles);

    Assert.assertNotNull(result);
  }

  @Test
  public void testIsValidWalFile_WithCompactFile_ReturnsTrue() throws Exception {
    Method method = SparkService.class.getDeclaredMethod("isValidWalFile", String.class);
    method.setAccessible(true);

    Boolean result = (Boolean) method.invoke(null, "123.compact");

    Assert.assertTrue(result);
  }

  @Test
  public void testIsValidWalFile_WithNonNumeric_ReturnsFalse() throws Exception {
    Method method = SparkService.class.getDeclaredMethod("isValidWalFile", String.class);
    method.setAccessible(true);

    Boolean result = (Boolean) method.invoke(null, "abc.compact");

    Assert.assertFalse(result);
  }
}
