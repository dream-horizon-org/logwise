package com.logwise.spark.dto.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;

public class KafkaAssignOption {
  private Map<String, List<Integer>> assign;
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public KafkaAssignOption() {
    this.assign = new HashMap<>();
  }

  public Map<String, List<Integer>> getAssign() {
    return assign;
  }

  public void addTopic(String topicName, List<Integer> partitions) {
    if (topicName == null || topicName.isEmpty()) {
      throw new IllegalArgumentException("Topic name cannot be null or empty");
    }
    this.assign.put(topicName, (partitions != null) ? partitions : new ArrayList<>());
  }

  public void addPartition(String topicName, Integer partition) {
    if (topicName == null || topicName.isEmpty()) {
      throw new IllegalArgumentException("Topic name cannot be null or empty");
    }
    this.assign.computeIfAbsent(topicName, k -> new ArrayList<>()).add(partition);
  }

  @SneakyThrows
  public String toJson() {
    return objectMapper.writeValueAsString(this.assign);
  }

  @Override
  public String toString() {
    return "KafkaAssignOption{" + "assign=" + assign + '}';
  }
}
