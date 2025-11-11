package com.dream11.logcentralorchestrator.dao;

import com.dream11.logcentralorchestrator.dao.query.Query;
import com.dream11.logcentralorchestrator.dto.entity.SparkSubmitStatus;
import com.dream11.logcentralorchestrator.enums.Tenant;
import com.dream11.logcentralorchestrator.error.ServiceError;
import com.dream11.logcentralorchestrator.mysql.reactivex.client.MysqlClient;
import com.dream11.logcentralorchestrator.rest.exception.RestException;
import com.dream11.logcentralorchestrator.util.ApplicationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class SparkSubmitStatusDao {
  final MysqlClient d11MysqlClient;
  final ObjectMapper objectMapper;

  public Single<SparkSubmitStatus> getPendingSparkSubmitStatus(Tenant tenant) {
    log.info("Getting Pending Starting Offsets Timestamp for tenant: {}", tenant);
    return d11MysqlClient
        .getMasterMysqlClient()
        .preparedQuery(Query.GET_PENDING_OFFSET_BY_TIMESTAMP_SPARK_JOB)
        .rxExecute(Tuple.of(tenant.getValue()))
        .map(
            rows -> {
              SparkSubmitStatus status =
                  ApplicationUtils.rowSetToMapList(rows).stream()
                      .findFirst()
                      .map(row -> objectMapper.convertValue(row, SparkSubmitStatus.class))
                      .orElse(SparkSubmitStatus.builder().tenant(tenant.getValue()).build());
              log.info("Pending Starting Offsets Timestamp for tenant: {} is {}", tenant, status);
              return status;
            })
        .doOnError(
            error -> log.error("Error in getting pending starting offsets timestamp: ", error));
  }

  public Completable updateIsSubmittedForOffsetsTimestampFlag(
      Long sparkSubmitStatusId, Boolean isSubmittedForOffsetsTimestamp) {
    log.info(
        "Updating isSubmittedForOffsetsTimestamp Flag: {} for id {}",
        isSubmittedForOffsetsTimestamp,
        sparkSubmitStatusId);
    Tuple tuple = Tuple.of(isSubmittedForOffsetsTimestamp, sparkSubmitStatusId);
    String query = Query.UPDATE_IS_SUBMITTED_FOR_OFFSET_TIMESTAMP_FLAG;
    return d11MysqlClient
        .getMasterMysqlClient()
        .preparedQuery(query)
        .rxExecute(tuple)
        .filter(rows -> rows.rowCount() >= 1)
        .switchIfEmpty(
            Single.error(new RestException(ServiceError.QUERY_EXECUTION_FAILED.format(query))))
        .ignoreElement();
  }

  public Completable updateIsResumedToSubscribePatternFlag(
      Long sparkSubmitStatusId, Boolean isResumedToSubscribePattern) {
    log.info(
        "Updating isResumedToSubscribePattern Flag: {} for id {}",
        isResumedToSubscribePattern,
        sparkSubmitStatusId);
    Tuple tuple = Tuple.of(isResumedToSubscribePattern, sparkSubmitStatusId);
    String query = Query.UPDATE_IS_RESUMED_TO_SUBSCRIBE_PATTERN_FLAG;
    return d11MysqlClient
        .getMasterMysqlClient()
        .preparedQuery(query)
        .rxExecute(tuple)
        .filter(rows -> rows.rowCount() >= 1)
        .switchIfEmpty(
            Single.error(new RestException(ServiceError.QUERY_EXECUTION_FAILED.format(query))))
        .ignoreElement();
  }
}
