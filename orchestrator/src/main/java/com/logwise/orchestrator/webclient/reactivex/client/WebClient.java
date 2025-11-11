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

package com.logwise.orchestrator.webclient.reactivex.client;

import io.vertx.lang.rx.RxGen;
import io.vertx.lang.rx.TypeArg;

@RxGen(com.logwise.orchestrator.webclient.client.WebClient.class)
public class WebClient {

  @Override
  public String toString() {
    return delegate.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WebClient that = (WebClient) o;
    return delegate.equals(that.delegate);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  public static final TypeArg<WebClient> __TYPE_ARG =
      new TypeArg<>(
          obj -> new WebClient((com.logwise.orchestrator.webclient.client.WebClient) obj),
          WebClient::getDelegate);

  private final com.logwise.orchestrator.webclient.client.WebClient delegate;

  public WebClient(com.logwise.orchestrator.webclient.client.WebClient delegate) {
    this.delegate = delegate;
  }

  public WebClient(Object delegate) {
    this.delegate = (com.logwise.orchestrator.webclient.client.WebClient) delegate;
  }

  public com.logwise.orchestrator.webclient.client.WebClient getDelegate() {
    return delegate;
  }

  /**
   * Creates and configures a new object ready to send http requests from
   *
   * @param vertx
   * @return
   */
  public static WebClient create(io.vertx.reactivex.core.Vertx vertx) {
    WebClient ret =
        WebClient.newInstance(
            (com.logwise.orchestrator.webclient.client.WebClient)
                com.logwise.orchestrator.webclient.client.WebClient.create(vertx.getDelegate()));
    return ret;
  }

  public static WebClient create(io.vertx.reactivex.core.Vertx vertx, String configType) {
    WebClient ret =
        WebClient.newInstance(
            (com.logwise.orchestrator.webclient.client.WebClient)
                com.logwise.orchestrator.webclient.client.WebClient.create(
                    vertx.getDelegate(), configType));
    return ret;
  }

  /**
   * Return a WebClient Note: Call this every time you want to send http requests
   *
   * @return
   */
  public io.vertx.reactivex.ext.web.client.WebClient getWebClient() {
    io.vertx.reactivex.ext.web.client.WebClient ret =
        io.vertx.reactivex.ext.web.client.WebClient.newInstance(
            (io.vertx.ext.web.client.WebClient) delegate.getWebClient());
    return ret;
  }

  /** Close the WebClient */
  public void close() {
    delegate.close();
  }

  public static WebClient newInstance(com.logwise.orchestrator.webclient.client.WebClient arg) {
    return arg != null ? new WebClient(arg) : null;
  }
}
