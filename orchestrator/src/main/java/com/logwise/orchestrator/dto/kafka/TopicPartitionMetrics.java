package com.logwise.orchestrator.dto.kafka;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopicPartitionMetrics {
  private String topic;
  private int partitionCount;
  private long totalMessages;
  private long avgMessagesPerPartition;
  private long estimatedSizeBytes;
  private Map<Integer, Long> partitionOffsets; // partition -> latest offset
  private Map<Integer, Long> partitionSizes; // partition -> size in bytes
}
