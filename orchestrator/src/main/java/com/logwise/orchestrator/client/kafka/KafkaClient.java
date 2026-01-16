package com.logwise.orchestrator.client.kafka;

import com.logwise.orchestrator.dto.kafka.ClusterInfo;
import com.logwise.orchestrator.dto.kafka.SparkCheckpointOffsets;
import com.logwise.orchestrator.dto.kafka.TopicPartitionMetrics;
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
   * Get partition metrics for topics (offsets, sizes, etc.). This replaces the need for Kafka
   * Manager producer rate.
   *
   * @param topics List of topic names
   * @return Map of topic name to metrics
   */
  Single<Map<String, TopicPartitionMetrics>> getPartitionMetrics(List<String> topics);

  /**
   * Get end offsets (high watermarks) for partitions. Used to calculate lag when combined with
   * Spark checkpoint offsets.
   *
   * @param topicPartitions List of topic partitions
   * @return Map of TopicPartition to latest offset
   */
  Single<Map<TopicPartition, Long>> getEndOffsets(List<TopicPartition> topicPartitions);

  /**
   * Get Spark checkpoint offsets from S3. Parses Spark Structured Streaming checkpoint files to
   * extract partition offsets.
   *
   * @param checkpointPath S3 path to Spark checkpoint directory
   * @return SparkCheckpointOffsets with partition offsets
   */
  Single<SparkCheckpointOffsets> getSparkCheckpointOffsets(String checkpointPath);

  /**
   * Calculate lag between end offsets and Spark checkpoint offsets.
   *
   * @param endOffsets Map of TopicPartition to latest offset
   * @param checkpointOffsets Map of TopicPartition to checkpoint offset
   * @return Map of TopicPartition to lag (endOffset - checkpointOffset)
   */
  Single<Map<TopicPartition, Long>> calculateLag(
      Map<TopicPartition, Long> endOffsets, Map<TopicPartition, Long> checkpointOffsets);

  /**
   * Increase partitions for topics.
   *
   * @param topicPartitionsMap Map of topic name to new partition count
   * @return Completable that completes when partitions are increased
   */
  Completable increasePartitions(Map<String, Integer> topicPartitionsMap);

  /**
   * Delete topics.
   *
   * @param topics List of topic names to delete
   * @return Completable that completes when topics are deleted
   */
  Completable deleteTopics(List<String> topics);

  /**
   * Get default partition count from broker config. Used for rounding partition counts.
   *
   * @return Default partition count
   */
  Single<Integer> getDefaultPartitions();

  /**
   * Get cluster information (brokers, cluster ID, etc.).
   *
   * @return ClusterInfo object
   */
  Single<ClusterInfo> getClusterInfo();

  /** Close the client and release resources. Should be called when done using the client. */
  void close();
}
