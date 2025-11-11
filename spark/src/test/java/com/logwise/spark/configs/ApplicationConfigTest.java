package com.logwise.spark.configs;

import static org.testng.Assert.*;

import com.typesafe.config.Config;
import java.lang.reflect.Method;
import org.testng.annotations.Test;

/**
 * Unit tests for ApplicationConfig.
 *
 * <p>Tests configuration loading, fallback chain, and system properties/environment integration.
 */
public class ApplicationConfigTest {

  @Test
  public void testGetConfig_WithCommandLineArguments_ReturnsConfig() {
    // Arrange
    String arg1 = "app.job.name=TEST_JOB";
    String arg2 = "tenant.name=test-tenant";

    // Act
    Config config = ApplicationConfig.getConfig(arg1, arg2);

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

    // Act
    Config config = ApplicationConfig.getConfig(arg1, arg2, arg3);

    // Assert
    assertNotNull(config);
    // First argument takes precedence (withFallback means earlier configs override later ones)
    assertEquals(config.getString("app.job.name"), "JOB1");
  }

  @Test
  public void testGetConfig_SystemPropertiesAreLoaded() throws Exception {
    // Arrange
    String testPropertyKey = "test.system.property";
    String testPropertyValue = "system-value";
    System.setProperty(testPropertyKey, testPropertyValue);
    String configArg = "tenant.name=test-tenant";
    ApplicationConfig appConfig = null;

    try {
      // Act - use reflection to call private init() method to get initialized ApplicationConfig
      Method initMethod = ApplicationConfig.class.getDeclaredMethod("init", String[].class);
      initMethod.setAccessible(true);
      appConfig = (ApplicationConfig) initMethod.invoke(null, (Object) new String[] {configArg});

      // Assert - instance creation must succeed
      assertNotNull(appConfig, "ApplicationConfig instance creation failed");
      Config systemConfig = appConfig.getSystemProperties();
      assertNotNull(systemConfig);
      // Verify system property is accessible
      assertTrue(systemConfig.hasPath(testPropertyKey), "System property should be accessible");
      assertEquals(
          systemConfig.getString(testPropertyKey),
          testPropertyValue,
          "System property value should match");
    } finally {
      // Cleanup
      System.clearProperty(testPropertyKey);
      // Fail test if instance creation did not succeed
      assertNotNull(
          appConfig, "Test failed: ApplicationConfig instance was not created successfully");
    }
  }

  @Test
  public void testGetConfig_SystemEnvironmentIsLoaded() throws Exception {
    // Arrange
    String configArg = "tenant.name=test-tenant";
    ApplicationConfig appConfig = null;

    try {
      // Act - use reflection to call private init() method to get initialized ApplicationConfig
      Method initMethod = ApplicationConfig.class.getDeclaredMethod("init", String[].class);
      initMethod.setAccessible(true);
      appConfig = (ApplicationConfig) initMethod.invoke(null, (Object) new String[] {configArg});

      // Assert - instance creation must succeed
      assertNotNull(appConfig, "ApplicationConfig instance creation failed");
      Config envConfig = appConfig.getSystemEnvironment();
      assertNotNull(envConfig);
      // Verify environment config is not empty (should contain at least PATH or similar)
      assertTrue(
          envConfig.entrySet().size() > 0,
          "Environment config should contain environment variables");
    } finally {
      // Fail test if instance creation did not succeed
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
    // Arrange - provide tenant.name to resolve substitution
    String configArg = "tenant.name=test-tenant";

    // Act
    Config config = ApplicationConfig.getConfig(configArg);

    // Assert
    assertNotNull(config);
    assertEquals(config.getString("tenant.name"), "test-tenant");
  }

  @Test
  public void testGetConfig_ConfigFactoryCacheInvalidation() {
    // Arrange
    String propertyKey = "test.property";
    String configString1 = propertyKey + "=value1\n" + "tenant.name=test-tenant";
    String configString2 = propertyKey + "=value2\n" + "tenant.name=test-tenant";

    // Act
    Config config1 = ApplicationConfig.getConfig(configString1);
    Config config2 = ApplicationConfig.getConfig(configString2);

    // Assert - ConfigFactory.invalidateCaches() is called, so each call should use new config
    assertNotNull(config1);
    assertNotNull(config2);
    // Verify cache invalidation works - each config should have different values
    assertEquals(config1.getString(propertyKey), "value1", "First config should have value1");
    assertEquals(
        config2.getString(propertyKey),
        "value2",
        "Second config should have value2 after cache invalidation");
  }

  @Test(expectedExceptions = com.typesafe.config.ConfigException.UnresolvedSubstitution.class)
  public void testGetConfig_WithEmptyArgs_RequiresSubstitutionResolution() {
    // Arrange - This test verifies that empty args require substitution to be provided
    // Since application.conf has ${X-Tenant-Name}, it will fail without providing it

    // Act & Assert - Should throw UnresolvedSubstitution exception
    // Note: System properties are not automatically included in config resolution chain
    // by the current implementation, so this will fail as expected
    ApplicationConfig.getConfig();
  }

  @Test
  public void testGetConfig_CommandLineArgsOverrideApplicationConf() {
    // Arrange
    // application.conf has app.job.name = push-logs-to-s3
    // We'll override it with command-line arg
    String overriddenJobName = "custom-job-name";
    String configArg = "app.job.name=" + overriddenJobName + "\n" + "tenant.name=test-tenant";

    // Act
    Config config = ApplicationConfig.getConfig(configArg);

    // Assert
    assertNotNull(config);
    // Command-line arg should override application.conf value
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
    // Arrange - Set system properties
    String customPropertyKey = "custom.test.property";
    String customPropertyValue = "custom-value";
    System.setProperty(customPropertyKey, customPropertyValue);
    String configArg = "tenant.name=test-tenant";

    try {
      // Act - Get config and access system properties
      Config config = ApplicationConfig.getConfig(configArg);

      // Use reflection to get ApplicationConfig instance to access system properties
      Method initMethod = ApplicationConfig.class.getDeclaredMethod("init", String[].class);
      initMethod.setAccessible(true);
      ApplicationConfig appConfig =
          (ApplicationConfig) initMethod.invoke(null, (Object) new String[] {configArg});

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
}
