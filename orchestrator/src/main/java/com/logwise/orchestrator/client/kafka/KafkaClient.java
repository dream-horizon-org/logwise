package com.logwise.orchestrator.client.kafka;

import com.logwise.orchestrator.dto.kafka.TopicOffsetInfo;
import com.logwise.orchestrator.enums.KafkaType;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.common.TopicPartition;

/**
 * Generic interface for Kafka operations across different Kafka implementations. This interface
 * abstracts the differences between EC2, MSK, and Confluent Kafka.
 */
public interface KafkaClient {

  /** Get the type of Kafka this client supports */
  KafkaType getKafkaType();

  /**
   * Create AdminClient with appropriate configuration for this Kafka type. The AdminClient should
   * be closed by the caller.
   */
  Single<AdminClient> createAdminClient();

  /**
   * Get all topics matching a pattern (regex).
   *
   * @param pattern Regex pattern to match topic names
   * @return Set of topic names
   */
  Single<Set<String>> listTopics(String pattern);

  /**
   * Get end offset sum and current partition count for topics.
   *
   * @param topics List of topic names
   * @return Map of topic name to TopicOffsetInfo containing sumOfEndOffsets and
   *     currentNumberOfPartitions
   */
  Single<Map<String, TopicOffsetInfo>> getEndOffsetSum(List<String> topics);

  /**
   * Get end offsets (high watermarks) for partitions. Used to calculate lag when combined with
   * Spark checkpoint offsets.
   *
   * @param topicPartitions List of topic partitions
   * @return Map of TopicPartition to latest offset
   */
  Single<Map<TopicPartition, Long>> getEndOffsets(List<TopicPartition> topicPartitions);

  /**
   * Increase partitions for topics.
   *
   * @param topicPartitionsMap Map of topic name to new partition count
   * @return Completable that completes when partitions are increased
   */
  Completable increasePartitions(Map<String, Integer> topicPartitionsMap);

  /** Close the client and release resources. Should be called when done using the client. */
  void close();
}
