/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.logwise.orchestrator.mysql.reactivex.client;

import com.logwise.orchestrator.mysql.client.MysqlConfig;
import io.reactivex.Completable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.lang.rx.RxGen;
import io.vertx.lang.rx.TypeArg;
import io.vertx.reactivex.impl.AsyncResultCompletable;

@RxGen(com.logwise.orchestrator.mysql.MysqlClient.class)
public class MysqlClient {

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MysqlClient that = (MysqlClient) o;
    return delegate.equals(that.delegate);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  public static final TypeArg<MysqlClient> __TYPE_ARG =
      new TypeArg<>(
          obj -> new MysqlClient((com.logwise.orchestrator.mysql.MysqlClient) obj),
          MysqlClient::getDelegate);

  private final com.logwise.orchestrator.mysql.MysqlClient delegate;

  public MysqlClient(com.logwise.orchestrator.mysql.MysqlClient delegate) {
    this.delegate = delegate;
  }

  public MysqlClient(Object delegate) {
    this.delegate = (com.logwise.orchestrator.mysql.MysqlClient) delegate;
  }

  public com.logwise.orchestrator.mysql.MysqlClient getDelegate() {
    return delegate;
  }

  public static MysqlClient create(
      io.vertx.reactivex.core.Vertx vertx,
      io.vertx.reactivex.config.ConfigRetriever configRetriever) {
    MysqlClient ret =
        MysqlClient.newInstance(
            (com.logwise.orchestrator.mysql.MysqlClient)
                com.logwise.orchestrator.mysql.MysqlClient.create(
                    vertx.getDelegate(), configRetriever.getDelegate()));
    return ret;
  }

  public static MysqlClient create(io.vertx.reactivex.core.Vertx vertx) {
    MysqlClient ret =
        MysqlClient.newInstance(
            (com.logwise.orchestrator.mysql.MysqlClient)
                com.logwise.orchestrator.mysql.MysqlClient.create(vertx.getDelegate()));
    return ret;
  }

  public static MysqlClient create(io.vertx.reactivex.core.Vertx vertx, String configType) {
    MysqlClient ret =
        MysqlClient.newInstance(
            (com.logwise.orchestrator.mysql.MysqlClient)
                com.logwise.orchestrator.mysql.MysqlClient.create(vertx.getDelegate(), configType));
    return ret;
  }

  public void connect(Handler<AsyncResult<Void>> handler) {
    delegate.connect(handler);
  }

  public Completable rxConnect() {
    return AsyncResultCompletable.toCompletable(
        handler -> {
          connect(handler);
        });
  }

  /**
   * Return a MysqlClient Note: Call this every time you want to perform an query on mysql master
   *
   * @return
   */
  public io.vertx.reactivex.mysqlclient.MySQLPool getMasterMysqlClient() {
    io.vertx.reactivex.mysqlclient.MySQLPool ret =
        io.vertx.reactivex.mysqlclient.MySQLPool.newInstance(
            (io.vertx.mysqlclient.MySQLPool) delegate.getMasterMysqlClient());
    return ret;
  }

  /**
   * Return a MysqlClient Note: Call this every time you want to perform an query on mysql slave
   * Note: Picks a random slave node at the time of connection and forwards all later queries to
   * that node only
   *
   * @return
   */
  public io.vertx.reactivex.mysqlclient.MySQLPool getSlaveMysqlClient() {
    io.vertx.reactivex.mysqlclient.MySQLPool ret =
        io.vertx.reactivex.mysqlclient.MySQLPool.newInstance(
            (io.vertx.mysqlclient.MySQLPool) delegate.getSlaveMysqlClient());
    return ret;
  }

  /**
   * Note: Only use for debugging as it may return stale config
   *
   * @return
   */
  @Deprecated()
  public MysqlConfig getConfig() {
    MysqlConfig ret = delegate.getConfig();
    return ret;
  }

  public void close() {
    delegate.close();
  }

  public static MysqlClient newInstance(com.logwise.orchestrator.mysql.MysqlClient arg) {
    return arg != null ? new MysqlClient(arg) : null;
  }
}
