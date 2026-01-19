package com.logwise.orchestrator.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.google.inject.Inject;
import com.logwise.orchestrator.CaffeineCacheFactory;
import com.logwise.orchestrator.client.kafka.KafkaClient;
import com.logwise.orchestrator.config.ApplicationConfig.KafkaConfig;
import com.logwise.orchestrator.config.ApplicationConfig.SparkConfig;
import com.logwise.orchestrator.dto.kafka.TopicOffsetInfo;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.KafkaClientFactory;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import java.util.*;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for scaling Kafka partitions based on metrics and Spark checkpoint lag. Orchestrates the
 * scaling flow: get metrics, calculate lag, make decisions, scale partitions.
 */
@Slf4j
public class KafkaService {

  private final KafkaClientFactory kafkaClientFactory;
  private final Cache<String, Single<OffsetWithTimestamp>> topicOffsetSumCache;

  /** Data class to hold offset sum and timestamp together. */
  @Getter
  private static class OffsetWithTimestamp {
    private final long offsetSum;
    private final long timestamp;

    public OffsetWithTimestamp(long offsetSum, long timestamp) {
      this.offsetSum = offsetSum;
      this.timestamp = timestamp;
    }
  }

  @Inject
  public KafkaService(Vertx vertx, KafkaClientFactory kafkaClientFactory) {
    this.kafkaClientFactory = kafkaClientFactory;
    this.topicOffsetSumCache =
        CaffeineCacheFactory.createCache(vertx, "kafka-topic-offset-sum-cache");
  }

  /**
   * Scale Kafka partitions for a tenant based on metrics and lag.
   *
   * @param tenant Tenant to scale partitions for
   * @return Single that emits the list of scaling decisions made
   */
  public Single<Map<String, Integer>> scaleKafkaPartitions(Tenant tenant) {
    log.info("Starting Kafka partition scaling for tenant: {}", tenant);

    try {
      var tenantConfig = ApplicationConfigUtil.getTenantConfig(tenant);
      KafkaConfig kafkaConfig = tenantConfig.getKafka();

      // Check feature flag
      if (kafkaConfig.getEnablePartitionScaling() == null
          || !kafkaConfig.getEnablePartitionScaling()) {
        log.info("Partition scaling is disabled for tenant: {}", tenant);
        return Single.just(Collections.emptyMap());
      }

      SparkConfig sparkConfig = tenantConfig.getSpark();

      // Create appropriate Kafka client
      KafkaClient kafkaClient = kafkaClientFactory.createKafkaClient(kafkaConfig);

      try {
        return performScaling(kafkaClient, kafkaConfig, sparkConfig, tenant)
            .doFinally(
                () -> {
                  kafkaClient.close();
                  log.info("Completed Kafka partition scaling for tenant: {}", tenant);
                });
      } catch (Exception e) {
        kafkaClient.close();
        log.error("Error during scaling for tenant: {}", tenant, e);
        return Single.error(e);
      }
    } catch (Exception e) {
      log.error("Error creating Kafka client for tenant: {}", tenant, e);
      return Single.error(e);
    }
  }

  private Single<Map<String, Integer>> performScaling(
      KafkaClient kafkaClient, KafkaConfig kafkaConfig, SparkConfig sparkConfig, Tenant tenant) {

    long startTime = System.currentTimeMillis();
    log.info(
        "Starting partition scaling for tenant: {}, pattern: {}, kafkaType: {}",
        tenant,
        sparkConfig.getSubscribePattern(),
        kafkaConfig.getKafkaType());

    // 1. Get topics matching Spark's subscribe pattern
    return kafkaClient
        .listTopics(sparkConfig.getSubscribePattern())
        .flatMap(
            topics -> {
              if (topics.isEmpty()) {
                log.info(
                    "No topics found matching pattern: {} for tenant: {}",
                    sparkConfig.getSubscribePattern(),
                    tenant);
                return Single.just(Collections.emptyMap());
              }

              List<String> topicList = new ArrayList<>(topics);
              log.info(
                  "Found {} topics matching pattern for tenant: {}: {}",
                  topicList.size(),
                  tenant,
                  topicList);

              // 2. get end offset sum for each topic
              return kafkaClient
                  .getEndOffsetSum(topicList)
                  .flatMap(
                      offsetsSum -> {
                        Map<String, Integer> scalingMap =
                            calculateScalingDecisions(offsetsSum, kafkaConfig);

                        if (scalingMap.isEmpty()) {
                          log.info("No partitions to increase");
                          return Single.just(Collections.emptyMap());
                        }

                        return kafkaClient
                            .increasePartitions(scalingMap)
                            .doOnComplete(
                                () -> {
                                  log.info(
                                      "Successfully scaled the partitions for {} topics",
                                      scalingMap.size());
                                })
                            .doOnError(
                                th -> {
                                  long duration = System.currentTimeMillis() - startTime;
                                  log.error(
                                      "Error increasing partitions for tenant: {} after {}ms",
                                      tenant,
                                      duration,
                                      th);
                                })
                            .toSingle(() -> scalingMap);
                      });
            });
  }

