package com.dream11.logcentralorchestrator.dao;

import com.dream11.logcentralorchestrator.common.util.JsonUtils;
import com.dream11.logcentralorchestrator.common.util.SingleUtils;
import com.dream11.logcentralorchestrator.mysql.reactivex.client.MysqlClient;
import com.google.inject.Inject;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class HealthCheckDao {

  final MysqlClient d11MysqlClient;

  public Single<JsonObject> mysqlHealthCheck() {
    val masterMysqlClient = d11MysqlClient.getMasterMysqlClient();
    return masterMysqlClient
        .query("SELECT 1;")
        .rxExecute()
        .map(rowSet -> JsonUtils.jsonFrom("response", "1"))
        .compose(SingleUtils.applyDebugLogs(log));
  }
}
