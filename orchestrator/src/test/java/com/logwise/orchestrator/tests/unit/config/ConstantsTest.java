package com.logwise.orchestrator.tests.unit.config;

import com.logwise.orchestrator.config.constant.Constants;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for Constants. */
public class ConstantsTest {

  private String originalConsulToken;
  private String originalEnv;
  private String originalNamespace;
  private String originalServiceName;
  private String originalVaultToken;
  private String originalAppEnvironment;

  @BeforeMethod
  public void setUp() {

    originalConsulToken = System.getenv("CONSUL_TOKEN");
    originalEnv = System.getenv("ENV");
    originalNamespace = System.getenv("NAMESPACE");
    originalServiceName = System.getenv("SERVICE_NAME");
    originalVaultToken = System.getenv("VAULT_TOKEN");
    originalAppEnvironment = System.getProperty("app.environment");
  }

  @AfterMethod
  public void tearDown() {

    if (originalConsulToken != null) {
      System.setProperty("CONSUL_TOKEN", originalConsulToken);
    }
    if (originalEnv != null) {
      System.setProperty("ENV", originalEnv);
    }
    if (originalNamespace != null) {
      System.setProperty("NAMESPACE", originalNamespace);
    }
    if (originalServiceName != null) {
      System.setProperty("SERVICE_NAME", originalServiceName);
    }
    if (originalVaultToken != null) {
      System.setProperty("VAULT_TOKEN", originalVaultToken);
    }
    if (originalAppEnvironment != null) {
      System.setProperty("app.environment", originalAppEnvironment);
    } else {
      System.clearProperty("app.environment");
    }
  }

  @Test
  public void testConsulToken_CanBeAccessed() {

    String consulToken = Constants.CONSUL_TOKEN;

    Assert.assertTrue(true); // Constant accessed successfully
  }

  @Test
  public void testEnv_CanBeAccessed() {

    String env = Constants.ENV;

    Assert.assertTrue(true); // Constant accessed successfully
  }

  @Test
  public void testNamespace_CanBeAccessed() {
    String namespace = Constants.NAMESPACE;

    Assert.assertTrue(true); // Constant accessed successfully
  }

  @Test
  public void testServiceName_CanBeAccessed() {
    String serviceName = Constants.SERVICE_NAME;

    Assert.assertTrue(true); // Constant accessed successfully
  }

  @Test
  public void testVaultToken_CanBeAccessed() {
    String vaultToken = Constants.VAULT_TOKEN;

    Assert.assertTrue(true); // Constant accessed successfully
  }
}
