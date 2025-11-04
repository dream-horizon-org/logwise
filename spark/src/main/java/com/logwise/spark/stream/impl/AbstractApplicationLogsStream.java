package com.logwise.spark.stream.impl;

import com.google.inject.Inject;
import com.logwise.spark.constants.Constants;
import com.logwise.spark.dto.entity.KafkaReadStreamOptions;
import com.logwise.spark.dto.entity.SparkStageHistory;
import com.logwise.spark.dto.entity.StartingOffsetsByTimestampOption;
import com.logwise.spark.dto.mapper.KafkaAssignOptionMapper;
import com.logwise.spark.services.KafkaManagerService;
import com.logwise.spark.services.KafkaService;
import com.logwise.spark.services.SparkMasterService;
import com.logwise.spark.services.SparkStageHistoryService;
import com.logwise.spark.stream.Stream;
import com.logwise.spark.utils.SparkUtils;
import com.typesafe.config.Config;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public abstract class AbstractApplicationLogsStream implements Stream {
  protected final Config config;
  private final KafkaService kafkaService;
  private final SparkMasterService sparkMasterService;
  private final KafkaManagerService kafkaManagerService;
  private final SparkStageHistoryService sparkStageHistoryService;

  @Override
  public List<StreamingQuery> startStreams(SparkSession sparkSession) {
    CompletableFuture<Integer> kafkaPartitionCountFuture = getKafkaPartitionsCountFuture();
    CompletableFuture<Integer> sparkPartitionsCountFuture =
        kafkaPartitionCountFuture.thenApplyAsync(this::getSparkPartitionsCount);
    Long maxOffset = getMaxOffsetPerTrigger();
    Integer kafkaPartitionsCount = kafkaPartitionCountFuture.join();
    Integer sparkPartitionsCount = sparkPartitionsCountFuture.join();

    log.info(
        "maxOffset: {}, sparkPartitionsCount: {}, kafkaPartitionsCount: {}",
        maxOffset,
        sparkPartitionsCount,
        kafkaPartitionsCount);

    String startingOffsetsByTimestampJson = null;
    String assign = null;

    String kafkaHostname = config.getString("kafka.cluster.dns");
    // If kafka.startingOffsetsTimestamp is set, then get startingOffsetsByTimestampJson
    if (config.getLong("kafka.startingOffsetsTimestamp") != 0) {
      StartingOffsetsByTimestampOption startingOffsetsByTimestamp =
          getStartingOffsetsByTimestamp(kafkaHostname);
      startingOffsetsByTimestampJson = startingOffsetsByTimestamp.toJson();

      if (startingOffsetsByTimestampJson.equals("{}")) {
        throw new IllegalStateException("startingOffsetsByTimestampJson is empty");
      } else {
        // startingOffsetsByTimestamp only works with assign not with subscribePattern/subscribe
        assign =
            KafkaAssignOptionMapper.toKafkaAssignOption.apply(startingOffsetsByTimestamp).toJson();

        log.info("kafka assign option: {}", assign);
        // Adding additional buffer to spark partitions to avoid disk spill
        double buffer = config.getDouble("spark.timestampOffset.partition.buffer.fraction");
        sparkPartitionsCount =
            sparkPartitionsCount == null
                ? null
                : (int) Math.ceil(sparkPartitionsCount * (1 + buffer));
      }
    }

    boolean doDynamicPartitioning = sparkPartitionsCount != null && kafkaPartitionsCount != null;
    boolean coalesceRequired = doDynamicPartitioning && sparkPartitionsCount < kafkaPartitionsCount;
    boolean minPartitionsRequired =
        doDynamicPartitioning && sparkPartitionsCount >= kafkaPartitionsCount;

    KafkaReadStreamOptions appKafkaReadStreamOptions =
        KafkaReadStreamOptions.builder()
            .failOnDataLoss("false")
            .maxOffsetsPerTrigger(String.valueOf(maxOffset))
            .startingOffsets(config.getString("kafka.startingOffsets"))
            .startingOffsetsByTimestamp(startingOffsetsByTimestampJson)
            .assign(assign)
            .subscribePattern(
                assign == null ? config.getString("kafka.topic.prefix.application") : null)
            .kafkaBootstrapServers(kafkaService.getKafkaBootstrapServerIp(kafkaHostname))
            .maxRatePerPartition(config.getString("kafka.maxRatePerPartition"))
            .minPartitions(minPartitionsRequired ? String.valueOf(sparkPartitionsCount) : null)
            .groupIdPrefix(Constants.APPLICATION_LOGS_KAFKA_GROUP_ID)
            .build();

    Dataset<Row> appKafkaReadStreamDataset =
        SparkUtils.getKafkaReadStream(sparkSession, appKafkaReadStreamOptions);

    if (coalesceRequired) {
      log.info("Coalescing to {} partitions", sparkPartitionsCount);
      appKafkaReadStreamDataset = appKafkaReadStreamDataset.coalesce(sparkPartitionsCount);
    }
    Dataset<Row> appLogsStream = appKafkaReadStreamDataset.selectExpr("value");

    StreamingQuery appLogsStreamingQuery = getVectorApplicationLogsStreamQuery(appLogsStream);
    List<StreamingQuery> streamingQueries = new ArrayList<>();
    streamingQueries.add(appLogsStreamingQuery);
    return streamingQueries;
  }

  private StartingOffsetsByTimestampOption getStartingOffsetsByTimestamp(String kafkaHostname) {
    String topicRegexPattern = config.getString("kafka.topic.prefix.application");
    Long timestamp = config.getLong("kafka.startingOffsetsTimestamp");
    return kafkaService.getStartingOffsetsByTimestamp(kafkaHostname, topicRegexPattern, timestamp);
  }

  private Long getMaxOffsetPerTrigger() {
    try {
      Integer coreUsed = sparkMasterService.getCoresUsed();
      setCurrentSparkStageHistory(coreUsed);
      return getMaxOffset(coreUsed);
    } catch (Exception e) {
      log.error("Error in fetching max offset, so selecting default maxOffset: ", e);
      return config.getLong("spark.offsetPerTrigger.default");
    }
  }

  private Long getMaxOffset(Integer availableCore) {
    long maxOffset = config.getLong("spark.offsetPerTrigger.default");

    if (availableCore != null) {
      try {
        long rawMaxOffset =
            availableCore * config.getLong("spark.eventProcessPerCore.count")
                + (config.getLong("spark.offsetPerTrigger.buffer"));
        maxOffset =
            Math.min(
                config.getLong("spark.offsetPerTrigger.max"),
                Math.max(config.getLong("spark.offsetPerTrigger.min"), rawMaxOffset));
      } catch (Exception e) {
        log.error("Error in fetching max offset: ", e);
      }
    }
    log.info("Max Offset: " + maxOffset);
    return maxOffset;
  }

  private void setCurrentSparkStageHistory(Integer coreUsed) {
    SparkStageHistory sparkStageHistory = new SparkStageHistory();
    sparkStageHistory.setCoresUsed(coreUsed);
    sparkStageHistory.setTenant(config.getString("tenant.name"));
    sparkStageHistoryService.setCurrentSparkStageHistory(sparkStageHistory);
  }

  private CompletableFuture<Integer> getKafkaPartitionsCountFuture() {
    return CompletableFuture.supplyAsync(
            () -> kafkaManagerService.getTopicIdentities(config.getString("kafka.cluster.name")))
        .thenApplyAsync(
            response -> {
              if (response == null) {
                log.error("topicIdentitiesResponse is null");
                return null;
              }
              try {
                return kafkaManagerService.getActiveLogsTopicPartitionCount(
                    response, config.getString("kafka.topic.prefix.application"));
              } catch (Exception e) {
                log.error("Error in fetching total kafka partitions: ", e);
                return null;
              }
            });
  }

  private Integer getSparkPartitionsCount(Integer totalKafkaPartitions) {
    try {
      return sparkStageHistoryService.getStageHistoryList().stream()
          .findFirst()
          .map(
              sparkStageHistory ->
                  getSparkPartitionsCount(totalKafkaPartitions, sparkStageHistory.getOutputBytes()))
          .orElse(null);
    } catch (Exception e) {
      log.error("Error in fetching spark partitions: ", e);
      return null;
    }
  }

  private Integer getSparkPartitionsCount(Integer totalKafkaPartitions, Long lastOutputBytes) {
    Integer partitions = null;
    if (totalKafkaPartitions != null && lastOutputBytes != null && totalKafkaPartitions > 0) {
      double outputGigaBytes = (double) lastOutputBytes / (1024L * 1024L * 1024L);
      if (outputGigaBytes > 0) {
        Map.Entry<Double, Double> entry =
            getOutputGigaBytesToKafkaPartitionMultiplierMap().floorEntry(outputGigaBytes);
        double multiplier = entry.getValue();
        log.info("Output GigaBytes: {}, Multiplier: {}", outputGigaBytes, multiplier);
        partitions = (int) Math.ceil(totalKafkaPartitions * multiplier);
      }
    }
    log.info("Spark Partitions Count: {}", partitions);
    return partitions;
  }

  private NavigableMap<Double, Double> getOutputGigaBytesToKafkaPartitionMultiplierMap() {
    NavigableMap<Double, Double> outputGigaBytesToMultiplierMap = new TreeMap<>();
    config
        .getObject("spark.outputGbToKafkaPartitionMultiplier")
        .forEach(
            (key, value) ->
                outputGigaBytesToMultiplierMap.put(
                    Double.valueOf(key), Double.valueOf(value.unwrapped().toString())));
    log.info("Output GigaBytes To Multiplier Map: " + outputGigaBytesToMultiplierMap);
    return outputGigaBytesToMultiplierMap;
  }

  protected abstract StreamingQuery getVectorApplicationLogsStreamQuery(
      Dataset<Row> kafkaValueTopicStream);
}
