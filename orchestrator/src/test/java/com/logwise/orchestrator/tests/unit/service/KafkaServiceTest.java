package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.github.benmanes.caffeine.cache.Cache;
import com.logwise.orchestrator.CaffeineCacheFactory;
import com.logwise.orchestrator.client.kafka.KafkaClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.kafka.TopicOffsetInfo;
import com.logwise.orchestrator.enums.KafkaType;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.KafkaClientFactory;
import com.logwise.orchestrator.service.KafkaService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import java.util.*;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class KafkaServiceTest extends BaseTest {

  private KafkaClientFactory mockKafkaClientFactory;
  private KafkaClient mockKafkaClient;
  private ApplicationConfig.TenantConfig mockTenantConfig;
  private ApplicationConfig.KafkaConfig mockKafkaConfig;
  private ApplicationConfig.SparkConfig mockSparkConfig;
  @SuppressWarnings("unchecked")
  private Cache<String, Single<?>> mockCache;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    
    mockKafkaClientFactory = mock(KafkaClientFactory.class);
    mockKafkaClient = mock(KafkaClient.class);
    mockTenantConfig = mock(ApplicationConfig.TenantConfig.class);
    mockKafkaConfig = mock(ApplicationConfig.KafkaConfig.class);
    mockSparkConfig = mock(ApplicationConfig.SparkConfig.class);
    @SuppressWarnings("unchecked")
    Cache<String, Single<?>> cacheMock = mock(Cache.class);
    mockCache = cacheMock;

    when(mockTenantConfig.getKafka()).thenReturn(mockKafkaConfig);
    when(mockTenantConfig.getSpark()).thenReturn(mockSparkConfig);
    when(mockSparkConfig.getSubscribePattern()).thenReturn("logs.*");
    when(mockKafkaConfig.getKafkaType()).thenReturn(KafkaType.MSK);
    when(mockKafkaClientFactory.createKafkaClient(any(ApplicationConfig.KafkaConfig.class)))
        .thenReturn(mockKafkaClient);
  }

  /**
   * Creates a KafkaService instance with mocked cache factory.
   * The MockedStatic must remain open during the test execution.
   */
  private KafkaService createKafkaServiceWithMockedCache(MockedStatic<CaffeineCacheFactory> mockedFactory) {
    mockedFactory
        .when(() -> CaffeineCacheFactory.createCache(any(Vertx.class), anyString()))
        .thenReturn(mockCache);
    return new KafkaService(BaseTest.getReactiveVertx(), mockKafkaClientFactory);
  }

  @Test
  public void testScaleKafkaPartitions_WithScalingDisabled_ReturnsEmptyList() throws Exception {
    Tenant tenant = Tenant.ABC;
    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(false);

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);

      Map<String, Integer> scalingMap = result.blockingGet();
      Assert.assertNotNull(scalingMap);
      Assert.assertTrue(scalingMap.isEmpty());
      verify(mockKafkaClientFactory, never()).createKafkaClient(any());
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithNullScalingFlag_ReturnsEmptyList() throws Exception {
    Tenant tenant = Tenant.ABC;
    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(null);

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);

      Map<String, Integer> scalingMap = result.blockingGet();
      Assert.assertNotNull(scalingMap);
      Assert.assertTrue(scalingMap.isEmpty());
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithNoTopics_ReturnsEmptyList() throws Exception {
    Tenant tenant = Tenant.ABC;
    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(Collections.emptySet()));

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);

      Map<String, Integer> scalingMap = result.blockingGet();
      Assert.assertNotNull(scalingMap);
      Assert.assertTrue(scalingMap.isEmpty());
      verify(mockKafkaClient, times(1)).listTopics(anyString());
      verify(mockKafkaClient, times(1)).close();
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithTopicsButNoScalingNeeded_ReturnsEmptyList()
      throws Exception {
    Tenant tenant = Tenant.ABC;
    Set<String> topics = new HashSet<>(Arrays.asList("logs.service1", "logs.service2"));
    Map<String, TopicOffsetInfo> offsetsSumMap = new HashMap<>();
    // Create TopicOffsetInfo with values that won't trigger scaling
    // Small offset sum and sufficient partitions to ensure requiredPartitions <= currentPartitions
    TopicOffsetInfo offsetInfo1 =
        TopicOffsetInfo.builder().sumOfEndOffsets(1000L).currentNumberOfPartitions(10).build();
    TopicOffsetInfo offsetInfo2 =
        TopicOffsetInfo.builder().sumOfEndOffsets(2000L).currentNumberOfPartitions(10).build();
    offsetsSumMap.put("logs.service1", offsetInfo1);
    offsetsSumMap.put("logs.service2", offsetInfo2);

    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaConfig.getPartitionRatePerSecond()).thenReturn(1000L);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getEndOffsetSum(anyList())).thenReturn(Single.just(offsetsSumMap));
    // Mock cache to return null (no cached data) - first time check
    when(mockCache.getIfPresent(anyString())).thenReturn(null);

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);

      Map<String, Integer> scalingMap = result.blockingGet();
      Assert.assertNotNull(scalingMap);
      Assert.assertTrue(scalingMap.isEmpty());
      verify(mockKafkaClient, times(1)).close();
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithCachedDataButNoScalingNeeded_ReturnsEmptyList()
      throws Exception {
    Tenant tenant = Tenant.ABC;
    Set<String> topics = new HashSet<>(Arrays.asList("logs.service1"));
    Map<String, TopicOffsetInfo> offsetsSumMap = new HashMap<>();
    
    // Current offset: 110000, last cached offset: 100000, time difference: 60 seconds
    // ingestionRate = (110000 - 100000) / 60 = 166.67/sec
    // requiredPartitions = ceil(166.67 / 1000) = 1
    // currentPartitions = 10, so requiredPartitions (1) <= currentPartitions (10) -> no scaling
    TopicOffsetInfo offsetInfo =
        TopicOffsetInfo.builder().sumOfEndOffsets(110000L).currentNumberOfPartitions(10).build();
    offsetsSumMap.put("logs.service1", offsetInfo);

    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaConfig.getPartitionRatePerSecond()).thenReturn(1000L);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getEndOffsetSum(anyList())).thenReturn(Single.just(offsetsSumMap));

    // Create cached data - simulate previous call with offset 100000, 60 seconds ago
    // Use relative timestamp to ensure valid time difference (must be > 0 and <= 300 seconds)
    long cachedTimestamp = System.currentTimeMillis() - 60000; // 60 seconds ago
    Object offsetWithTimestamp = createOffsetWithTimestamp(100000L, cachedTimestamp);
    @SuppressWarnings({"unchecked", "rawtypes"})
    Single cachedValue = Single.just(offsetWithTimestamp);
    
    // Mock cache to return cached data - this will trigger the calculation path (lines 218-238)
    when(mockCache.getIfPresent(eq("logs.service1"))).thenReturn(cachedValue);

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);

      Map<String, Integer> scalingMap = result.blockingGet();
      Assert.assertNotNull(scalingMap);
      Assert.assertTrue(scalingMap.isEmpty(), "Should return empty when requiredPartitions <= currentPartitions");
      
      // Verify that cache was updated with new offset
      verify(mockCache, times(1)).put(eq("logs.service1"), any(Single.class));
      verify(mockKafkaClient, times(1)).close();
      // Verify increasePartitions was NOT called since no scaling is needed
      verify(mockKafkaClient, never()).increasePartitions(anyMap());
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithInvalidTimeDifference_ReturnsEmptyList()
      throws Exception {
    Tenant tenant = Tenant.ABC;
    Set<String> topics = new HashSet<>(Arrays.asList("logs.service1"));
    Map<String, TopicOffsetInfo> offsetsSumMap = new HashMap<>();
    TopicOffsetInfo offsetInfo =
        TopicOffsetInfo.builder().sumOfEndOffsets(110000L).currentNumberOfPartitions(10).build();
    offsetsSumMap.put("logs.service1", offsetInfo);

    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaConfig.getPartitionRatePerSecond()).thenReturn(1000L);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getEndOffsetSum(anyList())).thenReturn(Single.just(offsetsSumMap));

    // Create cached data with timestamp > 300 seconds ago (invalid)
    // Use relative timestamp to ensure time difference > 300 seconds
    long cachedTimestamp = System.currentTimeMillis() - 301000; // 301 seconds ago
    Object offsetWithTimestamp = createOffsetWithTimestamp(100000L, cachedTimestamp);
    @SuppressWarnings({"unchecked", "rawtypes"})
    Single cachedValue = Single.just(offsetWithTimestamp);
    
    // Mock cache to return cached data with invalid time difference
    when(mockCache.getIfPresent(eq("logs.service1"))).thenReturn(cachedValue);

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);

      Map<String, Integer> scalingMap = result.blockingGet();
      Assert.assertNotNull(scalingMap);
      Assert.assertTrue(scalingMap.isEmpty(), "Should return empty when time difference > 300 seconds");
      
      // Verify that cache was updated with new offset (line 214)
      verify(mockCache, times(1)).put(eq("logs.service1"), any(Single.class));
      verify(mockKafkaClient, times(1)).close();
      verify(mockKafkaClient, never()).increasePartitions(anyMap());
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithZeroOrNegativeTimeDifference_ReturnsEmptyList()
      throws Exception {
    Tenant tenant = Tenant.ABC;
    Set<String> topics = new HashSet<>(Arrays.asList("logs.service1"));
    Map<String, TopicOffsetInfo> offsetsSumMap = new HashMap<>();
    TopicOffsetInfo offsetInfo =
        TopicOffsetInfo.builder().sumOfEndOffsets(110000L).currentNumberOfPartitions(10).build();
    offsetsSumMap.put("logs.service1", offsetInfo);

    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaConfig.getPartitionRatePerSecond()).thenReturn(1000L);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getEndOffsetSum(anyList())).thenReturn(Single.just(offsetsSumMap));

    // Create cached data with future timestamp (negative time difference)
    // Use relative timestamp - future timestamp will always be > current time
    long cachedTimestamp = System.currentTimeMillis() + 1000; // 1 second in the future
    Object offsetWithTimestamp = createOffsetWithTimestamp(100000L, cachedTimestamp);
    @SuppressWarnings({"unchecked", "rawtypes"})
    Single cachedValue = Single.just(offsetWithTimestamp);
    
    // Mock cache to return cached data with invalid time difference
    when(mockCache.getIfPresent(eq("logs.service1"))).thenReturn(cachedValue);

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);

      Map<String, Integer> scalingMap = result.blockingGet();
      Assert.assertNotNull(scalingMap);
      Assert.assertTrue(scalingMap.isEmpty(), "Should return empty when time difference <= 0");
      
      // Verify that cache was updated with new offset (line 214)
      verify(mockCache, times(1)).put(eq("logs.service1"), any(Single.class));
      verify(mockKafkaClient, times(1)).close();
      verify(mockKafkaClient, never()).increasePartitions(anyMap());
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithFirstTimeTopic_StoresInCacheAndReturnsEmpty()
      throws Exception {
    Tenant tenant = Tenant.ABC;
    Set<String> topics = new HashSet<>(Arrays.asList("logs.newservice"));
    Map<String, TopicOffsetInfo> offsetsSumMap = new HashMap<>();
    // First time checking this topic - no cached data exists
    // This should trigger the lastOffsetData == null path (lines 202-207)
    TopicOffsetInfo offsetInfo =
        TopicOffsetInfo.builder().sumOfEndOffsets(50000L).currentNumberOfPartitions(5).build();
    offsetsSumMap.put("logs.newservice", offsetInfo);

    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaConfig.getPartitionRatePerSecond()).thenReturn(1000L);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getEndOffsetSum(anyList())).thenReturn(Single.just(offsetsSumMap));
    // Mock cache to return null (no cached data) - this triggers the null path
    when(mockCache.getIfPresent(anyString())).thenReturn(null);

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      // First call - should store in cache and return empty (no scaling on first check)
      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);
      Map<String, Integer> scalingMap = result.blockingGet();
      Assert.assertNotNull(scalingMap);
      Assert.assertTrue(scalingMap.isEmpty(), "First time check should return empty map");
      // Verify that cache.put was called to store the offset
      verify(mockCache, times(1)).put(eq("logs.newservice"), any(Single.class));
      verify(mockKafkaClient, times(1)).close();
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithErrorCreatingClient_ReturnsError() {
    Tenant tenant = Tenant.ABC;
    RuntimeException error = new RuntimeException("Client creation error");

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig.when(() -> ApplicationConfigUtil.getTenantConfig(tenant)).thenThrow(error);

      // The method should catch the exception and return Single.error()
      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);
      Assert.assertNotNull(result, "Result Single should not be null");

      // When blockingGet() is called on a Single.error(), it throws RuntimeException
      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (RuntimeException e) {
        Assert.assertNotNull(e);
        // Verify the exception contains the original error message
        // RxJava may wrap the exception, so check both the exception and its cause
        String errorMessage = e.getMessage();
        if (e.getCause() != null) {
          errorMessage = e.getCause().getMessage();
        }
        Assert.assertTrue(
            errorMessage != null && errorMessage.contains("Client creation error"),
            "Exception should contain 'Client creation error'. Got: "
                + (errorMessage != null ? errorMessage : e.getClass().getName()));
      }
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithErrorDuringScaling_ClosesClient() throws Exception {
    Tenant tenant = Tenant.ABC;
    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    RuntimeException error = new RuntimeException("Scaling error");
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.error(error));

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);
      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);
      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertNotNull(e);
      }
      verify(mockKafkaClient, times(1)).close();
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithTopicsAndScalingNeeded_ReturnsScalingMap()
      throws Exception {
    Tenant tenant = Tenant.ABC;
    Set<String> topics = new HashSet<>(Arrays.asList("logs.service1", "logs.service2"));
    
    // First call: populate cache with initial offset
    Map<String, TopicOffsetInfo> firstCallOffsetsSumMap = new HashMap<>();
    TopicOffsetInfo firstCallOffsetInfo =
        TopicOffsetInfo.builder().sumOfEndOffsets(100000L).currentNumberOfPartitions(3).build();
    firstCallOffsetsSumMap.put("logs.service1", firstCallOffsetInfo);

    // Second call: use higher offset to trigger scaling
    // If currentOffsetSum = 1100000, lastOffsetSum = 100000 (from first call), 
    // timeDifference = 60 seconds, ingestionRate = (1100000-100000)/60 = 16666.67/sec
    // requiredPartitions = ceil(16666.67 / 1000) = 17, which is > 3 (currentPartitions)
    Map<String, TopicOffsetInfo> secondCallOffsetsSumMap = new HashMap<>();
    TopicOffsetInfo secondCallOffsetInfo =
        TopicOffsetInfo.builder().sumOfEndOffsets(1100000L).currentNumberOfPartitions(3).build();
    secondCallOffsetsSumMap.put("logs.service1", secondCallOffsetInfo);

    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaConfig.getPartitionRatePerSecond()).thenReturn(1000L);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getEndOffsetSum(anyList()))
        .thenReturn(Single.just(firstCallOffsetsSumMap))
        .thenReturn(Single.just(secondCallOffsetsSumMap));
    when(mockKafkaClient.increasePartitions(anyMap()))
        .thenReturn(io.reactivex.Completable.complete());

    // Create cached data for second call - simulate what was stored in first call
    // Use relative timestamp to ensure valid time difference (must be > 0 and <= 300 seconds)
    long firstCallTimestamp = System.currentTimeMillis() - 60000; // 60 seconds ago
    Object offsetWithTimestamp = createOffsetWithTimestamp(100000L, firstCallTimestamp);
    @SuppressWarnings({"unchecked", "rawtypes"})
    Single cachedValue = Single.just(offsetWithTimestamp);
    
    // First call: cache returns null (no cached data)
    // Second call: cache returns the cached value from first call
    when(mockCache.getIfPresent(eq("logs.service1")))
        .thenReturn(null)
        .thenReturn(cachedValue);

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      // First call: should populate cache and return empty (no scaling on first check)
      Single<Map<String, Integer>> firstResult = kafkaService.scaleKafkaPartitions(tenant);
      Map<String, Integer> firstScalingMap = firstResult.blockingGet();
      Assert.assertNotNull(firstScalingMap);
      Assert.assertTrue(firstScalingMap.isEmpty(), "First call should return empty (no cached data)");
      // Verify cache.put was called to store the offset
      verify(mockCache, times(1)).put(eq("logs.service1"), any(Single.class));
      
      // Second call: should trigger scaling due to increased offset
      Single<Map<String, Integer>> secondResult = kafkaService.scaleKafkaPartitions(tenant);
      Map<String, Integer> secondScalingMap = secondResult.blockingGet();
      Assert.assertNotNull(secondScalingMap);
      Assert.assertEquals(secondScalingMap.size(), 1, "Should scale logs.service1");
      Assert.assertNotNull(secondScalingMap.get("logs.service1"));
      
      // Verify exact calculation:
      // ingestionRate = (1100000 - 100000) / 60 = 16666.67/sec
      // requiredPartitions = ceil(16666.67 / 1000) = 17
      Integer requiredPartitions = secondScalingMap.get("logs.service1");
      Assert.assertEquals(requiredPartitions.intValue(), 17, 
          "Should require exactly 17 partitions based on ingestion rate calculation");
      Assert.assertTrue(requiredPartitions > 3, "Should require more than 3 partitions");
      
      // Verify increasePartitions was called with correct parameters
      verify(mockKafkaClient, times(1)).increasePartitions(argThat(map -> 
          map != null && map.size() == 1 && map.get("logs.service1") != null && 
          map.get("logs.service1").equals(17)));
      
      verify(mockKafkaClient, times(2)).close();
      verify(mockKafkaClient, times(1)).increasePartitions(anyMap());
    }
  }

  /**
   * Helper method to create an OffsetWithTimestamp object using reflection since it's a private inner class.
   */
  private Object createOffsetWithTimestamp(long offsetSum, long timestamp) {
    try {
      // OffsetWithTimestamp is a private static inner class, so we need to use reflection
      Class<?> offsetClass = Class.forName("com.logwise.orchestrator.service.KafkaService$OffsetWithTimestamp");
      
      // Get the constructor and make it accessible
      java.lang.reflect.Constructor<?> constructor = offsetClass.getDeclaredConstructor(long.class, long.class);
      constructor.setAccessible(true);
      
      return constructor.newInstance(offsetSum, timestamp);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create OffsetWithTimestamp", e);
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithExceptionDuringScaling_ClosesClient() throws Exception {
    Tenant tenant = Tenant.ABC;
    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    Set<String> topics = new HashSet<>(Arrays.asList("logs.service1"));
    RuntimeException error = new RuntimeException("Scaling error");

    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getEndOffsetSum(anyList())).thenReturn(Single.error(error));

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);
      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);

      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertNotNull(e);
      }
      verify(mockKafkaClient, times(1)).close();
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithErrorInGetEndOffsetSum_ClosesClient() throws Exception {
    Tenant tenant = Tenant.ABC;
    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    Set<String> topics = new HashSet<>(Arrays.asList("logs.service1"));
    RuntimeException error = new RuntimeException("Error getting end offsets");

    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getEndOffsetSum(anyList())).thenReturn(Single.error(error));

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);
      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);

      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertNotNull(e);
        // Verify the exception contains the error message
        String errorMessage = e.getMessage();
        if (e.getCause() != null) {
          errorMessage = e.getCause().getMessage();
        }
        Assert.assertTrue(
            errorMessage != null && errorMessage.contains("Error getting end offsets"),
            "Exception should contain 'Error getting end offsets'. Got: "
                + (errorMessage != null ? errorMessage : e.getClass().getName()));
      }
      verify(mockKafkaClient, times(1)).close();
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithErrorInIncreasePartitions_ClosesClient() throws Exception {
    Tenant tenant = Tenant.ABC;
    Set<String> topics = new HashSet<>(Arrays.asList("logs.service1"));
    Map<String, TopicOffsetInfo> offsetsSumMap = new HashMap<>();
    
    // Setup to trigger scaling: currentOffsetSum = 1100000, lastOffsetSum = 100000, 
    // timeDifference = 60 seconds, ingestionRate = 16666.67/sec, requiredPartitions = 17
    TopicOffsetInfo offsetInfo =
        TopicOffsetInfo.builder().sumOfEndOffsets(1100000L).currentNumberOfPartitions(3).build();
    offsetsSumMap.put("logs.service1", offsetInfo);

    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaConfig.getPartitionRatePerSecond()).thenReturn(1000L);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getEndOffsetSum(anyList())).thenReturn(Single.just(offsetsSumMap));
    
    RuntimeException error = new RuntimeException("Error increasing partitions");
    when(mockKafkaClient.increasePartitions(anyMap())).thenReturn(io.reactivex.Completable.error(error));

    // Create cached data - simulate previous call with offset 100000, 60 seconds ago
    // Use relative timestamp to ensure valid time difference (must be > 0 and <= 300 seconds)
    long cachedTimestamp = System.currentTimeMillis() - 60000; // 60 seconds ago
    Object offsetWithTimestamp = createOffsetWithTimestamp(100000L, cachedTimestamp);
    @SuppressWarnings({"unchecked", "rawtypes"})
    Single cachedValue = Single.just(offsetWithTimestamp);
    
    when(mockCache.getIfPresent(eq("logs.service1"))).thenReturn(cachedValue);

    try (MockedStatic<CaffeineCacheFactory> mockedCacheFactory =
            mockStatic(CaffeineCacheFactory.class);
        MockedStatic<ApplicationConfigUtil> mockedConfig =
            mockStatic(ApplicationConfigUtil.class)) {
      KafkaService kafkaService = createKafkaServiceWithMockedCache(mockedCacheFactory);
      
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);
      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);

      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertNotNull(e);
        // Verify the exception contains the error message
        String errorMessage = e.getMessage();
        if (e.getCause() != null) {
          errorMessage = e.getCause().getMessage();
        }
        Assert.assertTrue(
            errorMessage != null && errorMessage.contains("Error increasing partitions"),
            "Exception should contain 'Error increasing partitions'. Got: "
                + (errorMessage != null ? errorMessage : e.getClass().getName()));
      }
      verify(mockKafkaClient, times(1)).close();
      verify(mockKafkaClient, times(1)).increasePartitions(anyMap());
    }
  }
}
