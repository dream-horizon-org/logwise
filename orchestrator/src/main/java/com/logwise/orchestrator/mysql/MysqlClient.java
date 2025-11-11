package com.logwise.orchestrator.mysql;

import static com.logwise.orchestrator.common.util.ConfigUtils.getRetriever;

import com.logwise.orchestrator.mysql.client.MysqlConfig;
import com.logwise.orchestrator.mysql.client.impl.MysqlClientImpl;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@VertxGen
public interface MysqlClient extends AutoCloseable {

  static MysqlClient create(Vertx vertx, ConfigRetriever configRetriever) {
    return new MysqlClientImpl(vertx, configRetriever);
  }

  static MysqlClient create(Vertx vertx) {
    return create(vertx, "");
  }

  static MysqlClient create(Vertx vertx, String configType) {
    String folderName = configType.equals("") ? "mysql" : "mysql-" + configType;
    return create(vertx, getRetriever(vertx, "config/" + folderName + "/connection-%s.conf"));
  }

  void connect(Handler<AsyncResult<Void>> handler);

  /**
   * Return a MysqlClient Note: Call this every time you want to perform an query on mysql master
   */
  io.vertx.mysqlclient.MySQLPool getMasterMysqlClient();

  /**
   * Return a MysqlClient Note: Call this every time you want to perform an query on mysql slave
   * Note: Picks a random slave node at the time of connection and forwards all later queries to
   * that node only
   */
  io.vertx.mysqlclient.MySQLPool getSlaveMysqlClient();

  /** Note: Only use for debugging as it may return stale config */
  @Deprecated
  MysqlConfig getConfig();

  void close();
}
