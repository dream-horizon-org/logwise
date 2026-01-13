package com.logwise.orchestrator.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScaleKafkaPartitionsRequest {
  // Optional override parameters can be added here in the future
  // For now, scaling uses configuration from tenant config
}
