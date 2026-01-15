package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
import java.util.*;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class KafkaServiceTest extends BaseTest {

  private KafkaService kafkaService;
  private KafkaClientFactory mockKafkaClientFactory;
  private KafkaClient mockKafkaClient;
  private ApplicationConfig.TenantConfig mockTenantConfig;
  private ApplicationConfig.KafkaConfig mockKafkaConfig;
  private ApplicationConfig.SparkConfig mockSparkConfig;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockKafkaClientFactory = mock(KafkaClientFactory.class);
    mockKafkaClient = mock(KafkaClient.class);
    mockTenantConfig = mock(ApplicationConfig.TenantConfig.class);
    mockKafkaConfig = mock(ApplicationConfig.KafkaConfig.class);
    mockSparkConfig = mock(ApplicationConfig.SparkConfig.class);

    when(mockTenantConfig.getKafka()).thenReturn(mockKafkaConfig);
    when(mockTenantConfig.getSpark()).thenReturn(mockSparkConfig);
    when(mockSparkConfig.getSubscribePattern()).thenReturn("logs.*");
    when(mockKafkaConfig.getKafkaType()).thenReturn(KafkaType.MSK);
    when(mockKafkaClientFactory.createKafkaClient(any(ApplicationConfig.KafkaConfig.class)))
        .thenReturn(mockKafkaClient);

    kafkaService = new KafkaService(BaseTest.getReactiveVertx(), mockKafkaClientFactory);
  }

  @Test
  public void testScaleKafkaPartitions_WithScalingDisabled_ReturnsEmptyList() throws Exception {
    Tenant tenant = Tenant.ABC;
    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(false);

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
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

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
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

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
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

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
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
  public void testScaleKafkaPartitions_WithErrorCreatingClient_ReturnsError() {
    Tenant tenant = Tenant.ABC;
    RuntimeException error = new RuntimeException("Client creation error");

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
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

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
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
  public void testScaleKafkaPartitions_WithTopicsAndScalingNeeded_ReturnsDecisions()
      throws Exception {
    Tenant tenant = Tenant.ABC;
    Set<String> topics = new HashSet<>(Arrays.asList("logs.service1", "logs.service2"));
    Map<String, TopicOffsetInfo> offsetsSumMap = new HashMap<>();
    // Create TopicOffsetInfo with values that will trigger scaling
    // High offset sum relative to current partitions to ensure requiredPartitions >
    // currentPartitions
    // If currentOffsetSum = 1000000, lastOffsetSum = 0 (first time), ingestionRate = 1000000/60 =
    // 16666.67
    // requiredPartitions = ceil(16666.67 / 1000) = 17, which is > 3 (currentPartitions)
    TopicOffsetInfo offsetInfo1 =
        TopicOffsetInfo.builder().sumOfEndOffsets(1000000L).currentNumberOfPartitions(3).build();
    offsetsSumMap.put("logs.service1", offsetInfo1);

    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaConfig.getPartitionRatePerSecond()).thenReturn(1000L);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getEndOffsetSum(anyList())).thenReturn(Single.just(offsetsSumMap));
    when(mockKafkaClient.increasePartitions(anyMap()))
        .thenReturn(io.reactivex.Completable.complete());

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<Map<String, Integer>> result = kafkaService.scaleKafkaPartitions(tenant);

      Map<String, Integer> scalingMap = result.blockingGet();
      Assert.assertNotNull(scalingMap);
      Assert.assertEquals(scalingMap.size(), 1);
      Assert.assertNotNull(scalingMap.get("logs.service1"));
      verify(mockKafkaClient, times(1)).close();
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

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
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
}
