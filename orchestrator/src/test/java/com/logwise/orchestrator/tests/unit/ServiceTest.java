package com.logwise.orchestrator.tests.unit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.logwise.orchestrator.CaffeineCacheFactory;
import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.constants.TestConstants;
import com.logwise.orchestrator.dao.ServicesDao;
import com.logwise.orchestrator.dto.entity.ServiceDetails;
import com.logwise.orchestrator.dto.request.SubmitSparkJobRequest;
import com.logwise.orchestrator.dto.response.GetServiceDetailsResponse;
import com.logwise.orchestrator.dto.response.LogSyncDelayResponse;
import com.logwise.orchestrator.dto.response.SparkMasterJsonResponse;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.ObjectStoreFactory;
import com.logwise.orchestrator.service.MetricsService;
import com.logwise.orchestrator.service.ObjectStoreService;
import com.logwise.orchestrator.service.ServiceManagerService;
import com.logwise.orchestrator.service.SparkService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import com.logwise.orchestrator.util.ApplicationUtils;
import com.logwise.orchestrator.webclient.reactivex.client.WebClient;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for service package (ObjectStoreService, SparkService, ServiceManagerService,
 * MetricsService).
 */
public class ServiceTest extends BaseTest {

  private ObjectStoreService objectStoreService;
  private ServicesDao mockServicesDao;
  private ObjectStoreClient mockObjectStoreClient;
  @Mock private ApplicationConfig.TenantConfig mockTenantConfig;
  private ApplicationConfig.SparkConfig mockSparkConfig;

  private SparkService sparkService;
  private WebClient mockWebClient;
  private ObjectMapper mockObjectMapper;
  private io.vertx.reactivex.ext.web.client.WebClient mockVertxWebClient;
  private ApplicationConfig.KafkaConfig mockKafkaConfig;

  @Mock private ServicesDao mockServicesDaoForManager;
  @Mock private ObjectStoreService mockObjectStoreServiceForManager;
  @Mock private AsyncLoadingCache<Tenant, GetServiceDetailsResponse> mockCache;
  private ServiceManagerService serviceManagerService;
  private Vertx vertx;

