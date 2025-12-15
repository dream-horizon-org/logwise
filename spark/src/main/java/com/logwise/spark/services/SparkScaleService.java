package com.logwise.spark.services;

import com.google.inject.Inject;
import com.logwise.spark.clients.LogCentralOrchestratorClient;
import com.logwise.spark.constants.Constants;
import com.logwise.spark.dto.entity.SparkStageHistory;
import com.logwise.spark.dto.request.ScaleSparkClusterRequest;
import com.typesafe.config.Config;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class SparkScaleService {

  @NonFinal private static SparkStageHistory currentSparkStageHistory = null;
  private final Config config;
  private final LogCentralOrchestratorClient logCentralOrchestratorClient;

  public SparkStageHistory getCurrentSparkStageHistory() {
    return currentSparkStageHistory;
  }

  public void setCurrentSparkStageHistory(SparkStageHistory sparkStageHistory) {
    log.info("Setting current spark stage history: {}", sparkStageHistory);
    currentSparkStageHistory = sparkStageHistory;
  }

  public void updateStageHistory(SparkStageHistory newSparkStageHistory) {
    try {
      ScaleSparkClusterRequest request = new ScaleSparkClusterRequest();
      request.setSparkStageHistory(newSparkStageHistory);
      request.setEnableDownScale(config.getBoolean("spark.scale.downscale.enable"));
      request.setEnableUpScale(config.getBoolean("spark.scale.upscale.enable"));

      Map<String, String> headers = new HashMap<>();
      headers.put(Constants.X_TENANT_NAME, config.getString("tenant.name"));
      headers.put("Content-Type", "application/json");

      log.info("Updating SparkStageHistory: {}", newSparkStageHistory);
      logCentralOrchestratorClient.postScaleSparkCluster(headers, request);
    } catch (Exception e) {
      log.error("Error in updating spark stage history: ", e);
    }
  }
}
