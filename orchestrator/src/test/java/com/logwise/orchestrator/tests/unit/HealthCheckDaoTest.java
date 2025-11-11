package com.logwise.orchestrator.tests.unit;

import com.logwise.orchestrator.setup.BaseTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;

/**
 * Unit tests for HealthCheckDao.
 *
 * <p>NOTE: These tests are disabled because MysqlClient is a generated RxGen class that Mockito
 * cannot mock. Use integration tests with Testcontainers for testing DAO classes.
 */
public class HealthCheckDaoTest extends BaseTest {

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();

    throw new org.testng.SkipException(
        "MysqlClient cannot be mocked - requires integration test setup");
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }
}
