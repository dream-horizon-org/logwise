package com.logwise.orchestrator.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.logwise.orchestrator.dao.query.Query;
import com.logwise.orchestrator.dto.entity.SparkStageHistory;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.mysql.reactivex.client.MysqlClient;
import com.logwise.orchestrator.util.ApplicationUtils;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.sqlclient.Tuple;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class SparkStageHistoryDao {
  final MysqlClient mysqlClient;
  final ObjectMapper objectMapper;

  public Single<List<SparkStageHistory>> getSparkStageHistory(
      Tenant tenant, int limit, boolean fromMaster) {
    log.info("Getting Spark Stage History for tenant: {}", tenant);

    String query = Query.GET_SPARK_STAGE_HISTORY;
    return mysqlClient
        .getMasterMysqlClient()
        .preparedQuery(query)
        .rxExecute(Tuple.of(tenant.getValue(), limit))
        .map(
            rows -> {
              log.info("RowSet for query: {} is: {}", query, rows);
              return ApplicationUtils.rowSetToMapList(rows).stream()
                  .map(row -> objectMapper.convertValue(row, SparkStageHistory.class))
                  .collect(Collectors.toList());
            })
        .doOnError(error -> log.error("Error in getting Spark Stage History: ", error));
  }

  public Completable insertSparkStageHistory(SparkStageHistory history) {
    log.info("Inserting Spark Stage History: {}", history);
    List<Object> values =
        Arrays.asList(
            history.getOutputBytes(),
            history.getInputRecords(),
            history.getSubmissionTime(),
            history.getCompletionTime(),
            history.getCoresUsed(),
            history.getStatus(),
            history.getTenant());
    return mysqlClient
        .getMasterMysqlClient()
        .preparedQuery(Query.INSERT_SPARK_STAGE_HISTORY)
        .rxExecute(Tuple.wrap(values))
        .ignoreElement()
        .doOnError(error -> log.error("Error in getting Spark Stage History: ", error));
  }
}
