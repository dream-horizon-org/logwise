package com.logwise.orchestrator.dao.query;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Query {
  public final String GET_SPARK_STAGE_HISTORY =
      "SELECT outputBytes, inputRecords, submissionTime, completionTime, coresUsed, status FROM spark_stage_history WHERE tenant = ? ORDER BY createdAt DESC LIMIT ? ";
  public final String INSERT_SPARK_STAGE_HISTORY =
      "INSERT INTO spark_stage_history (outputBytes, inputRecords, submissionTime, completionTime, coresUsed, status, tenant) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?) ";
  public final String GET_SERVICES =
      "SELECT serviceName, retentionDays, tenant FROM service_details WHERE tenant = ?;";
  public final String INSERT_SERVICE_DETAILS =
      "INSERT INTO service_details (serviceName, retentionDays, tenant) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE lastCheckedAt = NOW()";
  public final String DELETE_SERVICE_DETAILS =
      "DELETE FROM service_details WHERE serviceName = ? AND tenant = ?;";
  public final String DELETE_SERVICE_DETAILS_BEFORE_INTERVAL =
      "DELETE FROM service_details WHERE tenant = ? AND lastCheckedAt <= ?;";
  public final String GET_SPARK_SCALE_OVERRIDE =
      "SELECT upscale, downscale, tenant from spark_scale_override WHERE tenant = ? ";
  public final String UPDATE_SPARK_SCALE_OVERRIDE =
      "INSERT INTO spark_scale_override (upscale, downscale, tenant) VALUES (?, ?, ?) "
          + "ON DUPLICATE KEY UPDATE "
          + "upscale = VALUES(upscale), downscale = VALUES(downscale);";
}
