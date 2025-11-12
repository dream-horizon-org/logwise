package com.logwise.spark.stream.impl;

import com.google.inject.Inject;
import com.logwise.spark.constants.Constants;
import com.logwise.spark.dto.entity.KafkaReadStreamOptions;
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

    String kafkaHostname = config.getString("kafka.cluster.dns");

    KafkaReadStreamOptions appKafkaReadStreamOptions =
        KafkaReadStreamOptions.builder()
            .failOnDataLoss("false")
            .maxOffsetsPerTrigger(String.valueOf(maxOffset))
            .startingOffsets(config.getString("kafka.startingOffsets"))
            .subscribePattern(config.getString("kafka.topic.prefix.application"))
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

  private Long getMaxOffsetPerTrigger() {
    return config.getLong("spark.offsetPerTrigger.default");
  }

  protected abstract StreamingQuery getVectorApplicationLogsStreamQuery(
      Dataset<Row> kafkaValueTopicStream);
}
