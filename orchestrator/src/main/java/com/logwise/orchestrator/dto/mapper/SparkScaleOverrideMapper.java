package com.logwise.orchestrator.dto.mapper;

import com.logwise.orchestrator.dto.entity.SparkScaleOverride;
import com.logwise.orchestrator.dto.request.UpdateSparkScaleOverrideRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SparkScaleOverrideMapper {
  public SparkScaleOverride toSparkScaleOverride(
      String tenant, UpdateSparkScaleOverrideRequest request) {
    return SparkScaleOverride.builder()
        .tenant(tenant)
        .downscale(request.getEnableDownScale())
        .upscale(request.getEnableUpScale())
        .build();
  }
}
