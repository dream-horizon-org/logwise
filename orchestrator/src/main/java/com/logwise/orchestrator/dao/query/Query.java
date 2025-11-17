package com.logwise.orchestrator.dao.query;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Query {
  public final String GET_SERVICES =
      "SELECT environmentName, serviceName, componentType, retentionDays, tenant FROM service_details WHERE tenant = ?;";
  public final String INSERT_SERVICE_DETAILS =
      "INSERT INTO service_details (environmentName, componentType, serviceName, retentionDays, tenant) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE lastCheckedAt = NOW()";
  public final String DELETE_SERVICE_DETAILS =
      "DELETE FROM service_details WHERE environmentName = ? AND serviceName = ? AND tenant = ?;";
  public final String DELETE_SERVICE_DETAILS_BEFORE_INTERVAL =
      "DELETE FROM service_details WHERE tenant = ? AND lastCheckedAt <= ?;";
  public final String GET_PENDING_OFFSET_BY_TIMESTAMP_SPARK_JOB =
      "SELECT id, tenant, startingOffsetsTimestamp, resumeToSubscribePatternTimestamp, isSubmittedForOffsetsTimestamp, isResumedToSubscribePattern "
          + "FROM spark_submit_status "
          + "WHERE tenant = ? AND (!isSubmittedForOffsetsTimestamp OR !isResumedToSubscribePattern);";
  public final String UPDATE_IS_SUBMITTED_FOR_OFFSET_TIMESTAMP_FLAG =
      "UPDATE spark_submit_status " + "SET isSubmittedForOffsetsTimestamp = ? " + "WHERE id = ?;";
  public final String UPDATE_IS_RESUMED_TO_SUBSCRIBE_PATTERN_FLAG =
      "UPDATE spark_submit_status " + "SET isResumedToSubscribePattern = ? " + "WHERE id = ?;";
}
