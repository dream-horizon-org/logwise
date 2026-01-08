package com.logwise.orchestrator.dto.kafka;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScalingDecision {
  private String topic;
  private int currentPartitions;
  private int newPartitions;
  private String reason;
  private List<String> factors; // e.g., "lag", "size", "messages"
  private long lagPerPartition;
  private long sizePerPartition;
  private long messagesPerPartition;
}
