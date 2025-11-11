package com.dream11.logcentralorchestrator.dto.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.NonFinal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparkSubmitStatus {
  @Builder.Default Long id = null;
  @Builder.Default Long startingOffsetsTimestamp = 0L;
  @Builder.Default Long resumeToSubscribePatternTimestamp = 0L;
  @Builder.Default Boolean isSubmittedForOffsetsTimestamp = false;
  @Builder.Default Boolean isResumedToSubscribePattern = false;
  @NonFinal String tenant;
}
