package com.dream11.logcentralorchestrator.dto.entity;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.NonFinal;

@Data
@AllArgsConstructor
public class SparkSubmitArgs {
  @NonFinal @NotNull Long timeStamp;
  @NonFinal @NotNull Boolean cleanStateRequired;
  @NonFinal @NotNull Boolean submitJobRequired;
  @NonFinal Long sparkSubmitStatusId;
  @NonFinal Boolean submittedForOffsetsTimestamp;
  @NonFinal Boolean resumedToSubscribePattern;
}
