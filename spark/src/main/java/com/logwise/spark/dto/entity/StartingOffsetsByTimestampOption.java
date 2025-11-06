package com.logwise.spark.dto.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;

public class StartingOffsetsByTimestampOption {
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private Map<String, Map<String, Long>> offsetByTimestamp;

  public StartingOffsetsByTimestampOption() {
    this.offsetByTimestamp = new HashMap<>();
  }

  public Map<String, Map<String, Long>> getOffsetByTimestamp() {
    return offsetByTimestamp;
  }

  public void addTopic(String topicName, Map<String, Long> partitions) {
    if (topicName == null || topicName.isEmpty()) {
      throw new IllegalArgumentException("Topic name cannot be null or empty");
    }
    this.offsetByTimestamp.put(topicName, (partitions != null) ? partitions : new HashMap<>());
  }

  public void addPartition(String topicName, String partition, Long value) {
    if (topicName == null || topicName.isEmpty()) {
      throw new IllegalArgumentException("Topic name cannot be null or empty");
    }
    this.offsetByTimestamp.computeIfAbsent(topicName, k -> new HashMap<>()).put(partition, value);
  }

  @SneakyThrows
  public String toJson() {
    return objectMapper.writeValueAsString(this.offsetByTimestamp);
  }

  @Override
  public String toString() {
    return "StartingOffsetsByTimestampOption{" + "offsetByTimestamp=" + offsetByTimestamp + '}';
  }
}
