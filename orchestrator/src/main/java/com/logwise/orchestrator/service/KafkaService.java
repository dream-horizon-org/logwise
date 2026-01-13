package com.logwise.orchestrator.service;

import com.google.inject.Inject;
import com.logwise.orchestrator.client.kafka.KafkaClient;
import com.logwise.orchestrator.config.ApplicationConfig.KafkaConfig;
import com.logwise.orchestrator.config.ApplicationConfig.SparkConfig;
import com.logwise.orchestrator.dto.kafka.ScalingDecision;
import com.logwise.orchestrator.dto.kafka.TopicPartitionMetrics;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.KafkaClientFactory;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import io.reactivex.Single;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;

/**
 * Service for scaling Kafka partitions based on metrics and Spark checkpoint lag. Orchestrates the
 * scaling flow: get metrics, calculate lag, make decisions, scale partitions.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class KafkaService {

  private final KafkaClientFactory kafkaClientFactory;
  private final SparkCheckpointService sparkCheckpointService;
  private final KafkaScalingService kafkaScalingService;

  /**
   * Scale Kafka partitions for a tenant based on metrics and lag.
   *
   * @param tenant Tenant to scale partitions for
   * @return Single that emits the list of scaling decisions made
   */
  public Single<List<ScalingDecision>> scaleKafkaPartitions(Tenant tenant) {
    log.info("Starting Kafka partition scaling for tenant: {}", tenant);

    try {
      var tenantConfig = ApplicationConfigUtil.getTenantConfig(tenant);
      KafkaConfig kafkaConfig = tenantConfig.getKafka();

      // Check feature flag
      if (kafkaConfig.getEnablePartitionScaling() == null
          || !kafkaConfig.getEnablePartitionScaling()) {
        log.info("Partition scaling is disabled for tenant: {}", tenant);
        return Single.just(Collections.emptyList());
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

  private Single<List<ScalingDecision>> performScaling(
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
                return Single.just(Collections.<ScalingDecision>emptyList());
              }

              List<String> topicList = new ArrayList<>(topics);
              log.info(
                  "Found {} topics matching pattern for tenant: {}: {}",
                  topicList.size(),
                  tenant,
                  topicList);

              // 2. Get partition metrics (replaces Kafka Manager producer rate)
              return kafkaClient
                  .getPartitionMetrics(topicList)
                  .flatMap(
                      metricsMap -> {
                        // 3. Get Spark checkpoint offsets
                        return sparkCheckpointService
                            .getSparkCheckpointOffsets(tenant)
                            .flatMap(
                                checkpointOffsets -> {
                                  // 4. Get end offsets from Kafka
                                  List<TopicPartition> allPartitions = new ArrayList<>();
                                  for (TopicPartitionMetrics metrics : metricsMap.values()) {
                                    for (int partitionId = 0;
                                        partitionId < metrics.getPartitionCount();
                                        partitionId++) {
                                      allPartitions.add(
                                          new TopicPartition(metrics.getTopic(), partitionId));
                                    }
                                  }

                                  return kafkaClient
                                      .getEndOffsets(allPartitions)
                                      .flatMap(
                                          endOffsets -> {
                                            // 5. Calculate lag
                                            Single<Map<TopicPartition, Long>> lagMapSingle;
                                            if (checkpointOffsets.isAvailable()
                                                && !checkpointOffsets.getOffsets().isEmpty()) {
                                              lagMapSingle =
                                                  kafkaClient.calculateLag(
                                                      endOffsets, checkpointOffsets.getOffsets());
                                            } else {
                                              log.warn(
                                                  "Spark checkpoint offsets not available, using zero lag");
                                              Map<TopicPartition, Long> lagMap = new HashMap<>();
                                              for (TopicPartition tp : allPartitions) {
                                                lagMap.put(tp, 0L);
                                              }
                                              lagMapSingle = Single.just(lagMap);
                                            }

                                            return lagMapSingle.flatMap(
                                                lagMap -> {
                                                  // 6. Identify topics needing scaling
                                                  List<ScalingDecision> scalingDecisions =
                                                      kafkaScalingService
                                                          .identifyTopicsNeedingScaling(
                                                              metricsMap, lagMap, kafkaConfig);

                                                  if (scalingDecisions.isEmpty()) {
                                                    long duration =
                                                        System.currentTimeMillis() - startTime;
                                                    log.info(
                                                        "No topics need scaling for tenant: {} (checked in {}ms)",
                                                        tenant,
                                                        duration);
                                                    return Single.just(scalingDecisions);
                                                  }

                                                  log.info(
                                                      "Found {} topics needing scaling for tenant: {}: {}",
                                                      scalingDecisions.size(),
                                                      tenant,
                                                      scalingDecisions.stream()
                                                          .map(
                                                              d ->
                                                                  d.getTopic()
                                                                      + "("
                                                                      + d.getCurrentPartitions()
                                                                      + "->"
                                                                      + d.getNewPartitions()
                                                                      + ")")
                                                          .collect(
                                                              java.util.stream.Collectors.joining(
                                                                  ", ")));

                                                  // 7. Increase partitions
                                                  Map<String, Integer> scalingMap =
                                                      scalingDecisions.stream()
                                                          .collect(
                                                              Collectors.toMap(
                                                                  ScalingDecision::getTopic,
                                                                  ScalingDecision
                                                                      ::getNewPartitions));

                                                  return kafkaClient
                                                      .increasePartitions(scalingMap)
                                                      .doOnComplete(
                                                          () -> {
                                                            long duration =
                                                                System.currentTimeMillis()
                                                                    - startTime;
                                                            int totalPartitionsAdded =
                                                                scalingDecisions.stream()
                                                                    .mapToInt(
                                                                        d ->
                                                                            d.getNewPartitions()
                                                                                - d
                                                                                    .getCurrentPartitions())
                                                                    .sum();

                                                            log.info(
                                                                "Successfully scaled {} topics for tenant: {} in {}ms. Total partitions added: {}",
                                                                scalingDecisions.size(),
                                                                tenant,
                                                                duration,
                                                                totalPartitionsAdded);

                                                            for (ScalingDecision decision :
                                                                scalingDecisions) {
                                                              log.info(
                                                                  "Scaled topic {} from {} to {} partitions (factors: {}, reason: {})",
                                                                  decision.getTopic(),
                                                                  decision.getCurrentPartitions(),
                                                                  decision.getNewPartitions(),
                                                                  decision.getFactors(),
                                                                  decision.getReason());
                                                            }
                                                          })
                                                      .doOnError(
                                                          th -> {
                                                            long duration =
                                                                System.currentTimeMillis()
                                                                    - startTime;
                                                            log.error(
                                                                "Error increasing partitions for tenant: {} after {}ms",
                                                                tenant,
                                                                duration,
                                                                th);
                                                          })
                                                      .toSingle(() -> scalingDecisions);
                                                });
                                          });
                                });
                      });
            });
  }
}
