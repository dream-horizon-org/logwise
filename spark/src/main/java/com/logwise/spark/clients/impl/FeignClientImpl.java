package com.logwise.spark.clients.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.spark.clients.FeignClient;
import com.logwise.spark.constants.Constants;
import com.logwise.spark.feign.decoders.DefaultErrorDecoder;
import com.logwise.spark.feign.logger.Log4jLogger;
import feign.Feign;
import feign.Logger;
import feign.Request.Options;
import feign.Retryer;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeignClientImpl implements FeignClient {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Options defaultOptions =
      new Options(
          Constants.FEIGN_DEFAULT_CONNECTION_TIMEOUT_IN_SECONDS,
          TimeUnit.SECONDS,
          Constants.FEIGN_DEFAULT_READ_TIMEOUT_IN_SECONDS,
          TimeUnit.SECONDS,
          true);
  private final Retryer retryer =
      new Retryer.Default(
          Constants.FEIGN_DEFAULT_RETRY_PERIOD_IN_MILLIS,
          Constants.FEIGN_DEFAULT_RETRY_MAX_PERIOD_IN_MILLIS,
          Constants.FEIGN_DEFAULT_RETRY_COUNT);

  @Override
  public <T> T createClient(Class<T> targetClass, String url) {
    log.info("Creating client for targetClass: {} and url: {}", targetClass.getSimpleName(), url);
    return Feign.builder()
        .encoder(new JacksonEncoder(objectMapper))
        .decoder(new JacksonDecoder(objectMapper))
        .retryer(retryer)
        .logger(new Log4jLogger())
        .logLevel(Logger.Level.BASIC)
        .errorDecoder(new DefaultErrorDecoder())
        .options(defaultOptions)
        .target(targetClass, url);
  }
}
