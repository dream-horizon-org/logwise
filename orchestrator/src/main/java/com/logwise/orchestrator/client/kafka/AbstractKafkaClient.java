package com.logwise.orchestrator.client.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.orchestrator.config.ApplicationConfig.KafkaConfig;
import com.logwise.orchestrator.dto.kafka.TopicOffsetInfo;
import com.logwise.orchestrator.dto.kafka.TopicPartitionMetrics;
import com.logwise.orchestrator.enums.KafkaType;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;

/**
 * Abstract base class for Kafka client implementations. Provides common functionality that works
 * for all Kafka types. Subclasses only need to implement authentication and connection specifics.
 */
@Slf4j
public abstract class AbstractKafkaClient implements KafkaClient {

  protected final KafkaConfig kafkaConfig;
  protected AdminClient adminClient;
  protected final ObjectMapper objectMapper = new ObjectMapper();

  protected AbstractKafkaClient(KafkaConfig kafkaConfig) {
    this.kafkaConfig = kafkaConfig;
  }

  @Override
  public abstract KafkaType getKafkaType();

  /**
   * Build bootstrap servers string - override for different formats. EC2: Resolve hostname to IPs
   * MSK: Use provided bootstrap servers or fetch from API Confluent: Use provided bootstrap servers
   */
  protected abstract Single<String> buildBootstrapServers();

  /**
   * Build AdminClient configuration - override for auth, SSL, etc. EC2: Basic config MSK: IAM
   * authentication Confluent: API key authentication
   */
  protected abstract Single<Map<String, Object>> buildAdminClientConfig();

  @Override
  public Single<AdminClient> createAdminClient() {
    if (adminClient != null) {
      return Single.just(adminClient);
    }

    return buildAdminClientConfig()
        .map(
            config -> {
              adminClient = AdminClient.create(config);
              log.info("Created AdminClient for Kafka type: {}", getKafkaType());
              return adminClient;
            });
  }

  @Override
  public Single<Set<String>> listTopics(String pattern) {
    return createAdminClient()
        .flatMap(
            adminClient -> {
              try {
                Set<String> allTopics = adminClient.listTopics().names().get();
                if (pattern == null || pattern.isEmpty()) {
                  return Single.just(allTopics);
                }

                Pattern topicPattern = Pattern.compile(pattern);
                Set<String> matchingTopics =
                    allTopics.stream()
                        .filter(topic -> topicPattern.matcher(topic).matches())
                        .collect(Collectors.toSet());

                log.info("Found {} topics matching pattern: {}", matchingTopics.size(), pattern);
                return Single.just(matchingTopics);
              } catch (InterruptedException | ExecutionException e) {
                log.error("Error listing topics", e);
                return Single.error(e);
              }
            });
  }

  @Override
  public Single<Map<String, TopicPartitionMetrics>> getPartitionMetrics(List<String> topics) {
    return createAdminClient()
        .flatMap(
            adminClient -> {
              try {
                // Get topic descriptions
                DescribeTopicsResult topicsResult = adminClient.describeTopics(topics);
                Map<String, TopicDescription> topicDescriptions = topicsResult.all().get();

                // Get all partitions
                List<TopicPartition> allPartitions = new ArrayList<>();
                for (String topic : topics) {
                  TopicDescription desc = topicDescriptions.get(topic);
                  if (desc != null) {
                    for (TopicPartitionInfo partitionInfo : desc.partitions()) {
                      allPartitions.add(new TopicPartition(topic, partitionInfo.partition()));
                    }
                  }
                }

                // Get end offsets for all partitions
                return getEndOffsets(allPartitions)
                    .map(
                        endOffsets -> {
                          Map<String, TopicPartitionMetrics> metricsMap = new HashMap<>();

                          for (String topic : topics) {
                            TopicDescription desc = topicDescriptions.get(topic);
                            if (desc == null) continue;

                            int partitionCount = desc.partitions().size();
                            Map<Integer, Long> partitionOffsets = new HashMap<>();
                            long totalMessages = 0;

                            for (TopicPartitionInfo partitionInfo : desc.partitions()) {
                              int partitionId = partitionInfo.partition();
                              TopicPartition tp = new TopicPartition(topic, partitionId);
                              Long offset = endOffsets.get(tp);
                              if (offset != null) {
                                partitionOffsets.put(partitionId, offset);
                                totalMessages += offset;
                              }
                            }

                            long avgMessagesPerPartition =
                                partitionCount > 0 ? totalMessages / partitionCount : 0;

                            // Estimate size (rough calculation: assume 1KB per message)
                            long estimatedSizeBytes = totalMessages * 1024;

                            TopicPartitionMetrics metrics =
                                TopicPartitionMetrics.builder()
                                    .topic(topic)
                                    .partitionCount(partitionCount)
                                    .totalMessages(totalMessages)
                                    .avgMessagesPerPartition(avgMessagesPerPartition)
                                    .estimatedSizeBytes(estimatedSizeBytes)
                                    .partitionOffsets(partitionOffsets)
                                    .build();

                            metricsMap.put(topic, metrics);
                          }

                          return metricsMap;
                        });
              } catch (InterruptedException | ExecutionException e) {
                log.error("Error getting partition metrics", e);
                return Single.error(e);
              }
            });
  }

