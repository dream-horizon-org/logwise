package com.logwise.spark.runners;

import com.logwise.spark.constants.TestConstants;
import com.logwise.spark.setup.Setup;
import com.logwise.spark.setup.factory.SetupFactory;
import com.logwise.tests.runners.TestNgRunner;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

@Slf4j
public class TestRunner extends TestNgRunner {

  @NonFinal private Setup setup;

  public TestRunner() {
    super();
    this.setup = SetupFactory.getSetup(TestConstants.EXISTING_TEST_RUN_TYPE);
  }

  @BeforeSuite(alwaysRun = true)
  public void beforeTest() {
    log.info("Starting Before Test");
    setup.setUp();
  }

  @AfterSuite(alwaysRun = true)
  public void afterTest() {
    log.info("Starting After Test");
    setup.tearDown();
  }
}
