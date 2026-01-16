package com.logwise.spark.feign.logger;

import feign.Logger;
import feign.Request;
import feign.Response;
import lombok.SneakyThrows;

/**
 * Log4jLogger is a custom logger that extends Feign Logger class and uses Log4j to log the requests
 * and responses.
 */
public class Log4jLogger extends Logger {
  private final org.apache.log4j.Logger logger;

  public Log4jLogger() {
    this(Logger.class);
  }

  public Log4jLogger(Class<?> clazz) {
    this(org.apache.log4j.Logger.getLogger(clazz));
  }

  public Log4jLogger(String name) {
    this(org.apache.log4j.Logger.getLogger(name));
  }

  Log4jLogger(org.apache.log4j.Logger logger) {
    logger.setLevel(org.apache.log4j.Level.DEBUG);
    this.logger = logger;
  }

  protected void logRequest(String configKey, Level logLevel, Request request) {
    if (this.logger.isDebugEnabled()) {
      super.logRequest(configKey, logLevel, request);
    }
  }

  @SneakyThrows
  protected Response logAndRebufferResponse(
      String configKey, Level logLevel, Response response, long elapsedTime) {
    return this.logger.isDebugEnabled()
        ? super.logAndRebufferResponse(configKey, logLevel, response, elapsedTime)
        : response;
  }

  protected void log(String configKey, String format, Object... args) {
    if (this.logger.isDebugEnabled()) {
      this.logger.debug(String.format(methodTag(configKey) + format, args));
    }
  }
}
