package com.logwise.orchestrator.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.logwise.orchestrator.dao.query.Query;
import com.logwise.orchestrator.dto.entity.SparkScaleOverride;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.error.ServiceError;
import com.logwise.orchestrator.mysql.reactivex.client.MysqlClient;
import com.logwise.orchestrator.rest.exception.RestException;
import com.logwise.orchestrator.util.ApplicationUtils;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class SparkScaleOverrideDao {
  final MysqlClient mysqlClient;
  final ObjectMapper objectMapper;

  public Single<SparkScaleOverride> getSparkScaleOverride(Tenant tenant) {
    log.info("Getting Spark Scale Override for tenant: {}", tenant);
    return mysqlClient
        .getSlaveMysqlClient()
        .preparedQuery(Query.GET_SPARK_SCALE_OVERRIDE)
        .rxExecute(Tuple.of(tenant.getValue()))
        .map(
            rows ->
                ApplicationUtils.rowSetToMapList(rows).stream()
                    .findFirst()
                    .map(row -> objectMapper.convertValue(row, SparkScaleOverride.class))
                    .orElse(SparkScaleOverride.builder().tenant(tenant.getValue()).build()))
        .doOnError(error -> log.error("Error in Spark Scale Override: ", error));
  }

  public Completable updateSparkScaleOverride(SparkScaleOverride sparkScaleOverride) {
    log.info("Updating Spark Scale Override for tenant: {}", sparkScaleOverride.getTenant());
    Tuple tuple =
        Tuple.of(
            sparkScaleOverride.getUpscale(),
            sparkScaleOverride.getDownscale(),
            sparkScaleOverride.getTenant());
    String query = Query.UPDATE_SPARK_SCALE_OVERRIDE;
    return mysqlClient
        .getMasterMysqlClient()
        .preparedQuery(query)
        .rxExecute(tuple)
        .filter(rows -> rows.rowCount() >= 1)
        .switchIfEmpty(
            Single.error(new RestException(ServiceError.QUERY_EXECUTION_FAILED.format(query))))
        .ignoreElement();
  }
}
