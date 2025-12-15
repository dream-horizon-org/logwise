package com.logwise.orchestrator.dto.entity;

import com.logwise.orchestrator.constant.ApplicationConstants;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.NonFinal;

@Data
@Builder
public class SparkScaleArgs {
  @Builder.Default boolean enableDownscale = true;
  @Builder.Default boolean enableUpscale = true;

  @Builder.Default
  int minimumDownscale = com.logwise.orchestrator.constant.ApplicationConstants.SPARK_MIN_DOWNSCALE;

  @Builder.Default int maximumDownscale = ApplicationConstants.SPARK_MAX_DOWNSCALE;
  @Builder.Default double downscaleProportion = ApplicationConstants.SPARK_DOWNSCALE_PROPORTION;
  @Builder.Default int minimumUpscale = ApplicationConstants.SPARK_MIN_UPSCALE;
  @Builder.Default int maximumUpscale = ApplicationConstants.SPARK_MAX_UPSCALE;
  @NonFinal int minWorkerCount;
  @NonFinal int maxWorkerCount;
  @NonFinal Integer workerCount;
}
