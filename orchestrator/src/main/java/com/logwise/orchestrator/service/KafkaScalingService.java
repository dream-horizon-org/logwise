package com.logwise.orchestrator.service;

import com.google.inject.Inject;
import com.logwise.orchestrator.config.ApplicationConfig.KafkaConfig;
import com.logwise.orchestrator.dto.kafka.ScalingDecision;
import com.logwise.orchestrator.dto.kafka.TopicPartitionMetrics;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;

/**
 * Service for making scaling decisions based on consumer lag. Implements the scaling algorithm that
 * considers lag as the primary signal for partition scaling.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class KafkaScalingService {

  /**
   * Identify topics that need scaling based on metrics and lag.
   *
   * @param metricsMap Map of topic name to partition metrics
   * @param lagMap Map of TopicPartition to lag (endOffset - checkpointOffset)
   * @param kafkaConfig Kafka configuration with scaling thresholds
   * @return List of scaling decisions
   */
  public List<ScalingDecision> identifyTopicsNeedingScaling(
      Map<String, TopicPartitionMetrics> metricsMap,
      Map<TopicPartition, Long> lagMap,
      KafkaConfig kafkaConfig) {

    List<ScalingDecision> scalingDecisions = new ArrayList<>();
    int defaultPartitions =
        kafkaConfig.getDefaultPartitions() != null ? kafkaConfig.getDefaultPartitions() : 3;

    for (Map.Entry<String, TopicPartitionMetrics> entry : metricsMap.entrySet()) {
      String topic = entry.getKey();
      TopicPartitionMetrics metrics = entry.getValue();

      // Calculate average lag per partition for this topic
      long totalLag = 0;
      int partitionsWithLag = 0;
      for (int partitionId = 0; partitionId < metrics.getPartitionCount(); partitionId++) {
        TopicPartition tp = new TopicPartition(topic, partitionId);
        Long lag = lagMap.get(tp);
        if (lag != null && lag > 0) {
          totalLag += lag;
          partitionsWithLag++;
        }
      }
      long avgLagPerPartition = partitionsWithLag > 0 ? totalLag / partitionsWithLag : 0;

      if (shouldScalePartition(topic, metrics, avgLagPerPartition, kafkaConfig)) {
        int newPartitionCount =
            calculateNewPartitionCount(metrics, avgLagPerPartition, kafkaConfig, defaultPartitions);

        if (newPartitionCount > metrics.getPartitionCount()) {
          List<String> factors = identifyScalingFactors(metrics, avgLagPerPartition, kafkaConfig);

          ScalingDecision decision =
              ScalingDecision.builder()
                  .topic(topic)
                  .currentPartitions(metrics.getPartitionCount())
                  .newPartitions(newPartitionCount)
                  .reason(buildScalingReason(factors, metrics, avgLagPerPartition))
                  .factors(factors)
                  .lagPerPartition(avgLagPerPartition)
                  .sizePerPartition(
                      metrics.getEstimatedSizeBytes() / Math.max(metrics.getPartitionCount(), 1))
                  .messagesPerPartition(metrics.getAvgMessagesPerPartition())
                  .build();

          scalingDecisions.add(decision);
        }
      }
    }

    return scalingDecisions;
  }

  private boolean shouldScalePartition(
      String topic, TopicPartitionMetrics metrics, long lagPerPartition, KafkaConfig kafkaConfig) {

    long maxLagPerPartition =
        kafkaConfig.getMaxLagPerPartition() != null ? kafkaConfig.getMaxLagPerPartition() : 50_000L;

    // Consumer Lag (Primary Signal)
    if (lagPerPartition > maxLagPerPartition) {
      log.info(
          "Topic {} needs scaling: {} lag/partition (threshold: {})",
          topic,
          lagPerPartition,
          maxLagPerPartition);
      return true;
    }

    return false;
  }

  private int calculateNewPartitionCount(
      TopicPartitionMetrics metrics,
      long lagPerPartition,
      KafkaConfig kafkaConfig,
      int defaultPartitions) {

    int currentPartitions = metrics.getPartitionCount();
    long maxLagPerPartition =
        kafkaConfig.getMaxLagPerPartition() != null ? kafkaConfig.getMaxLagPerPartition() : 50_000L;

    // Lag-based scaling: calculate how many partitions we need to handle the lag
    // If lag is 100k and max is 50k, we need at least 2x partitions (but we want more headroom)
    long requiredForLag = lagPerPartition > 0 ? (lagPerPartition / maxLagPerPartition) + 1 : 1;

    // Calculate total partitions needed: current * multiplier
    int partitionsNeeded = (int) (currentPartitions * requiredForLag);

    // Round up to nearest multiple of defaultPartitions
    int newPartitionCount =
        (int) Math.ceil((double) partitionsNeeded / defaultPartitions) * defaultPartitions;

    // Ensure we're increasing by at least defaultPartitions
    return Math.max(newPartitionCount, currentPartitions + defaultPartitions);
  }

  private List<String> identifyScalingFactors(
      TopicPartitionMetrics metrics, long lagPerPartition, KafkaConfig kafkaConfig) {

    List<String> factors = new ArrayList<>();
    long maxLagPerPartition =
        kafkaConfig.getMaxLagPerPartition() != null ? kafkaConfig.getMaxLagPerPartition() : 50_000L;

    if (lagPerPartition > maxLagPerPartition) {
      factors.add("lag");
    }

    return factors;
  }

  private String buildScalingReason(
      List<String> factors, TopicPartitionMetrics metrics, long lagPerPartition) {

    List<String> reasons = new ArrayList<>();
    if (factors.contains("lag")) {
      reasons.add(String.format("lag: %d", lagPerPartition));
    }

    return String.join(", ", reasons);
  }
}
