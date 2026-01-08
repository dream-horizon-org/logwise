package com.logwise.orchestrator.dto.kafka;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.apache.kafka.common.TopicPartition;

@Data
@Builder
public class SparkCheckpointOffsets {
  private String checkpointPath;
  private Map<TopicPartition, Long> offsets; // TopicPartition -> checkpoint offset
  private long lastUpdatedTimestamp;
  private boolean available;
}
