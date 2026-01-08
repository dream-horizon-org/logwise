package com.logwise.orchestrator.tests.unit.service;

import static org.testng.Assert.*;

import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.dto.kafka.ScalingDecision;
import com.logwise.orchestrator.dto.kafka.TopicPartitionMetrics;
import com.logwise.orchestrator.service.KafkaScalingService;
import com.logwise.orchestrator.setup.BaseTest;
import java.util.*;
import org.apache.kafka.common.TopicPartition;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for KafkaScalingService. */
public class KafkaScalingServiceTest extends BaseTest {

  private KafkaScalingService kafkaScalingService;
  private ApplicationConfig.KafkaConfig kafkaConfig;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    kafkaScalingService = new KafkaScalingService();
    kafkaConfig = new ApplicationConfig.KafkaConfig();
    kafkaConfig.setMaxLagPerPartition(50_000L);
    kafkaConfig.setDefaultPartitions(3);
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithHighLag_ReturnsScalingDecision() {
    String topic = "test-topic";
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
    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition(topic, 0), 100_000L);
    lagMap.put(new TopicPartition(topic, 1), 100_000L);
    lagMap.put(new TopicPartition(topic, 2), 100_000L);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, kafkaConfig);

    assertNotNull(decisions);
    assertEquals(decisions.size(), 1);
    ScalingDecision decision = decisions.get(0);
    assertEquals(decision.getTopic(), topic);
    assertTrue(decision.getNewPartitions() > decision.getCurrentPartitions());
    assertTrue(decision.getFactors().contains("lag"));
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithHighPartitionSize_DoesNotScale() {
    String topic = "test-topic";
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic(topic)
            .partitionCount(3)
            .totalMessages(1_000_000L)
            .avgMessagesPerPartition(333_333L)
            .estimatedSizeBytes(30_000_000_000L) // 30GB total, 10GB per partition
            .partitionOffsets(Map.of(0, 333_333L, 1, 333_333L, 2, 333_333L))
            .build();

    Map<String, TopicPartitionMetrics> metricsMap = Map.of(topic, metrics);
    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition(topic, 0), 10_000L); // Low lag
    lagMap.put(new TopicPartition(topic, 1), 10_000L);
    lagMap.put(new TopicPartition(topic, 2), 10_000L);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, kafkaConfig);

    // Should not scale based on size alone - only lag triggers scaling
    assertNotNull(decisions);
    assertTrue(decisions.isEmpty());
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithHighMessageCount_DoesNotScale() {
    String topic = "test-topic";
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic(topic)
            .partitionCount(3)
            .totalMessages(6_000_000L) // 2M per partition
            .avgMessagesPerPartition(2_000_000L)
            .estimatedSizeBytes(1_000_000_000L)
            .partitionOffsets(Map.of(0, 2_000_000L, 1, 2_000_000L, 2, 2_000_000L))
            .build();

    Map<String, TopicPartitionMetrics> metricsMap = Map.of(topic, metrics);
    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition(topic, 0), 10_000L); // Low lag
    lagMap.put(new TopicPartition(topic, 1), 10_000L);
    lagMap.put(new TopicPartition(topic, 2), 10_000L);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, kafkaConfig);

    // Should not scale based on message count alone - only lag triggers scaling
    assertNotNull(decisions);
    assertTrue(decisions.isEmpty());
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithNoScalingNeeded_ReturnsEmptyList() {
    String topic = "test-topic";
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
    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition(topic, 0), 10_000L);
    lagMap.put(new TopicPartition(topic, 1), 10_000L);
    lagMap.put(new TopicPartition(topic, 2), 10_000L);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, kafkaConfig);

    assertNotNull(decisions);
    assertTrue(decisions.isEmpty());
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithMultipleTopics_ReturnsMultipleDecisions() {
    String topic1 = "topic-1";
    String topic2 = "topic-2";

    TopicPartitionMetrics metrics1 =
        TopicPartitionMetrics.builder()
            .topic(topic1)
            .partitionCount(3)
            .totalMessages(3_000_000L)
            .avgMessagesPerPartition(1_000_000L)
            .estimatedSizeBytes(3_000_000_000L)
            .partitionOffsets(Map.of(0, 1_000_000L, 1, 1_000_000L, 2, 1_000_000L))
            .build();

    TopicPartitionMetrics metrics2 =
        TopicPartitionMetrics.builder()
            .topic(topic2)
            .partitionCount(3)
            .totalMessages(3_000_000L)
            .avgMessagesPerPartition(1_000_000L)
            .estimatedSizeBytes(3_000_000_000L)
            .partitionOffsets(Map.of(0, 1_000_000L, 1, 1_000_000L, 2, 1_000_000L))
            .build();

    Map<String, TopicPartitionMetrics> metricsMap = Map.of(topic1, metrics1, topic2, metrics2);
    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition(topic1, 0), 100_000L);
    lagMap.put(new TopicPartition(topic1, 1), 100_000L);
    lagMap.put(new TopicPartition(topic1, 2), 100_000L);
    lagMap.put(new TopicPartition(topic2, 0), 100_000L);
    lagMap.put(new TopicPartition(topic2, 1), 100_000L);
    lagMap.put(new TopicPartition(topic2, 2), 100_000L);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, kafkaConfig);

    assertNotNull(decisions);
    assertEquals(decisions.size(), 2);
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithHighLag_IncludesLagFactor() {
    String topic = "test-topic";
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic(topic)
            .partitionCount(3)
            .totalMessages(6_000_000L) // High message count
            .avgMessagesPerPartition(2_000_000L)
            .estimatedSizeBytes(30_000_000_000L) // High size
            .partitionOffsets(Map.of(0, 2_000_000L, 1, 2_000_000L, 2, 2_000_000L))
            .build();

    Map<String, TopicPartitionMetrics> metricsMap = Map.of(topic, metrics);
    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition(topic, 0), 100_000L); // High lag
    lagMap.put(new TopicPartition(topic, 1), 100_000L);
    lagMap.put(new TopicPartition(topic, 2), 100_000L);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, kafkaConfig);

    assertNotNull(decisions);
    assertEquals(decisions.size(), 1);
    ScalingDecision decision = decisions.get(0);
    // Only lag factor should be present
    assertTrue(decision.getFactors().contains("lag"));
    assertEquals(decision.getFactors().size(), 1);
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithDefaultPartitions_RoundsUpCorrectly() {
    kafkaConfig.setDefaultPartitions(6);
    String topic = "test-topic";
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
    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition(topic, 0), 100_000L);
    lagMap.put(new TopicPartition(topic, 1), 100_000L);
    lagMap.put(new TopicPartition(topic, 2), 100_000L);

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, kafkaConfig);

    assertNotNull(decisions);
    assertEquals(decisions.size(), 1);
    ScalingDecision decision = decisions.get(0);
    // New partition count should be a multiple of 6
    assertTrue(decision.getNewPartitions() % 6 == 0);
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithNoLagData_DoesNotScale() {
    String topic = "test-topic";
    TopicPartitionMetrics metrics =
        TopicPartitionMetrics.builder()
            .topic(topic)
            .partitionCount(3)
            .totalMessages(1_000_000L)
            .avgMessagesPerPartition(333_333L)
            .estimatedSizeBytes(1_000_000_000L)
            .partitionOffsets(Map.of(0, 333_333L, 1, 333_333L, 2, 333_333L))
            .build();

    Map<String, TopicPartitionMetrics> metricsMap = Map.of(topic, metrics);
    Map<TopicPartition, Long> lagMap = new HashMap<>(); // Empty lag map

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, kafkaConfig);

    // Should not scale - only lag triggers scaling and there's no lag data
    assertNotNull(decisions);
    assertTrue(decisions.isEmpty());
  }

  @Test
  public void testIdentifyTopicsNeedingScaling_WithNullConfig_UsesDefaults() {
    String topic = "test-topic";
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
    Map<TopicPartition, Long> lagMap = new HashMap<>();
    lagMap.put(new TopicPartition(topic, 0), 100_000L);
    lagMap.put(new TopicPartition(topic, 1), 100_000L);
    lagMap.put(new TopicPartition(topic, 2), 100_000L);

    ApplicationConfig.KafkaConfig nullConfig = new ApplicationConfig.KafkaConfig();
    nullConfig.setKafkaBrokersHost("localhost:9092");

    List<ScalingDecision> decisions =
        kafkaScalingService.identifyTopicsNeedingScaling(metricsMap, lagMap, nullConfig);

    // Should use default thresholds
    assertNotNull(decisions);
  }
}
