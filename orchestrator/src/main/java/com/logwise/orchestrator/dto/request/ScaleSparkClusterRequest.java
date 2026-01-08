package com.logwise.orchestrator.dto.request;

import com.logwise.orchestrator.dto.entity.SparkStageHistory;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.NonFinal;

@Data
@NoArgsConstructor
public class ScaleSparkClusterRequest {
  @NonFinal Boolean enableUpScale = true;
  @NonFinal Boolean enableDownScale = true;
  @NonFinal SparkStageHistory sparkStageHistory;
}
