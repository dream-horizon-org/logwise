package com.logwise.orchestrator.dto.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.NonFinal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SparkScaleOverride {
  @NonFinal @Builder.Default Boolean upscale = null;
  @NonFinal @Builder.Default Boolean downscale = null;
  @NonFinal String tenant;
}
