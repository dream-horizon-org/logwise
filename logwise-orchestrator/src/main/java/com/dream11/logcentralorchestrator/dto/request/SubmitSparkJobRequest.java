package com.dream11.logcentralorchestrator.dto.request;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmitSparkJobRequest {
  @Builder.Default String action = "CreateSubmissionRequest";
  List<String> appArgs;
  String appResource;
  String clientSparkVersion;
  String mainClass;
  Map<String, String> environmentVariables;
  Map<String, Object> sparkProperties;
}
