package com.logwise.orchestrator.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.NonFinal;

@Data
@NoArgsConstructor
public class UpdateSparkScaleOverrideRequest {
  @NonFinal Boolean enableUpScale;
  @NonFinal Boolean enableDownScale;
}
