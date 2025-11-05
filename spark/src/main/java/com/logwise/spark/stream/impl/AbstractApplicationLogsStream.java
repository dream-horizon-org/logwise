package com.logwise.spark.stream.impl;

import com.google.inject.Inject;
import com.logwise.spark.constants.Constants;
import com.logwise.spark.dto.entity.KafkaReadStreamOptions;
import com.logwise.spark.dto.entity.StartingOffsetsByTimestampOption;
import com.logwise.spark.dto.mapper.KafkaAssignOptionMapper;
import com.logwise.spark.services.KafkaService;
import com.logwise.spark.stream.Stream;
import com.logwise.spark.utils.SparkUtils;
import com.typesafe.config.Config;
import java.util.*;
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

  @Override
  public List<StreamingQuery> startStreams(SparkSession sparkSession) {
    Long maxOffset = getMaxOffsetPerTrigger();

    log.info("maxOffset: {}", maxOffset);

    String startingOffsetsByTimestampJson = null;
    String assign = null;

    String kafkaHostname = config.getString("kafka.cluster.dns");
    // If kafka.startingOffsetsTimestamp is set, then get
    // startingOffsetsByTimestampJson
    if (config.getLong("kafka.startingOffsetsTimestamp") != 0) {
      StartingOffsetsByTimestampOption startingOffsetsByTimestamp =
          getStartingOffsetsByTimestamp(kafkaHostname);
      startingOffsetsByTimestampJson = startingOffsetsByTimestamp.toJson();

      if (startingOffsetsByTimestampJson.equals("{}")) {
        throw new IllegalStateException("startingOffsetsByTimestampJson is empty");
      } else {
        // startingOffsetsByTimestamp only works with assign not with
        // subscribePattern/subscribe
        assign =
            KafkaAssignOptionMapper.toKafkaAssignOption.apply(startingOffsetsByTimestamp).toJson();

        log.info("kafka assign option: {}", assign);
      }
    }

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
            .groupIdPrefix(Constants.APPLICATION_LOGS_KAFKA_GROUP_ID)
            .build();

    Dataset<Row> appKafkaReadStreamDataset =
        SparkUtils.getKafkaReadStream(sparkSession, appKafkaReadStreamOptions);

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
    return config.getLong("spark.offsetPerTrigger.default");
  }

  protected abstract StreamingQuery getVectorApplicationLogsStreamQuery(
      Dataset<Row> kafkaValueTopicStream);
}
