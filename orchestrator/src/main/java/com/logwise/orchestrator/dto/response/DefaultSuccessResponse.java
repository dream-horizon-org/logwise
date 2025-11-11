package com.logwise.orchestrator.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DefaultSuccessResponse {
  @Builder.Default boolean success = true;
  @Builder.Default String message = "Success";
}
