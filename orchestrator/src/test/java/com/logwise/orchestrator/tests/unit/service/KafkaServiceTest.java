package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.client.kafka.KafkaClient;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.kafka.ScalingDecision;
import com.logwise.orchestrator.dto.kafka.SparkCheckpointOffsets;
import com.logwise.orchestrator.dto.kafka.TopicPartitionMetrics;
import com.logwise.orchestrator.enums.KafkaType;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.KafkaClientFactory;
import com.logwise.orchestrator.service.KafkaScalingService;
import com.logwise.orchestrator.service.KafkaService;
import com.logwise.orchestrator.service.SparkCheckpointService;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import io.reactivex.Single;
import java.util.*;
import org.apache.kafka.common.TopicPartition;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class KafkaServiceTest extends BaseTest {

  private KafkaService kafkaService;
  private KafkaClientFactory mockKafkaClientFactory;
  private SparkCheckpointService mockSparkCheckpointService;
  private KafkaScalingService mockKafkaScalingService;
  private KafkaClient mockKafkaClient;
  private ApplicationConfig.TenantConfig mockTenantConfig;
  private ApplicationConfig.KafkaConfig mockKafkaConfig;
  private ApplicationConfig.SparkConfig mockSparkConfig;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockKafkaClientFactory = mock(KafkaClientFactory.class);
    mockSparkCheckpointService = mock(SparkCheckpointService.class);
    mockKafkaScalingService = mock(KafkaScalingService.class);
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

    kafkaService =
        new KafkaService(
            mockKafkaClientFactory, mockSparkCheckpointService, mockKafkaScalingService);
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

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);

      List<ScalingDecision> decisions = result.blockingGet();
      Assert.assertNotNull(decisions);
      Assert.assertTrue(decisions.isEmpty());
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

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);

      List<ScalingDecision> decisions = result.blockingGet();
      Assert.assertNotNull(decisions);
      Assert.assertTrue(decisions.isEmpty());
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

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);

      List<ScalingDecision> decisions = result.blockingGet();
      Assert.assertNotNull(decisions);
      Assert.assertTrue(decisions.isEmpty());
      verify(mockKafkaClient, times(1)).listTopics(anyString());
      verify(mockKafkaClient, times(1)).close();
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithTopicsButNoScalingNeeded_ReturnsEmptyList()
      throws Exception {
    Tenant tenant = Tenant.ABC;
    Set<String> topics = new HashSet<>(Arrays.asList("logs.service1", "logs.service2"));
    Map<String, TopicPartitionMetrics> metricsMap = new HashMap<>();
    SparkCheckpointOffsets checkpointOffsets =
        SparkCheckpointOffsets.builder().available(true).offsets(Collections.emptyMap()).build();
    Map<TopicPartition, Long> endOffsets = new HashMap<>();
    Map<TopicPartition, Long> lagMap = new HashMap<>();

    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getPartitionMetrics(anyList())).thenReturn(Single.just(metricsMap));
    when(mockSparkCheckpointService.getSparkCheckpointOffsets(tenant))
        .thenReturn(Single.just(checkpointOffsets));
    when(mockKafkaClient.getEndOffsets(anyList())).thenReturn(Single.just(endOffsets));
    when(mockKafkaClient.calculateLag(anyMap(), anyMap())).thenReturn(Single.just(lagMap));
    when(mockKafkaScalingService.identifyTopicsNeedingScaling(anyMap(), anyMap(), any()))
        .thenReturn(Collections.emptyList());

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);

      List<ScalingDecision> decisions = result.blockingGet();
      Assert.assertNotNull(decisions);
      Assert.assertTrue(decisions.isEmpty());
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

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);

      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertNotNull(e);
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

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);

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
    Map<String, TopicPartitionMetrics> metricsMap = new HashMap<>();
    TopicPartitionMetrics metrics1 =
        TopicPartitionMetrics.builder()
            .topic("logs.service1")
            .partitionCount(3)
            .totalMessages(1000L)
            .build();
    metricsMap.put("logs.service1", metrics1);

    SparkCheckpointOffsets checkpointOffsets =
        SparkCheckpointOffsets.builder().available(true).offsets(Collections.emptyMap()).build();
    Map<TopicPartition, Long> endOffsets = new HashMap<>();
    Map<TopicPartition, Long> lagMap = new HashMap<>();
    List<ScalingDecision> scalingDecisions =
        Arrays.asList(
            ScalingDecision.builder()
                .topic("logs.service1")
                .currentPartitions(3)
                .newPartitions(6)
                .reason("High lag")
                .build());

    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getPartitionMetrics(anyList())).thenReturn(Single.just(metricsMap));
    when(mockSparkCheckpointService.getSparkCheckpointOffsets(tenant))
        .thenReturn(Single.just(checkpointOffsets));
    when(mockKafkaClient.getEndOffsets(anyList())).thenReturn(Single.just(endOffsets));
    when(mockKafkaClient.calculateLag(anyMap(), anyMap())).thenReturn(Single.just(lagMap));
    when(mockKafkaScalingService.identifyTopicsNeedingScaling(anyMap(), anyMap(), any()))
        .thenReturn(scalingDecisions);
    when(mockKafkaClient.increasePartitions(anyMap()))
        .thenReturn(io.reactivex.Completable.complete());

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);

      List<ScalingDecision> decisions = result.blockingGet();
      Assert.assertNotNull(decisions);
      Assert.assertEquals(decisions.size(), 1);
      Assert.assertEquals(decisions.get(0).getTopic(), "logs.service1");
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
    when(mockKafkaClient.getPartitionMetrics(anyList())).thenReturn(Single.error(error));

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);

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
  public void testScaleKafkaPartitions_WithUnavailableCheckpoint_UsesZeroLag() throws Exception {
    Tenant tenant = Tenant.ABC;
    Set<String> topics = new HashSet<>(Arrays.asList("logs.service1"));
    Map<String, TopicPartitionMetrics> metricsMap = new HashMap<>();
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic("logs.service1")
            .partitionCount(3)
            .totalMessages(1000L)
            .build();
    metricsMap.put("logs.service1", metrics);

    SparkCheckpointOffsets checkpointOffsets =
        SparkCheckpointOffsets.builder().available(false).offsets(Collections.emptyMap()).build();
    Map<TopicPartition, Long> endOffsets = new HashMap<>();
    Map<TopicPartition, Long> lagMap = new HashMap<>();

    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getPartitionMetrics(anyList())).thenReturn(Single.just(metricsMap));
    when(mockSparkCheckpointService.getSparkCheckpointOffsets(tenant))
        .thenReturn(Single.just(checkpointOffsets));
    when(mockKafkaClient.getEndOffsets(anyList())).thenReturn(Single.just(endOffsets));
    when(mockKafkaScalingService.identifyTopicsNeedingScaling(anyMap(), anyMap(), any()))
        .thenReturn(Collections.emptyList());

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);

      List<ScalingDecision> decisions = result.blockingGet();
      Assert.assertNotNull(decisions);
      // Should use zero lag when checkpoint unavailable
      verify(mockKafkaClient, never()).calculateLag(anyMap(), anyMap());
      verify(mockKafkaClient, times(1)).close();
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithEmptyCheckpointOffsets_UsesZeroLag() throws Exception {
    Tenant tenant = Tenant.ABC;
    Set<String> topics = new HashSet<>(Arrays.asList("logs.service1"));
    Map<String, TopicPartitionMetrics> metricsMap = new HashMap<>();
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic("logs.service1")
            .partitionCount(3)
            .totalMessages(1000L)
            .build();
    metricsMap.put("logs.service1", metrics);

    SparkCheckpointOffsets checkpointOffsets =
        SparkCheckpointOffsets.builder().available(true).offsets(Collections.emptyMap()).build();
    Map<TopicPartition, Long> endOffsets = new HashMap<>();
    Map<TopicPartition, Long> lagMap = new HashMap<>();

    when(mockKafkaConfig.getEnablePartitionScaling()).thenReturn(true);
    when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
    when(mockKafkaClient.getPartitionMetrics(anyList())).thenReturn(Single.just(metricsMap));
    when(mockSparkCheckpointService.getSparkCheckpointOffsets(tenant))
        .thenReturn(Single.just(checkpointOffsets));
    when(mockKafkaClient.getEndOffsets(anyList())).thenReturn(Single.just(endOffsets));
    when(mockKafkaScalingService.identifyTopicsNeedingScaling(anyMap(), anyMap(), any()))
        .thenReturn(Collections.emptyList());

    try (MockedStatic<ApplicationConfigUtil> mockedConfig =
        mockStatic(ApplicationConfigUtil.class)) {
      mockedConfig
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(mockTenantConfig);

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);

      List<ScalingDecision> decisions = result.blockingGet();
      Assert.assertNotNull(decisions);
      // Should use zero lag when offsets are empty
      verify(mockKafkaClient, never()).calculateLag(anyMap(), anyMap());
      verify(mockKafkaClient, times(1)).close();
    }
  }
}
