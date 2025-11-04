package com.logwise.spark.setup.impl;

import com.logwise.spark.setup.Setup;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BlackBoxSetup implements Setup {
  @Override
  public void setUp() {
    log.info("Setting up Blackbox Test");
  }

  @Override
  public void tearDown() {
    log.info("Tearing down Blackbox Test");
  }
}