  @Override
  public Single<Map<String, TopicOffsetInfo>> getEndOffsetSum(List<String> topics) {
    return createAdminClient()
        .flatMap(
            adminClient -> {
              try {
                // Get topic descriptions
                DescribeTopicsResult topicsResult = adminClient.describeTopics(topics);
                Map<String, TopicDescription> topicDescriptions = topicsResult.all().get();

                // Process each topic separately
                return Observable.fromIterable(topics)
                    .flatMapSingle(
                        topic -> {
                          TopicDescription desc = topicDescriptions.get(topic);

                          // If topic doesn't exist, return default values
                          if (desc == null) {
                            TopicOffsetInfo defaultInfo =
                                TopicOffsetInfo.builder()
                                    .sumOfEndOffsets(0L)
                                    .currentNumberOfPartitions(0)
                                    .build();
                            return Single.just(new AbstractMap.SimpleEntry<>(topic, defaultInfo));
                          }

                          // Get current number of partitions
                          int partitionCount = desc.partitions().size();

                          // Get partitions for this topic
                          List<TopicPartition> topicPartitions = new ArrayList<>();
                          for (TopicPartitionInfo partitionInfo : desc.partitions()) {
                            topicPartitions.add(
                                new TopicPartition(topic, partitionInfo.partition()));
                          }

                          // Get end offsets for this topic's partitions
                          return getEndOffsets(topicPartitions)
                              .map(
                                  endOffsets -> {
                                    // Sum offsets for this topic
                                    long topicSum = 0L;
                                    for (Long offset : endOffsets.values()) {
                                      if (offset != null) {
                                        topicSum += offset;
                                      }
                                    }
                                    TopicOffsetInfo offsetInfo =
                                        TopicOffsetInfo.builder()
                                            .sumOfEndOffsets(topicSum)
                                            .currentNumberOfPartitions(partitionCount)
                                            .build();
                                    return new AbstractMap.SimpleEntry<>(topic, offsetInfo);
                                  });
                        })
                    .toList()
                    .map(
                        entries -> {
                          Map<String, TopicOffsetInfo> result = new HashMap<>();
                          for (AbstractMap.SimpleEntry<String, TopicOffsetInfo> entry : entries) {
                            result.put(entry.getKey(), entry.getValue());
                          }
                          return result;
                        });
              } catch (InterruptedException | ExecutionException e) {
                log.error("Error getting end offset sum", e);
                return Single.error(e);
              }
            });
  }

  @Override
  public Single<Map<TopicPartition, Long>> getEndOffsets(List<TopicPartition> topicPartitions) {
    return createAdminClient()
        .flatMap(
            adminClient -> {
              try {
                // Group by topic for efficient querying
                Map<String, List<TopicPartition>> partitionsByTopic =
                    topicPartitions.stream().collect(Collectors.groupingBy(TopicPartition::topic));

                Map<TopicPartition, Long> endOffsets = new HashMap<>();

                for (Map.Entry<String, List<TopicPartition>> entry : partitionsByTopic.entrySet()) {
                  String topic = entry.getKey();
                  List<TopicPartition> partitions = entry.getValue();

                  // Build offset spec map
                  Map<TopicPartition, OffsetSpec> offsetSpecMap =
                      partitions.stream()
                          .collect(Collectors.toMap(tp -> tp, tp -> OffsetSpec.latest()));

                  // Get end offsets
                  ListOffsetsResult offsetsResult = adminClient.listOffsets(offsetSpecMap);

                  for (TopicPartition partition : partitions) {
                    try {
                      Long offset = offsetsResult.partitionResult(partition).get().offset();
                      endOffsets.put(partition, offset);
                    } catch (Exception e) {
                      log.error("Error getting offset for partition {}", partition, e);
                    }
                  }
                }

                return Single.just(endOffsets);
              } catch (Exception e) {
                log.error("Error getting end offsets", e);
                return Single.error(e);
              }
            });
  }

  @Override
  public Completable increasePartitions(Map<String, Integer> topicPartitionsMap) {
    return createAdminClient()
        .flatMapCompletable(
            adminClient -> {
              try {
                log.info("Increasing partitions for topics: {}", topicPartitionsMap);

                Map<String, NewPartitions> newPartitionsMap =
                    topicPartitionsMap.entrySet().stream()
                        .filter(entry -> entry.getValue() != null && entry.getValue() > 1)
                        .collect(
                            Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> NewPartitions.increaseTo(entry.getValue())));

                if (newPartitionsMap.isEmpty()) {
                  log.info("No partitions to increase");
                  return Completable.complete();
                }

                adminClient.createPartitions(newPartitionsMap).all().get();
                log.info("Successfully increased partitions");
                return Completable.complete();
              } catch (InterruptedException | ExecutionException e) {
                log.error("Error increasing partitions", e);
                return Completable.error(e);
              }
            });
  }

  @Override
  public Completable deleteTopics(List<String> topics) {
    return createAdminClient()
        .flatMapCompletable(
            adminClient -> {
              try {
                log.info("Deleting topics: {}", topics);
                adminClient.deleteTopics(topics).all().get();
                log.info("Successfully deleted topics");
                return Completable.complete();
              } catch (InterruptedException | ExecutionException e) {
                log.error("Error deleting topics", e);
                return Completable.error(e);
              }
            });
  }

  @Override
  public void close() {
    if (adminClient != null) {
      adminClient.close();
      adminClient = null;
      log.info("Closed AdminClient for Kafka type: {}", getKafkaType());
    }
  }
}
