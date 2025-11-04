package com.logwise.spark.dto.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class KafkaReadStreamOptions {
  @JsonProperty("kafka.bootstrap.servers")
  @NonNull
  String kafkaBootstrapServers;

  @NonNull String failOnDataLoss;
  @NonNull String maxOffsetsPerTrigger;
  @NonNull String startingOffsets;
  @NonNull String maxRatePerPartition;
  @NonNull String groupIdPrefix;

  String subscribePattern;
  String assign;
  String startingOffsetsByTimestamp;
  String minPartitions;
}
