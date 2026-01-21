package com.logwise.spark.dto.request;

import com.logwise.spark.dto.entity.SparkStageHistory;
import lombok.Data;

@Data
public class ScaleSparkClusterRequest {
  private Boolean enableUpScale;
  private Boolean enableDownScale;
  private SparkStageHistory sparkStageHistory;
}
