package com.logwise.spark.configs;

import static org.testng.Assert.*;

import com.typesafe.config.Config;
import java.lang.reflect.Method;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for ApplicationConfig.
 *
 * <p>Tests configuration loading, fallback chain, and system properties/environment integration.
 */
public class ApplicationConfigTest {

  private String originalTenantValue;

  @BeforeMethod
  public void setUp() {
    originalTenantValue = System.getProperty("X-Tenant-Name");
    System.setProperty("X-Tenant-Name", "test-tenant");
  }

  @AfterMethod
  public void tearDown() {
    if (originalTenantValue != null) {
      System.setProperty("X-Tenant-Name", originalTenantValue);
    } else {
      System.clearProperty("X-Tenant-Name");
    }
  }

  @Test
  public void testGetConfig_WithCommandLineArguments_ReturnsConfig() {
    // Arrange
    String arg1 = "app.job.name=TEST_JOB";
    String arg2 = "tenant.name=test-tenant";
    String arg3 = "s3.bucket=test-bucket";

    // Act
    Config config = ApplicationConfig.getConfig(arg1, arg2, arg3);

    // Assert
    assertNotNull(config);
    assertEquals(config.getString("app.job.name"), "TEST_JOB");
    assertEquals(config.getString("tenant.name"), "test-tenant");
  }

  @Test
  public void testGetConfig_WithMultipleArguments_UsesFallbackChain() {
    // Arrange
    String arg1 = "app.job.name=JOB1";
    String arg2 = "app.job.name=JOB2";
    String arg3 = "tenant.name=test-tenant"; // Required to resolve substitution
    String arg4 = "s3.bucket=test-bucket"; // Required to resolve substitution

    // Act
    Config config = ApplicationConfig.getConfig(arg1, arg2, arg3, arg4);

    // Assert
    assertNotNull(config);
    // First argument takes precedence (withFallback means earlier configs override
    // later ones)
    assertEquals(config.getString("app.job.name"), "JOB1");
  }

  @Test
  public void testGetConfig_SystemPropertiesAreLoaded() throws Exception {
    String testPropertyKey = "test.system.property";
    String testPropertyValue = "system-value";
    System.setProperty(testPropertyKey, testPropertyValue);
    String configArg1 = "tenant.name=test-tenant";
    String configArg2 = "s3.bucket=test-bucket";
    ApplicationConfig appConfig = null;

    try {
      Method initMethod = ApplicationConfig.class.getDeclaredMethod("init", String[].class);
      initMethod.setAccessible(true);
      appConfig =
          (ApplicationConfig)
              initMethod.invoke(null, (Object) new String[] {configArg1, configArg2});

      assertNotNull(appConfig, "ApplicationConfig instance creation failed");
      Config systemConfig = appConfig.getSystemProperties();
      assertNotNull(systemConfig);
      assertTrue(systemConfig.hasPath(testPropertyKey), "System property should be accessible");
      assertEquals(
          systemConfig.getString(testPropertyKey),
          testPropertyValue,
          "System property value should match");
    } finally {
      System.clearProperty(testPropertyKey);
      assertNotNull(
          appConfig, "Test failed: ApplicationConfig instance was not created successfully");
    }
  }

  @Test
  public void testGetConfig_SystemEnvironmentIsLoaded() throws Exception {
    String configArg1 = "tenant.name=test-tenant";
    String configArg2 = "s3.bucket=test-bucket";
    ApplicationConfig appConfig = null;

    try {
      Method initMethod = ApplicationConfig.class.getDeclaredMethod("init", String[].class);
      initMethod.setAccessible(true);
      appConfig =
          (ApplicationConfig)
              initMethod.invoke(null, (Object) new String[] {configArg1, configArg2});

      assertNotNull(appConfig, "ApplicationConfig instance creation failed");
      Config envConfig = appConfig.getSystemEnvironment();
      assertNotNull(envConfig);
      assertTrue(
          envConfig.entrySet().size() > 0,
          "Environment config should contain environment variables");
    } finally {
      assertNotNull(
          appConfig, "Test failed: ApplicationConfig instance was not created successfully");
    }
  }

