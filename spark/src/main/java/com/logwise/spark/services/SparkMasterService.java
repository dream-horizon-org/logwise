package com.logwise.spark.services;

import com.google.inject.Inject;
import com.logwise.spark.clients.SparkMasterClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class SparkMasterService {
  private final SparkMasterClient sparkMasterClient;

  public Integer getCoresUsed() {
    try {
      int coresUsed = sparkMasterClient.json().getCoresused();
      log.info("coresUsed: {}", coresUsed);
      return coresUsed;
    } catch (Exception e) {
      log.error("Error in fetching active cores from spark master", e);
    }
    return null;
  }
}
