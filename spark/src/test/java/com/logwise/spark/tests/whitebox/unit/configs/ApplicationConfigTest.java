package com.logwise.spark.tests.whitebox.unit.configs;

import com.logwise.spark.configs.ApplicationConfig;
import com.logwise.spark.constants.Groups;
import com.logwise.tests.utils.AssertionUtils;
import com.logwise.spark.utils.TestUtils;
import com.typesafe.config.Config;
import java.lang.reflect.Method;
import java.util.function.Predicate;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = {Groups.TEST_RUN_TYPE_WHITEBOX})
public class ApplicationConfigTest {

  private static final String[] ENV_TESTS = {"testGetConfigWithEnv"};
  private static final Predicate<Method> IS_ENV_TEST =
      (method) ->
          method.isAnnotationPresent(Test.class)
              && ArrayUtils.contains(ENV_TESTS, method.getName());
  private static final Logger log = org.slf4j.LoggerFactory.getLogger(ApplicationConfigTest.class);

  @BeforeMethod
  public void beforeMethod(Method method) {
    if (IS_ENV_TEST.test(method)) {
      log.debug("Setting up environment variables: env=test");
      TestUtils.setEnv("env", "test");
    }
  }

  @AfterMethod
  public void afterMethod(Method method) {
    if (IS_ENV_TEST.test(method)) {
      log.debug("Tearing down environment variables");
      TestUtils.unsetEnv("env");
    }
  }

  @Test(
      description = "Test getConfig method with valid arguments for default env",
      groups = {Groups.TEST_TYPE_UNIT, Groups.PURPOSE_POSITIVE_TESTS})
  public void testGetConfigWithDefaultEnv() {
    String[] args =
        ArrayUtils.toArray(
            "test.config.args=args-config-value",
            "test.config.overwrite.args=overwrite-args-config-value");

    // Act
    Config config = ApplicationConfig.getConfig(args);

    // Assert
    String actualFromArgs = config.getString("test.config.args");
    AssertionUtils.assertEquals(
        actualFromArgs, "args-config-value", "getConfig method should return a correct Config");

    String actualFromDefaultConf = config.getString("test.config.default");
    AssertionUtils.assertEquals(
        actualFromDefaultConf,
        "default-config-value",
        "getConfig method should return a correct Config");

    String actualFromOverwriteArgs = config.getString("test.config.overwrite.args");
    AssertionUtils.assertEquals(
        actualFromOverwriteArgs,
        "overwrite-args-config-value",
        "getConfig method should return a correct Config with overwritten args");
  }

  @Test(
      description = "Test getConfig method with valid arguments for test env",
      groups = {Groups.TEST_TYPE_UNIT, Groups.PURPOSE_POSITIVE_TESTS})
  public void testGetConfigWithEnv() {
    String[] args =
        ArrayUtils.toArray(
            "test.config.args=args-config-value",
            "test.config.overwrite.args=overwrite-args-config-value");

    // Act
    Config config = ApplicationConfig.getConfig(args);

    // Assert
    String actualFromArgs = config.getString("test.config.args");
    AssertionUtils.assertEquals(
        actualFromArgs, "args-config-value", "getConfig method should return a correct Config");

    String actualFromEnvConf = config.getString("test.config.env");
    AssertionUtils.assertEquals(
        actualFromEnvConf,
        "test-env-config-value",
        "getConfig method should return a correct Config");

    String actualFromOverwriteEnvConf = config.getString("test.config.default");
    AssertionUtils.assertEquals(
        actualFromOverwriteEnvConf,
        "test-env-config-value",
        "getConfig method should return a correct Config with overwritten default");

    String actualFromOverwriteArgs = config.getString("test.config.overwrite.args");
    AssertionUtils.assertEquals(
        actualFromOverwriteArgs,
        "overwrite-args-config-value",
        "getConfig method should return a correct Config with overwritten args");
  }
}
