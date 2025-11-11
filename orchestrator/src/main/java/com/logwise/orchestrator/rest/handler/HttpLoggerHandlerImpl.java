package com.logwise.orchestrator.rest.handler;

import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.impl.LoggerHandlerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpLoggerHandlerImpl extends LoggerHandlerImpl {

  private final Logger log;

  public HttpLoggerHandlerImpl(String loggerName) {
    this(LoggerFormat.DEFAULT, loggerName);
  }

  public HttpLoggerHandlerImpl(boolean immediate, LoggerFormat format, String loggerName) {
    super(immediate, format);
    this.log = LoggerFactory.getLogger(loggerName);
  }

  public HttpLoggerHandlerImpl(LoggerFormat format, String loggerName) {
    this(false, format, loggerName);
  }

  @Override
  protected void doLog(int status, String message) {
    if (status >= 500) {
      log.error(message);
    } else if (status >= 400) {
      log.warn(message);
    } else {
      log.info(message);
    }
  }
}
