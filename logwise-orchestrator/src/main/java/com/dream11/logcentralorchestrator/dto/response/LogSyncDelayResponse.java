package com.dream11.logcentralorchestrator.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogSyncDelayResponse {
  @Builder.Default private String tenant = null;
  @Builder.Default private Integer appLogsDelayMinutes = null;
}
