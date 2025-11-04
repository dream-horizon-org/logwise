package com.logwise.spark.services;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.logwise.spark.constants.Constants;
import com.logwise.spark.dto.entity.StartingOffsetsByTimestampOption;
import com.logwise.spark.utils.ApplicationUtils;
import com.typesafe.config.Config;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class KafkaService {
  private final Config config;

  private static StartingOffsetsByTimestampOption getStartingOffsetsByTimestamp(
      long timestamp, String topicRegexPattern, String bootstrapServers) {

    Map<String, Object> configs =
        ImmutableMap.of(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            bootstrapServers,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            Constants.KEY_DESERIALIZER_CLASS_CONFIG_VALUE,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            Constants.VALUE_DESERIALIZER_CLASS_CONFIG_VALUE,
            ConsumerConfig.GROUP_ID_CONFIG,
            Constants.GROUP_ID_CONFIG_VALUE,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            Constants.AUTO_OFFSET_RESET_CONFIG_VALUE);

    Pattern pattern = Pattern.compile(topicRegexPattern);
    StartingOffsetsByTimestampOption option = new StartingOffsetsByTimestampOption();
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(configs)) {
      Map<String, List<PartitionInfo>> topicsMetadata =
          consumer.listTopics(Constants.KAFKA_CONSUMER_TIMEOUT);
      List<TopicPartition> partitions =
          topicsMetadata.entrySet().parallelStream()
              .filter(entry -> pattern.matcher(entry.getKey()).find())
              .flatMap(
                  entry ->
                      entry.getValue().parallelStream()
                          .map(p -> new TopicPartition(entry.getKey(), p.partition())))
              .collect(Collectors.toList());
      Map<TopicPartition, Long> timestampQuery = new ConcurrentHashMap<>();

      partitions.parallelStream().forEach(tp -> timestampQuery.put(tp, timestamp));

      Map<TopicPartition, org.apache.kafka.clients.consumer.OffsetAndTimestamp> offsetsForTimes =
          consumer.offsetsForTimes(timestampQuery);

      List<TopicPartition> availablePartitions =
          offsetsForTimes.entrySet().parallelStream()
              .filter(entry -> entry.getValue() != null)
              .map(Map.Entry::getKey)
              .collect(Collectors.toList());

      availablePartitions.forEach(
          tp -> option.addPartition(tp.topic(), String.valueOf(tp.partition()), timestamp));
    }

    log.info("StartingOffsetsByTimestampOption Fetched Successfully");
    return option;
  }

  public StartingOffsetsByTimestampOption getStartingOffsetsByTimestamp(
      String kafkaHostname, String topicRegexPattern, Long timestamp) {
    String bootstrapServers = getKafkaBootstrapServerIp(kafkaHostname);
    return getStartingOffsetsByTimestamp(timestamp, topicRegexPattern, bootstrapServers);
  }

  public String getKafkaBootstrapServerIp(String kafkaHostname) {
    List<String> ipAddressWithPort =
        ApplicationUtils.getIpAddresses(kafkaHostname).stream()
            .map(ip -> String.format("%s:%s", ip, config.getString("kafka.bootstrap.servers.port")))
            .collect(Collectors.toList());
    String kafkaBootstrapServerIp = String.join(",", ipAddressWithPort);
    log.info("Kafka Bootstrap Server IP: " + kafkaBootstrapServerIp);
    return kafkaBootstrapServerIp;
  }
}
