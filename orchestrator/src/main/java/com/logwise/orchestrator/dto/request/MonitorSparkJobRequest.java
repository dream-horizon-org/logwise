package com.logwise.orchestrator.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.NonFinal;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonitorSparkJobRequest {
  @NonFinal Integer driverCores = null;
  @NonFinal Integer driverMemoryInGb = null;
}