  @Test(expectedExceptions = com.typesafe.config.ConfigException.class)
  public void testGetConfig_WithInvalidConfigString_ThrowsException() {
    // Arrange
    String invalidConfig = "invalid config string {";

    // Act & Assert - should throw exception for invalid config
    ApplicationConfig.getConfig(invalidConfig);
  }

  @Test
  public void testGetConfig_WithTenantName_ReturnsConfig() {
    String configArg1 = "tenant.name=test-tenant";
    String configArg2 = "s3.bucket=test-bucket";

    Config config = ApplicationConfig.getConfig(configArg1, configArg2);

    assertNotNull(config);
    assertEquals(config.getString("tenant.name"), "test-tenant");
  }

  @Test
  public void testGetConfig_ConfigFactoryCacheInvalidation() {
    String propertyKey = "test.property";
    String configString1a = propertyKey + "=value1";
    String configString1b = "tenant.name=test-tenant";
    String configString1c = "s3.bucket=test-bucket";
    String configString2a = propertyKey + "=value2";
    String configString2b = "tenant.name=test-tenant";
    String configString2c = "s3.bucket=test-bucket";

    Config config1 = ApplicationConfig.getConfig(configString1a, configString1b, configString1c);
    Config config2 = ApplicationConfig.getConfig(configString2a, configString2b, configString2c);

    // Assert - ConfigFactory.invalidateCaches() is called, so each call should use
    // new config
    assertNotNull(config1);
    assertNotNull(config2);
    // Verify cache invalidation works - each config should have different values
    assertEquals(config1.getString(propertyKey), "value1", "First config should have value1");
    assertEquals(
        config2.getString(propertyKey),
        "value2",
        "Second config should have value2 after cache invalidation");
  }

  @Test
  public void testGetConfig_CommandLineArgsOverrideApplicationConf() {
    String overriddenJobName = "custom-job-name";
    String configArg1 = "app.job.name=" + overriddenJobName;
    String configArg2 = "tenant.name=test-tenant";
    String configArg3 = "s3.bucket=test-bucket";

    Config config = ApplicationConfig.getConfig(configArg1, configArg2, configArg3);

    assertNotNull(config);
    assertEquals(
        config.getString("app.job.name"),
        overriddenJobName,
        "Command-line arg should override application.conf value");
    // Verify other application.conf values are still present (fallback works)
    assertTrue(
        config.hasPath("kafka.bootstrap.servers.port"),
        "Should have kafka.bootstrap.servers.port from application.conf");
    assertEquals(
        config.getInt("kafka.bootstrap.servers.port"),
        9092,
        "Should have correct value from application.conf");
  }

  @Test
  public void testGetConfig_SystemPropertiesAvailableForAccess() {
    String customPropertyKey = "custom.test.property";
    String customPropertyValue = "custom-value";
    System.setProperty(customPropertyKey, customPropertyValue);
    String configArg1 = "tenant.name=test-tenant";
    String configArg2 = "s3.bucket=test-bucket";

    try {
      Config config = ApplicationConfig.getConfig(configArg1, configArg2);

      Method initMethod = ApplicationConfig.class.getDeclaredMethod("init", String[].class);
      initMethod.setAccessible(true);
      ApplicationConfig appConfig =
          (ApplicationConfig)
              initMethod.invoke(null, (Object) new String[] {configArg1, configArg2});

      Config systemConfig = appConfig.getSystemProperties();

      // Assert
      assertNotNull(config);
      assertNotNull(systemConfig);
      // Verify system properties are accessible via getSystemProperties()
      assertTrue(
          systemConfig.hasPath(customPropertyKey),
          "System property should be accessible via getSystemProperties()");
      assertEquals(
          systemConfig.getString(customPropertyKey),
          customPropertyValue,
          "System property value should match");
    } catch (Exception e) {
      fail("Test should not throw exception: " + e.getMessage());
    } finally {
      // Cleanup
      System.clearProperty(customPropertyKey);
    }
  }