  private MetricsService metricsService;
  private ApplicationConfig.DelayMetricsConfig mockDelayMetricsConfig;
  private ApplicationConfig.ApplicationDelayMetricsConfig mockAppDelayMetricsConfig;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);

    mockServicesDao = mock(ServicesDao.class);
    objectStoreService = new ObjectStoreService();
    Field servicesDaoField = ObjectStoreService.class.getDeclaredField("servicesDao");
    servicesDaoField.setAccessible(true);
    servicesDaoField.set(objectStoreService, mockServicesDao);

    mockObjectStoreClient = mock(ObjectStoreClient.class);

    mockSparkConfig = mock(ApplicationConfig.SparkConfig.class);
    when(mockTenantConfig.getName()).thenReturn("test-tenant");
    when(mockTenantConfig.getSpark()).thenReturn(mockSparkConfig);
    when(mockSparkConfig.getLogsDir())
        .thenReturn("logs"); // Required for getAllDistinctServicesInAws
    when(mockTenantConfig.getObjectStore())
        .thenReturn(ApplicationTestConfig.createMockObjectStoreConfig());

    mockWebClient = mock(WebClient.class);
    mockObjectMapper = new ObjectMapper();
    mockVertxWebClient = mock(io.vertx.reactivex.ext.web.client.WebClient.class);
    mockKafkaConfig = mock(ApplicationConfig.KafkaConfig.class);
    sparkService = new SparkService(mockWebClient, mockObjectMapper);
    doReturn(mockVertxWebClient).when(mockWebClient).getWebClient();
    when(mockTenantConfig.getKafka()).thenReturn(mockKafkaConfig);

    if (BaseTest.vertx != null) {
      vertx = new io.vertx.reactivex.core.Vertx(BaseTest.vertx);
    } else {
      vertx = new io.vertx.reactivex.core.Vertx(io.vertx.core.Vertx.vertx());
    }

    try (MockedStatic<CaffeineCacheFactory> mockedFactory =
        Mockito.mockStatic(CaffeineCacheFactory.class)) {
      mockedFactory
          .when(
              () ->
                  CaffeineCacheFactory.createAsyncLoadingCache(
                      any(Vertx.class), anyString(), any(), anyString()))
          .thenReturn(mockCache);
      serviceManagerService =
          new ServiceManagerService(
              vertx, mockServicesDaoForManager, mockObjectStoreServiceForManager);
    }

    metricsService = new MetricsService();
    mockDelayMetricsConfig = mock(ApplicationConfig.DelayMetricsConfig.class);
    mockAppDelayMetricsConfig = mock(ApplicationConfig.ApplicationDelayMetricsConfig.class);
    when(mockTenantConfig.getDelayMetrics()).thenReturn(mockDelayMetricsConfig);
    when(mockDelayMetricsConfig.getApp()).thenReturn(mockAppDelayMetricsConfig);
    when(mockAppDelayMetricsConfig.getSampleEnv()).thenReturn("local");
    when(mockAppDelayMetricsConfig.getSampleComponentType()).thenReturn("application");
    when(mockAppDelayMetricsConfig.getSampleServiceName()).thenReturn("test-service");
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void
      testObjectStoreService_GetAllDistinctServicesInAws_WithValidServices_ReturnsServiceDetailsList()
          throws Exception {

    Tenant tenant = TestConstants.VALID_TENANT;
    List<String> servicePrefixes =
        Arrays.asList("logs/service_name=service1/", "logs/service_name=service2/");

    ServiceDetails service1 = ServiceDetails.builder().serviceName("service1").build();
    ServiceDetails service2 = ServiceDetails.builder().serviceName("service2").build();

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {

      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      lenient()
          .when(mockObjectStoreClient.listCommonPrefix("logs/service_name=", "/"))
          .thenReturn(Single.just(servicePrefixes));

      mockedUtils
          .when(() -> ApplicationUtils.getServiceFromObjectKey(servicePrefixes.get(0)))
          .thenReturn(service1);
      mockedUtils
          .when(() -> ApplicationUtils.getServiceFromObjectKey(servicePrefixes.get(1)))
          .thenReturn(service2);

      when(mockTenantConfig.getDefaultLogsRetentionDays()).thenReturn(30);

      Single<List<ServiceDetails>> result = objectStoreService.getAllDistinctServicesInAws(tenant);
      List<ServiceDetails> services = result.blockingGet();

      Assert.assertNotNull(services);
      Assert.assertEquals(services.size(), 2);
      Assert.assertEquals(services.get(0).getServiceName(), "service1");
      Assert.assertEquals(services.get(0).getRetentionDays(), Integer.valueOf(30));
      Assert.assertEquals(services.get(0).getTenant(), tenant.getValue());
    }
  }

  @Test
  public void testObjectStoreService_GetAllDistinctServicesInAws_WithNoServices_ReturnsEmptyList()
      throws Exception {

    Tenant tenant = TestConstants.VALID_TENANT;

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class)) {

      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      lenient()
          .when(mockObjectStoreClient.listCommonPrefix("logs/service_name=", "/"))
          .thenReturn(Single.just(Collections.emptyList()));

      Single<List<ServiceDetails>> result = objectStoreService.getAllDistinctServicesInAws(tenant);
      List<ServiceDetails> services = result.blockingGet();

      Assert.assertNotNull(services);
      Assert.assertTrue(services.isEmpty());
    }
  }

  @Test
  public void testObjectStoreService_GetAllDistinctServicesInAws_WhenClientFails_PropagatesError() {

    Tenant tenant = TestConstants.VALID_TENANT;
    RuntimeException error = new RuntimeException("S3 client error");

    try (MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class)) {

      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      lenient()
          .when(mockObjectStoreClient.listCommonPrefix("logs/service_name=", "/"))
          .thenReturn(Single.error(error));

      Single<List<ServiceDetails>> result = objectStoreService.getAllDistinctServicesInAws(tenant);

      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (RuntimeException e) {
        Assert.assertNotNull(e);
      }
    }
  }

  @Test
  public void testSparkService_GetSparkSubmitRequestBody_WithValidConfig_ReturnsRequest() {

    when(mockSparkConfig.getSparkJarPath()).thenReturn("s3://bucket/app.jar");
    when(mockSparkConfig.getClientSparkVersion()).thenReturn("3.2.0");
    when(mockSparkConfig.getMainClass()).thenReturn("com.example.Main");
    when(mockSparkConfig.getDriverCores()).thenReturn("2");
    when(mockSparkConfig.getDriverMemory()).thenReturn("4G");
    when(mockSparkConfig.getDriverCoresMax()).thenReturn("10");
    when(mockSparkConfig.getDriverMaxResultSize()).thenReturn("2g");
    when(mockSparkConfig.getExecutorCores()).thenReturn("2");
    when(mockSparkConfig.getExecutorMemory()).thenReturn("4G");
    when(mockSparkConfig.getLog4jPropertiesFilePath()).thenReturn("log4j.properties");
    when(mockSparkConfig.getAppName()).thenReturn("test-app");
    when(mockSparkConfig.getKafkaMaxRatePerPartition()).thenReturn("1000");
    when(mockSparkConfig.getKafkaStartingOffsets()).thenReturn("latest");
    when(mockSparkConfig.getSubscribePattern()).thenReturn("topic.*");
    when(mockSparkConfig.getSparkMasterHost()).thenReturn("spark-master");
    when(mockKafkaConfig.getKafkaBrokersHost()).thenReturn("kafka-broker:9092");
    when(mockSparkConfig.getS3aAccessKey()).thenReturn("access-key");
    when(mockSparkConfig.getS3aSecretKey()).thenReturn("secret-key");

    try {
      Method method =
          SparkService.class.getDeclaredMethod(
              "getSparkSubmitRequestBody",
              ApplicationConfig.TenantConfig.class,
              Integer.class,
              Integer.class);
      method.setAccessible(true);
      SubmitSparkJobRequest request =
          (SubmitSparkJobRequest) method.invoke(null, mockTenantConfig, 4, 8);

      Assert.assertNotNull(request);
      Assert.assertEquals(request.getAction(), "CreateSubmissionRequest");
      Assert.assertNotNull(request.getAppArgs());
      Assert.assertTrue(request.getAppArgs().size() > 0);
      Assert.assertEquals(request.getAppResource(), "s3://bucket/app.jar");
      Assert.assertEquals(request.getClientSparkVersion(), "3.2.0");
      Assert.assertEquals(request.getMainClass(), "com.example.Main");
      Assert.assertNotNull(request.getEnvironmentVariables());
      Assert.assertNotNull(request.getSparkProperties());
      Assert.assertEquals(request.getSparkProperties().get("spark.driver.cores"), "4");
      Assert.assertEquals(request.getSparkProperties().get("spark.driver.memory"), "8G");
    } catch (Exception e) {
      Assert.fail("Should not throw exception", e);
    }
  }

  @Test
  public void testSparkService_IsDriverNotRunning_WithEmptyDrivers_ReturnsTrue() {

    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    response.setActivedrivers(new ArrayList<>());

    try {
      Method method =
          SparkService.class.getDeclaredMethod("isDriverNotRunning", SparkMasterJsonResponse.class);
      method.setAccessible(true);
      boolean result = (Boolean) method.invoke(sparkService, response);

      Assert.assertTrue(result);
    } catch (Exception e) {
      Assert.fail("Should not throw exception", e);
    }
  }

  @Test
  public void testSparkService_IsDriverNotRunning_WithRunningDriver_ReturnsFalse() {

    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    SparkMasterJsonResponse.Driver driver = new SparkMasterJsonResponse.Driver();
    driver.setState("RUNNING");
    response.setActivedrivers(Arrays.asList(driver));

    try {
      Method method =
          SparkService.class.getDeclaredMethod("isDriverNotRunning", SparkMasterJsonResponse.class);
      method.setAccessible(true);
      boolean result = (Boolean) method.invoke(sparkService, response);

      Assert.assertFalse(result);
    } catch (Exception e) {
      Assert.fail("Should not throw exception", e);
    }
  }

  @Test
  public void testSparkService_CleanSparkState_WithValidTenant_DeletesFiles() {

    Tenant tenant = TestConstants.VALID_TENANT;
    ObjectStoreClient mockObjectStoreClientForSpark = mock(ObjectStoreClient.class);
    List<String> checkpointFiles = Arrays.asList("checkpoint/file1", "checkpoint/file2");
    List<String> metadataFiles = Arrays.asList("metadata/file1");

    when(mockSparkConfig.getCheckPointDir()).thenReturn("checkpoint");
    when(mockSparkConfig.getLogsDir()).thenReturn("logs");

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class)) {
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);
      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClientForSpark);

      when(mockObjectStoreClientForSpark.listObjects("checkpoint/"))
          .thenReturn(Single.just(checkpointFiles));
      when(mockObjectStoreClientForSpark.listObjects("logs/_spark_metadata/"))
          .thenReturn(Single.just(metadataFiles));
      when(mockObjectStoreClientForSpark.deleteFile(anyString()))
          .thenReturn(Completable.complete());

      Completable result = sparkService.cleanSparkState(tenant);
      result.blockingAwait();

      verify(mockObjectStoreClientForSpark, times(3)).deleteFile(anyString());
    }
  }

  @Test
  public void
      testServiceManagerService_GetServiceDetailsFromCache_WithValidTenant_ReturnsCachedResponse() {

    Tenant tenant = TestConstants.VALID_TENANT;
    GetServiceDetailsResponse cachedResponse =
        GetServiceDetailsResponse.builder().serviceDetails(Collections.emptyList()).build();

    CompletableFuture<GetServiceDetailsResponse> future =
        CompletableFuture.completedFuture(cachedResponse);
    when(mockCache.get(tenant)).thenReturn(future);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(
              invocation -> {
                CompletableFuture<GetServiceDetailsResponse> cf = invocation.getArgument(0);
                return Single.fromFuture(cf);
              });

      Single<GetServiceDetailsResponse> result =
          serviceManagerService.getServiceDetailsFromCache(tenant);
      GetServiceDetailsResponse response = result.blockingGet();

      Assert.assertNotNull(response);
      Assert.assertEquals(response, cachedResponse);
    }
  }

  @Test
  public void
      testServiceManagerService_GetServiceDetailsFromDB_WithValidTenant_ReturnsServiceDetails() {

    Tenant tenant = TestConstants.VALID_TENANT;
    List<ServiceDetails> serviceDetailsList = new ArrayList<>();
    serviceDetailsList.add(ServiceDetails.builder().serviceName("test-service").build());

    when(mockServicesDaoForManager.getAllServiceDetails(tenant))
        .thenReturn(Single.just(serviceDetailsList));

    Single<GetServiceDetailsResponse> result =
        serviceManagerService.getServiceDetailsFromDB(tenant);
    GetServiceDetailsResponse response = result.blockingGet();

    Assert.assertNotNull(response);
    Assert.assertNotNull(response.getServiceDetails());
    Assert.assertEquals(response.getServiceDetails().size(), 1);
    Assert.assertEquals(response.getServiceDetails().get(0).getServiceName(), "test-service");
  }

  @Test
  public void testServiceManagerService_SyncServices_WithServicesNotInDb_OnBoardsNewServices() {

    Tenant tenant = TestConstants.VALID_TENANT;

    List<ServiceDetails> dbServices = new ArrayList<>();

    List<ServiceDetails> objectStoreServices = new ArrayList<>();
    ServiceDetails newService = ServiceDetails.builder().serviceName("new-service").build();
    objectStoreServices.add(newService);

    when(mockServicesDaoForManager.getAllServiceDetails(tenant))
        .thenReturn(Single.just(dbServices));
    when(mockObjectStoreServiceForManager.getAllDistinctServicesInAws(tenant))
        .thenReturn(Single.just(objectStoreServices));
    when(mockServicesDaoForManager.insertServiceDetails(anyList()))
        .thenReturn(Completable.complete());

    Completable result = serviceManagerService.syncServices(tenant);
    result.blockingAwait();

    verify(mockServicesDaoForManager, times(1)).insertServiceDetails(anyList());
    verify(mockServicesDaoForManager, never()).deleteServiceDetails(anyList());
  }

  @Test
  public void testServiceManagerService_SyncServices_WithNoChanges_CompletesWithoutModifications() {

    Tenant tenant = TestConstants.VALID_TENANT;

    ServiceDetails service = ServiceDetails.builder().serviceName("test-service").build();

    List<ServiceDetails> services = Collections.singletonList(service);

    when(mockServicesDaoForManager.getAllServiceDetails(tenant)).thenReturn(Single.just(services));
    when(mockObjectStoreServiceForManager.getAllDistinctServicesInAws(tenant))
        .thenReturn(Single.just(services));

    Completable result = serviceManagerService.syncServices(tenant);
    result.blockingAwait();

    verify(mockServicesDaoForManager, never()).insertServiceDetails(anyList());
    verify(mockServicesDaoForManager, never()).deleteServiceDetails(anyList());
  }

  @Test
  public void testMetricsService_ComputeLogSyncDelay_WithValidTenant_ReturnsLogSyncDelayResponse() {

    Tenant tenant = TestConstants.VALID_TENANT;
    List<String> objectNames = new ArrayList<>();
    objectNames.add(
        "logs/service_name=test-service/year=2024/month=01/day=01/hour=10/minute=30/file.log");

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {

      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      Single<List<String>> listObjectsSingle = Single.just(objectNames);
      when(mockObjectStoreClient.listObjects(anyString())).thenReturn(listObjectsSingle);

      Maybe<Integer> maybeDelay = Maybe.just(30);
      mockedUtils
          .when(() -> ApplicationUtils.executeBlockingCallable(any()))
          .thenReturn(maybeDelay);

      Single<LogSyncDelayResponse> result = metricsService.computeLogSyncDelay(tenant);
      LogSyncDelayResponse response = result.blockingGet();

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getTenant(), tenant.getValue());
      Assert.assertNotNull(response.getAppLogsDelayMinutes());
      Assert.assertTrue(response.getAppLogsDelayMinutes() >= 1);
    }
  }

  @Test
  public void testMetricsService_ComputeLogSyncDelay_WithNoObjectsFound_ReturnsMaxDelay() {

    Tenant tenant = TestConstants.VALID_TENANT;
    List<String> emptyList = Collections.emptyList();

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
            Mockito.mockStatic(ApplicationConfigUtil.class);
        MockedStatic<ObjectStoreFactory> mockedFactory =
            Mockito.mockStatic(ObjectStoreFactory.class);
        MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {

      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      mockedFactory
          .when(() -> ObjectStoreFactory.getClient(tenant))
          .thenReturn(mockObjectStoreClient);

      Single<List<String>> listObjectsSingle = Single.just(emptyList);
      when(mockObjectStoreClient.listObjects(anyString())).thenReturn(listObjectsSingle);

      Maybe<Integer> maybeDelay = Maybe.just(ApplicationConstants.MAX_LOGS_SYNC_DELAY_HOURS * 60);
      mockedUtils
          .when(() -> ApplicationUtils.executeBlockingCallable(any()))
          .thenReturn(maybeDelay);

      Single<LogSyncDelayResponse> result = metricsService.computeLogSyncDelay(tenant);
      LogSyncDelayResponse response = result.blockingGet();

      Assert.assertNotNull(response);
      Assert.assertEquals(response.getTenant(), tenant.getValue());
      Assert.assertEquals(
          response.getAppLogsDelayMinutes(),
          Integer.valueOf(ApplicationConstants.MAX_LOGS_SYNC_DELAY_HOURS * 60));
    }
  }
}
