package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

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
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.*;
import org.apache.kafka.common.TopicPartition;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for KafkaService. */
public class KafkaServiceTest extends BaseTest {

  private KafkaService kafkaService;
  private KafkaClientFactory mockKafkaClientFactory;
  private SparkCheckpointService mockSparkCheckpointService;
  private KafkaScalingService mockKafkaScalingService;
  private KafkaClient mockKafkaClient;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockKafkaClientFactory = mock(KafkaClientFactory.class);
    mockSparkCheckpointService = mock(SparkCheckpointService.class);
    mockKafkaScalingService = mock(KafkaScalingService.class);
    mockKafkaClient = mock(KafkaClient.class);
    kafkaService =
        new KafkaService(
            mockKafkaClientFactory, mockSparkCheckpointService, mockKafkaScalingService);
  }

  @Test
  public void testScaleKafkaPartitions_WithTopicsNeedingScaling_ScalesPartitions() {
    Tenant tenant = Tenant.ABC;
    String topic = "test-topic";
    Set<String> topics = Set.of(topic);

    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic(topic)
            .partitionCount(3)
            .totalMessages(3_000_000L)
            .avgMessagesPerPartition(1_000_000L)
            .estimatedSizeBytes(3_000_000_000L)
            .partitionOffsets(Map.of(0, 1_000_000L, 1, 1_000_000L, 2, 1_000_000L))
            .build();

    Map<String, TopicPartitionMetrics> metricsMap = Map.of(topic, metrics);
    Map<TopicPartition, Long> endOffsets = new HashMap<>();
    endOffsets.put(new TopicPartition(topic, 0), 1_000_000L);
    endOffsets.put(new TopicPartition(topic, 1), 1_000_000L);
    endOffsets.put(new TopicPartition(topic, 2), 1_000_000L);

    Map<TopicPartition, Long> checkpointOffsets = new HashMap<>();
    checkpointOffsets.put(new TopicPartition(topic, 0), 900_000L);
    checkpointOffsets.put(new TopicPartition(topic, 1), 900_000L);
    checkpointOffsets.put(new TopicPartition(topic, 2), 900_000L);

    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition(topic, 0), 100_000L);
    lagMap.put(new TopicPartition(topic, 1), 100_000L);
    lagMap.put(new TopicPartition(topic, 2), 100_000L);

    ScalingDecision decision =
        ScalingDecision.builder()
            .topic(topic)
            .currentPartitions(3)
            .newPartitions(6)
            .reason("lag: 100000")
            .factors(List.of("lag"))
            .build();

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
        Mockito.mockStatic(ApplicationConfigUtil.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      tenantConfig.getKafka().setKafkaType(KafkaType.EC2);
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      when(mockKafkaClientFactory.createKafkaClient(any())).thenReturn(mockKafkaClient);
      when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
      when(mockKafkaClient.getPartitionMetrics(anyList())).thenReturn(Single.just(metricsMap));
      when(mockKafkaClient.getEndOffsets(anyList())).thenReturn(Single.just(endOffsets));
      when(mockKafkaClient.calculateLag(any(), any())).thenReturn(Single.just(lagMap));
      when(mockKafkaClient.increasePartitions(anyMap())).thenReturn(Completable.complete());

      SparkCheckpointOffsets checkpointOffsetsObj =
          SparkCheckpointOffsets.builder()
              .checkpointPath("checkpoint/application")
              .offsets(checkpointOffsets)
              .available(true)
              .build();

      when(mockSparkCheckpointService.getSparkCheckpointOffsets(tenant))
          .thenReturn(Single.just(checkpointOffsetsObj));
      when(mockKafkaScalingService.identifyTopicsNeedingScaling(any(), any(), any()))
          .thenReturn(List.of(decision));

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);
      List<ScalingDecision> decisions = result.blockingGet();
      assertNotNull(decisions);
      assertEquals(decisions.size(), 1);
      assertEquals(decisions.get(0).getTopic(), topic);

      verify(mockKafkaClient, times(1)).listTopics(anyString());
      verify(mockKafkaClient, times(1)).getPartitionMetrics(anyList());
      verify(mockKafkaClient, times(1)).getEndOffsets(anyList());
      verify(mockKafkaClient, times(1)).calculateLag(any(), any());
      verify(mockKafkaClient, times(1)).increasePartitions(anyMap());
      verify(mockKafkaClient, times(1)).close();
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithNoTopics_CompletesSuccessfully() {
    Tenant tenant = Tenant.ABC;
    Set<String> topics = Collections.emptySet();

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
        Mockito.mockStatic(ApplicationConfigUtil.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      when(mockKafkaClientFactory.createKafkaClient(any())).thenReturn(mockKafkaClient);
      when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);
      List<ScalingDecision> decisions = result.blockingGet();
      assertNotNull(decisions);
      assertTrue(decisions.isEmpty());

      verify(mockKafkaClient, times(1)).listTopics(anyString());
      verify(mockKafkaClient, never()).getPartitionMetrics(anyList());
      verify(mockKafkaClient, times(1)).close();
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithNoScalingNeeded_CompletesSuccessfully() {
    Tenant tenant = Tenant.ABC;
    String topic = "test-topic";
    Set<String> topics = Set.of(topic);

    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic(topic)
            .partitionCount(3)
            .totalMessages(100_000L)
            .avgMessagesPerPartition(33_333L)
            .estimatedSizeBytes(100_000_000L)
            .partitionOffsets(Map.of(0, 33_333L, 1, 33_333L, 2, 33_333L))
            .build();

    Map<String, TopicPartitionMetrics> metricsMap = Map.of(topic, metrics);
    Map<TopicPartition, Long> endOffsets = new HashMap<>();
    endOffsets.put(new TopicPartition(topic, 0), 33_333L);
    endOffsets.put(new TopicPartition(topic, 1), 33_333L);
    endOffsets.put(new TopicPartition(topic, 2), 33_333L);

    Map<TopicPartition, Long> checkpointOffsets = new HashMap<>();
    checkpointOffsets.put(new TopicPartition(topic, 0), 33_000L);
    checkpointOffsets.put(new TopicPartition(topic, 1), 33_000L);
    checkpointOffsets.put(new TopicPartition(topic, 2), 33_000L);

    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition(topic, 0), 333L);
    lagMap.put(new TopicPartition(topic, 1), 333L);
    lagMap.put(new TopicPartition(topic, 2), 333L);

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
        Mockito.mockStatic(ApplicationConfigUtil.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      when(mockKafkaClientFactory.createKafkaClient(any())).thenReturn(mockKafkaClient);
      when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
      when(mockKafkaClient.getPartitionMetrics(anyList())).thenReturn(Single.just(metricsMap));
      when(mockKafkaClient.getEndOffsets(anyList())).thenReturn(Single.just(endOffsets));
      when(mockKafkaClient.calculateLag(any(), any())).thenReturn(Single.just(lagMap));

      SparkCheckpointOffsets checkpointOffsetsObj =
          SparkCheckpointOffsets.builder()
              .checkpointPath("checkpoint/application")
              .offsets(checkpointOffsets)
              .available(true)
              .build();

      when(mockSparkCheckpointService.getSparkCheckpointOffsets(tenant))
          .thenReturn(Single.just(checkpointOffsetsObj));
      when(mockKafkaScalingService.identifyTopicsNeedingScaling(any(), any(), any()))
          .thenReturn(Collections.emptyList());

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);
      List<ScalingDecision> decisions = result.blockingGet();
      assertNotNull(decisions);
      assertTrue(decisions.isEmpty());

      verify(mockKafkaClient, times(1)).listTopics(anyString());
      verify(mockKafkaClient, times(1)).getPartitionMetrics(anyList());
      verify(mockKafkaClient, never()).increasePartitions(anyMap());
      verify(mockKafkaClient, times(1)).close();
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithUnavailableCheckpoint_UsesZeroLag() {
    Tenant tenant = Tenant.ABC;
    String topic = "test-topic";
    Set<String> topics = Set.of(topic);

    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic(topic)
            .partitionCount(3)
            .totalMessages(3_000_000L)
            .avgMessagesPerPartition(1_000_000L)
            .estimatedSizeBytes(3_000_000_000L)
            .partitionOffsets(Map.of(0, 1_000_000L, 1, 1_000_000L, 2, 1_000_000L))
            .build();

    Map<String, TopicPartitionMetrics> metricsMap = Map.of(topic, metrics);
    Map<TopicPartition, Long> endOffsets = new HashMap<>();
    endOffsets.put(new TopicPartition(topic, 0), 1_000_000L);
    endOffsets.put(new TopicPartition(topic, 1), 1_000_000L);
    endOffsets.put(new TopicPartition(topic, 2), 1_000_000L);

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
        Mockito.mockStatic(ApplicationConfigUtil.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      when(mockKafkaClientFactory.createKafkaClient(any())).thenReturn(mockKafkaClient);
      when(mockKafkaClient.listTopics(anyString())).thenReturn(Single.just(topics));
      when(mockKafkaClient.getPartitionMetrics(anyList())).thenReturn(Single.just(metricsMap));
      when(mockKafkaClient.getEndOffsets(anyList())).thenReturn(Single.just(endOffsets));

      SparkCheckpointOffsets checkpointOffsetsObj =
          SparkCheckpointOffsets.builder()
              .checkpointPath("checkpoint/application")
              .offsets(Collections.emptyMap())
              .available(false)
              .build();

      when(mockSparkCheckpointService.getSparkCheckpointOffsets(tenant))
          .thenReturn(Single.just(checkpointOffsetsObj));
      when(mockKafkaScalingService.identifyTopicsNeedingScaling(any(), any(), any()))
          .thenReturn(Collections.emptyList());

      Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);
      List<ScalingDecision> decisions = result.blockingGet();
      assertNotNull(decisions);
      // With unavailable checkpoint, zero lag is used, so no scaling decisions should be made
      assertEquals(decisions.size(), 0);

      verify(mockKafkaClient, times(1)).listTopics(anyString());
      verify(mockKafkaClient, never()).calculateLag(any(), any());
      verify(mockKafkaClient, times(1)).close();
    }
  }

  @Test
  public void testScaleKafkaPartitions_WithError_ClosesClient() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigUtil> mockedConfigUtil =
        Mockito.mockStatic(ApplicationConfigUtil.class)) {
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockedConfigUtil
          .when(() -> ApplicationConfigUtil.getTenantConfig(tenant))
          .thenReturn(tenantConfig);

      when(mockKafkaClientFactory.createKafkaClient(any())).thenReturn(mockKafkaClient);
      when(mockKafkaClient.listTopics(anyString()))
          .thenReturn(Single.error(new RuntimeException("Kafka error")));

      try {
        Single<List<ScalingDecision>> result = kafkaService.scaleKafkaPartitions(tenant);
        result.blockingGet();
        fail("Should have thrown exception");
      } catch (Exception e) {
        // Expected
      }

      verify(mockKafkaClient, times(1)).close();
    }
  }
}