  @Test
  public void testGetConfigProperties_ReturnsPropertiesConfig() throws Exception {
    String configArg1 = "test.key=test.value";
    String configArg2 = "tenant.name=test-tenant";
    String configArg3 = "s3.bucket=test-bucket";
    Method initMethod = ApplicationConfig.class.getDeclaredMethod("init", String[].class);
    initMethod.setAccessible(true);
    ApplicationConfig appConfig =
        (ApplicationConfig)
            initMethod.invoke(null, (Object) new String[] {configArg1, configArg2, configArg3});

    // Act
    Config propertiesConfig = appConfig.getConfigProperties();

    // Assert
    assertNotNull(propertiesConfig);
    assertTrue(propertiesConfig.hasPath("test.key"));
    assertEquals(propertiesConfig.getString("test.key"), "test.value");
  }

  @Test
  public void testGetConfig_WithEmptyArgs_ReturnsConfig() {
    String configArg1 = "tenant.name=test-tenant";
    String configArg2 = "s3.bucket=test-bucket";

    Config config = ApplicationConfig.getConfig(configArg1, configArg2);

    assertNotNull(config);
    assertTrue(config.hasPath("app.job.name"));
  }

  @Test
  public void testGetConfig_WithEmptyStringArg_HandlesGracefully() {
    String emptyArg = "";
    String configArg1 = "tenant.name=test-tenant";
    String configArg2 = "s3.bucket=test-bucket";

    Config config = ApplicationConfig.getConfig(emptyArg, configArg1, configArg2);

    assertNotNull(config);
    assertTrue(config.hasPath("s3.bucket"));
  }

  @Test
  public void testGetConfig_WithMultipleEmptyArgs_HandlesGracefully() {
    String emptyArg1 = "";
    String emptyArg2 = "";
    String configArg1 = "tenant.name=test-tenant";
    String configArg2 = "s3.bucket=test-bucket";

    Config config = ApplicationConfig.getConfig(emptyArg1, emptyArg2, configArg1, configArg2);

    assertNotNull(config);
    assertTrue(config.hasPath("s3.bucket"));
  }

  @Test
  public void testGetConfig_WithWhitespaceOnlyArgs_HandlesGracefully() {
    String whitespaceArg = "   \n\t  ";
    String configArg1 = "tenant.name=test-tenant";
    String configArg2 = "s3.bucket=test-bucket";

    Config config = ApplicationConfig.getConfig(whitespaceArg, configArg1, configArg2);

    assertNotNull(config);
    assertTrue(config.hasPath("s3.bucket"));
  }

  @Test
  public void testGetConfig_WithQuotedValueInPropertiesFormat_RemovesQuotes() {
    // Test parseConfigString branch: when value already has quotes (lines 86-88)
    String configArg1 = "key=\"already-quoted-value\"";
    String configArg2 = "tenant.name=test-tenant";
    String configArg3 = "s3.bucket=test-bucket";

    Config config = ApplicationConfig.getConfig(configArg1, configArg2, configArg3);

    assertNotNull(config);
    assertEquals(config.getString("key"), "already-quoted-value");
  }

  @Test
  public void testGetConfig_WithHoconFormat_HandlesCorrectly() {
    // Test parseConfigString: when conf doesn't contain "=", uses HOCON format (line 95)
    String hoconConfig = "key:value";
    String configArg1 = "tenant.name=test-tenant";
    String configArg2 = "s3.bucket=test-bucket";

    Config config = ApplicationConfig.getConfig(hoconConfig, configArg1, configArg2);

    assertNotNull(config);
    assertEquals(config.getString("key"), "value");
  }

  @Test
  public void testGetConfig_WithPropertiesFormatContainingColon_QuotesValue() {
    // Test parseConfigString: properties format with colon in value (line 90)
    String configArg1 = "key=value:with:colons";
    String configArg2 = "tenant.name=test-tenant";
    String configArg3 = "s3.bucket=test-bucket";

    Config config = ApplicationConfig.getConfig(configArg1, configArg2, configArg3);

    assertNotNull(config);
    assertEquals(config.getString("key"), "value:with:colons");
  }
}