  /**
   * Calculates scaling decisions for each topic based on ingestion rate and current partition
   * count.
   *
   * @param offsetsSum Map of topic names to their offset information
   * @param kafkaConfig Kafka configuration containing partition rate per second
   * @return Map of topic names to required partition counts (only includes topics that need
   *     scaling)
   */
  private Map<String, Integer> calculateScalingDecisions(
      Map<String, TopicOffsetInfo> offsetsSum, KafkaConfig kafkaConfig) {
    return offsetsSum.entrySet().stream()
        .map(
            entry -> {
              String topic = entry.getKey();
              int requiredPartitions =
                  calculateRequiredPartitions(topic, entry.getValue(), kafkaConfig);
              return new AbstractMap.SimpleEntry<>(topic, requiredPartitions);
            })
        .filter(entry -> entry.getValue() > 0)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Calculates the required number of partitions for a topic based on ingestion rate.
   *
   * @param topic Topic name
   * @param offsetInfo Current offset information for the topic
   * @param kafkaConfig Kafka configuration containing partition rate per second
   * @return Required number of partitions, or -1 if no scaling is needed
   */
  private int calculateRequiredPartitions(
      String topic, TopicOffsetInfo offsetInfo, KafkaConfig kafkaConfig) {
    long currentOffsetSum = offsetInfo.getSumOfEndOffsets();

    OffsetWithTimestamp lastOffsetData = getLastTimeOffsetSumFromCache(topic);

    if (lastOffsetData == null) {
      updateLastTimeOffsetSumInCache(topic, offsetInfo.getSumOfEndOffsets());

      log.info(
          "Skipping the kafka partition scaling due to lastOffsetData is not available in cache");

      // No previous data, store current and return -1 (no scaling)
      return -1;
    }

    long currentTimestamp = System.currentTimeMillis();
    long timeDifferenceSeconds = (currentTimestamp - lastOffsetData.getTimestamp()) / 1000;

    if (timeDifferenceSeconds <= 30 || timeDifferenceSeconds > 300) {
      // Time difference is invalid or too long return -1
      updateLastTimeOffsetSumInCache(topic, offsetInfo.getSumOfEndOffsets());
      log.info(
          "Skipping the kafka partition scaling due to cache is {} second old",
          timeDifferenceSeconds);
      return -1;
    }

    long offsetDifference = currentOffsetSum - lastOffsetData.getOffsetSum();
    long ingestionRate = offsetDifference / timeDifferenceSeconds;
    int requiredPartitions =
        (int) Math.ceil((double) ingestionRate / kafkaConfig.getPartitionRatePerSecond());
    int currentPartitions = offsetInfo.getCurrentNumberOfPartitions();

    updateLastTimeOffsetSumInCache(topic, offsetInfo.getSumOfEndOffsets());

    // Only scale if required partitions exceed current partitions
    if (requiredPartitions <= currentPartitions) {
      log.info(
          "Skipping the kafka partition scaling because requiredPartitions is less then currentPartitions");
      return -1;
    }

    return requiredPartitions;
  }

  /**
   * Get the last time offset sum from cache for a given topic.
   *
   * @param topic Topic name
   * @return OffsetWithTimestamp containing last offset sum and timestamp, or null if not found in
   *     cache
   */
  private OffsetWithTimestamp getLastTimeOffsetSumFromCache(String topic) {
    Single<OffsetWithTimestamp> cachedValue = topicOffsetSumCache.getIfPresent(topic);
    if (cachedValue != null) {
      return cachedValue.blockingGet();
    }
    return null;
  }

  /**
   * Update the last time offset sum in cache for a given topic.
   *
   * @param topic Topic name
   * @param offsetSum Offset sum to store
   */
  private void updateLastTimeOffsetSumInCache(String topic, Long offsetSum) {
    long currentTimestamp = System.currentTimeMillis();
    topicOffsetSumCache.put(
        topic, Single.just(new OffsetWithTimestamp(offsetSum, currentTimestamp)));
  }
}
