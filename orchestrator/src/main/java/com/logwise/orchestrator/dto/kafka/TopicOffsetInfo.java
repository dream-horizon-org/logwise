package com.logwise.orchestrator.dto.kafka;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicOffsetInfo {
  private long sumOfEndOffsets;
  private int currentNumberOfPartitions;
}

