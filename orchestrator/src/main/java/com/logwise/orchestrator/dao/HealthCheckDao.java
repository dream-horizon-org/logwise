package com.logwise.orchestrator.dao;

import com.google.inject.Inject;
import com.logwise.orchestrator.common.util.JsonUtils;
import com.logwise.orchestrator.common.util.SingleUtils;
import com.logwise.orchestrator.mysql.reactivex.client.MysqlClient;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class HealthCheckDao {

  final MysqlClient mysqlClient;

  public Single<JsonObject> mysqlHealthCheck() {
    val masterMysqlClient = mysqlClient.getMasterMysqlClient();
    return masterMysqlClient
        .query("SELECT 1;")
        .rxExecute()
        .map(rowSet -> JsonUtils.jsonFrom("response", "1"))
        .compose(SingleUtils.applyDebugLogs(log));
  }
}
