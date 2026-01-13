package com.logwise.orchestrator.dto.response;

import com.logwise.orchestrator.dto.kafka.ScalingDecision;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ScaleKafkaPartitionsResponse {
  private boolean success;
  private String message;
  private int topicsScaled;
  private List<ScalingDecision> scalingDecisions;
  private List<String> warnings;
  private List<String> errors;
}
