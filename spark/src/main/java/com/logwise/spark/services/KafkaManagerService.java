package com.logwise.spark.services;

import com.logwise.spark.clients.KafkaManagerClient;
import com.logwise.spark.dto.response.TopicIdentitiesResponse;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class KafkaManagerService {
    private final KafkaManagerClient kafkaManagerClient;
    private final Config config;

    public TopicIdentitiesResponse getTopicIdentities(String clusterName) {
        log.info("Getting topicIdentities response for cluster: {}", clusterName);
        try {
            return kafkaManagerClient.topicIdentities(clusterName);
        } catch (Exception e) {
            log.error("Error in getTopicIdentities: ", e);
        }
        return null;
    }

    public Integer getActiveLogsTopicPartitionCount(
            TopicIdentitiesResponse topicIdentitiesResponse, String topicRegexPattern) {
        try {
            final Pattern pattern = Pattern.compile(topicRegexPattern);
            final AtomicInteger totalPartitions = new AtomicInteger(0);

            int activeKafkaPartitions =
                    topicIdentitiesResponse.getTopicIdentities().stream()
                            .filter(i -> pattern.matcher(i.getTopic()).find())
                            .peek(i -> totalPartitions.addAndGet(i.getPartitions()))
                            .mapToInt(i -> Double.parseDouble(i.getProducerRate()) > 0 ? i.getPartitions() : 0)
                            .sum();

            // If producer rate is 0 for all topics (intermittent issue in CMAK), then we need to consider
            // some ratio of all the partitions
            if (activeKafkaPartitions == 0) {
                float activeTopicsRatio = (float) config.getDouble("kafka.activeTopicsRatio");
                activeKafkaPartitions = Math.round(totalPartitions.get() * activeTopicsRatio);
                log.info(
                        "Producer rate for all partition is 0. Considering {} of {} partitions as active partitions: {}",
                        activeTopicsRatio,
                        totalPartitions.get(),
                        activeKafkaPartitions);
            }

            log.info(
                    "Active Partition for topicRegexPattern: {} is {}",
                    topicRegexPattern,
                    activeKafkaPartitions);
            return activeKafkaPartitions;
        } catch (Exception e) {
            log.error("Error in getActiveLogsTopicPartitionCount", e);
        }
        return null;
    }
}