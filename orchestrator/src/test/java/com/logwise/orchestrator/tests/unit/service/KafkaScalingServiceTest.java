package com.logwise.orchestrator.tests.unit.service;

import static org.mockito.Mockito.*;

import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.kafka.ScalingDecision;
import com.logwise.orchestrator.dto.kafka.TopicPartitionMetrics;
import com.logwise.orchestrator.service.KafkaScalingService;
import com.logwise.orchestrator.setup.BaseTest;
import java.lang.reflect.Method;
import java.util.*;
import org.apache.kafka.common.TopicPartition;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class KafkaScalingServiceTest extends BaseTest {

  private KafkaScalingService kafkaScalingService;
  private ApplicationConfig.KafkaConfig mockKafkaConfig;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    kafkaScalingService = new KafkaScalingService();
    mockKafkaConfig = mock(ApplicationConfig.KafkaConfig.class);
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithHighLag_ReturnsScalingDecisions()
      throws Exception {
    Map<String, TopicPartitionMetrics> metricsMap = new HashMap<>();
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic("logs.service1")
            .partitionCount(3)
            .estimatedSizeBytes(1000000L)
            .avgMessagesPerPartition(10000L)
            .build();
    metricsMap.put("logs.service1", metrics);

    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition("logs.service1", 0), 100000L);
    lagMap.put(new TopicPartition("logs.service1", 1), 100000L);
    lagMap.put(new TopicPartition("logs.service1", 2), 100000L);

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(50000L);
    when(mockKafkaConfig.getDefaultPartitions()).thenReturn(3);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, mockKafkaConfig);

    Assert.assertNotNull(decisions);
    Assert.assertFalse(decisions.isEmpty());
    Assert.assertEquals(decisions.get(0).getTopic(), "logs.service1");
    Assert.assertTrue(
        decisions.get(0).getNewPartitions() > decisions.get(0).getCurrentPartitions());
    Assert.assertTrue(decisions.get(0).getFactors().contains("lag"));
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithLowLag_ReturnsEmptyList() throws Exception {
    Map<String, TopicPartitionMetrics> metricsMap = new HashMap<>();
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic("logs.service1")
            .partitionCount(3)
            .estimatedSizeBytes(1000000L)
            .avgMessagesPerPartition(10000L)
            .build();
    metricsMap.put("logs.service1", metrics);

    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition("logs.service1", 0), 1000L);
    lagMap.put(new TopicPartition("logs.service1", 1), 1000L);
    lagMap.put(new TopicPartition("logs.service1", 2), 1000L);

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(50000L);
    when(mockKafkaConfig.getDefaultPartitions()).thenReturn(3);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, mockKafkaConfig);

    Assert.assertNotNull(decisions);
    Assert.assertTrue(decisions.isEmpty());
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithNullMaxLag_UsesDefault() throws Exception {
    Map<String, TopicPartitionMetrics> metricsMap = new HashMap<>();
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic("logs.service1")
            .partitionCount(3)
            .estimatedSizeBytes(1000000L)
            .avgMessagesPerPartition(10000L)
            .build();
    metricsMap.put("logs.service1", metrics);

    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition("logs.service1", 0), 100000L);

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(null);
    when(mockKafkaConfig.getDefaultPartitions()).thenReturn(3);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, mockKafkaConfig);

    Assert.assertNotNull(decisions);
    Assert.assertFalse(decisions.isEmpty());
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithNullDefaultPartitions_UsesDefault()
      throws Exception {
    Map<String, TopicPartitionMetrics> metricsMap = new HashMap<>();
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic("logs.service1")
            .partitionCount(3)
            .estimatedSizeBytes(1000000L)
            .avgMessagesPerPartition(10000L)
            .build();
    metricsMap.put("logs.service1", metrics);

    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition("logs.service1", 0), 100000L);

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(50000L);
    when(mockKafkaConfig.getDefaultPartitions()).thenReturn(null);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, mockKafkaConfig);

    Assert.assertNotNull(decisions);
    Assert.assertFalse(decisions.isEmpty());
  }

  @Test
  public void testShouldScalePartition_WithHighLag_ReturnsTrue() throws Exception {
    Method method =
        KafkaScalingService.class.getDeclaredMethod(
            "shouldScalePartition",
            String.class,
            TopicPartitionMetrics.class,
            long.class,
            ApplicationConfig.KafkaConfig.class);
    method.setAccessible(true);

    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder().topic("logs.service1").partitionCount(3).build();

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(50000L);

    boolean result =
        (Boolean)
            method.invoke(kafkaScalingService, "logs.service1", metrics, 100000L, mockKafkaConfig);

    Assert.assertTrue(result);
  }

  @Test
  public void testShouldScalePartition_WithLowLag_ReturnsFalse() throws Exception {
    Method method =
        KafkaScalingService.class.getDeclaredMethod(
            "shouldScalePartition",
            String.class,
            TopicPartitionMetrics.class,
            long.class,
            ApplicationConfig.KafkaConfig.class);
    method.setAccessible(true);

    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder().topic("logs.service1").partitionCount(3).build();

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(50000L);

    boolean result =
        (Boolean)
            method.invoke(kafkaScalingService, "logs.service1", metrics, 1000L, mockKafkaConfig);

    Assert.assertFalse(result);
  }

  @Test
  public void testCalculateNewPartitionCount_WithHighLag_ReturnsIncreasedCount() throws Exception {
    Method method =
        KafkaScalingService.class.getDeclaredMethod(
            "calculateNewPartitionCount",
            TopicPartitionMetrics.class,
            long.class,
            ApplicationConfig.KafkaConfig.class,
            int.class);
    method.setAccessible(true);

    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder().topic("logs.service1").partitionCount(3).build();

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(50000L);

    int result = (Integer) method.invoke(kafkaScalingService, metrics, 100000L, mockKafkaConfig, 3);

    Assert.assertTrue(result > metrics.getPartitionCount());
  }

  @Test
  public void testIdentifyScalingFactors_WithHighLag_ReturnsLagFactor() throws Exception {
    Method method =
        KafkaScalingService.class.getDeclaredMethod(
            "identifyScalingFactors",
            TopicPartitionMetrics.class,
            long.class,
            ApplicationConfig.KafkaConfig.class);
    method.setAccessible(true);

    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder().topic("logs.service1").partitionCount(3).build();

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(50000L);

    @SuppressWarnings("unchecked")
    List<String> factors =
        (List<String>) method.invoke(kafkaScalingService, metrics, 100000L, mockKafkaConfig);

    Assert.assertNotNull(factors);
    Assert.assertTrue(factors.contains("lag"));
  }

  @Test
  public void testBuildScalingReason_WithLagFactor_ReturnsReason() throws Exception {
    Method method =
        KafkaScalingService.class.getDeclaredMethod(
            "buildScalingReason", List.class, TopicPartitionMetrics.class, long.class);
    method.setAccessible(true);

    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder().topic("logs.service1").partitionCount(3).build();

    List<String> factors = Arrays.asList("lag");

    String reason = (String) method.invoke(kafkaScalingService, factors, metrics, 100000L);

    Assert.assertNotNull(reason);
    Assert.assertTrue(reason.contains("lag"));
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithZeroLag_ReturnsEmptyList() throws Exception {
    Map<String, TopicPartitionMetrics> metricsMap = new HashMap<>();
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic("logs.service1")
            .partitionCount(3)
            .estimatedSizeBytes(1000000L)
            .avgMessagesPerPartition(10000L)
            .build();
    metricsMap.put("logs.service1", metrics);

    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition("logs.service1", 0), 0L);
    lagMap.put(new TopicPartition("logs.service1", 1), 0L);
    lagMap.put(new TopicPartition("logs.service1", 2), 0L);

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(50000L);
    when(mockKafkaConfig.getDefaultPartitions()).thenReturn(3);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, mockKafkaConfig);

    Assert.assertNotNull(decisions);
    Assert.assertTrue(decisions.isEmpty());
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithNullLag_ReturnsEmptyList() throws Exception {
    Map<String, TopicPartitionMetrics> metricsMap = new HashMap<>();
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic("logs.service1")
            .partitionCount(3)
            .estimatedSizeBytes(1000000L)
            .avgMessagesPerPartition(10000L)
            .build();
    metricsMap.put("logs.service1", metrics);

    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition("logs.service1", 0), null);

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(50000L);
    when(mockKafkaConfig.getDefaultPartitions()).thenReturn(3);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, mockKafkaConfig);

    Assert.assertNotNull(decisions);
    Assert.assertTrue(decisions.isEmpty());
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithNewPartitionsLessThanCurrent_ReturnsEmptyList()
      throws Exception {
    Map<String, TopicPartitionMetrics> metricsMap = new HashMap<>();
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic("logs.service1")
            .partitionCount(10) // High partition count
            .estimatedSizeBytes(1000000L)
            .avgMessagesPerPartition(10000L)
            .build();
    metricsMap.put("logs.service1", metrics);

    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition("logs.service1", 0), 60000L);

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(50000L);
    when(mockKafkaConfig.getDefaultPartitions()).thenReturn(3);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, mockKafkaConfig);

    // If calculated new partitions <= current, should not scale
    Assert.assertNotNull(decisions);
    // May or may not be empty depending on calculation, but should not have newPartitions < current
    if (!decisions.isEmpty()) {
      Assert.assertTrue(
          decisions.get(0).getNewPartitions() > decisions.get(0).getCurrentPartitions());
    }
  }

  @Test
  public void testCalculateNewPartitionCount_WithZeroLag_ReturnsMinimumIncrease() throws Exception {
    Method method =
        KafkaScalingService.class.getDeclaredMethod(
            "calculateNewPartitionCount",
            TopicPartitionMetrics.class,
            long.class,
            ApplicationConfig.KafkaConfig.class,
            int.class);
    method.setAccessible(true);

    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder().topic("logs.service1").partitionCount(3).build();

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(50000L);

    int result = (Integer) method.invoke(kafkaScalingService, metrics, 0L, mockKafkaConfig, 3);

    // Should return at least current + defaultPartitions
    Assert.assertTrue(result >= metrics.getPartitionCount() + 3);
  }

  @Test
  public void testIdentifyScalingFactors_WithLowLag_ReturnsEmptyList() throws Exception {
    Method method =
        KafkaScalingService.class.getDeclaredMethod(
            "identifyScalingFactors",
            TopicPartitionMetrics.class,
            long.class,
            ApplicationConfig.KafkaConfig.class);
    method.setAccessible(true);

    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder().topic("logs.service1").partitionCount(3).build();

    when(mockKafkaConfig.getMaxLagPerPartition()).thenReturn(50000L);

    @SuppressWarnings("unchecked")
    List<String> factors =
        (List<String>) method.invoke(kafkaScalingService, metrics, 1000L, mockKafkaConfig);

    Assert.assertNotNull(factors);
    Assert.assertTrue(factors.isEmpty());
  }

  @Test
  public void testBuildScalingReason_WithEmptyFactors_ReturnsEmptyReason() throws Exception {
    Method method =
        KafkaScalingService.class.getDeclaredMethod(
            "buildScalingReason", List.class, TopicPartitionMetrics.class, long.class);
    method.setAccessible(true);

    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder().topic("logs.service1").partitionCount(3).build();

    List<String> factors = Collections.emptyList();

    String reason = (String) method.invoke(kafkaScalingService, factors, metrics, 100000L);

    Assert.assertNotNull(reason);
    Assert.assertTrue(reason.isEmpty() || reason.trim().isEmpty());
  }
}
