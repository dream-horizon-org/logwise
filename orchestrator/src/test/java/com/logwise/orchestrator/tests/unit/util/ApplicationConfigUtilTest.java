package com.logwise.orchestrator.tests.unit.util;

import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.config.ApplicationConfigProvider;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.testconfig.ApplicationTestConfig;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ApplicationConfigUtil. */
public class ApplicationConfigUtilTest extends BaseTest {

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testGetTenantConfig_WithValidTenant_ReturnsConfig() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigProvider> mockedProvider =
        Mockito.mockStatic(ApplicationConfigProvider.class)) {
      ApplicationConfig mockConfig = new ApplicationConfig();
      ApplicationConfig.TenantConfig tenantConfig =
          ApplicationTestConfig.createMockTenantConfig("ABC");
      mockConfig.setTenants(java.util.Arrays.asList(tenantConfig));
      mockedProvider.when(ApplicationConfigProvider::getApplicationConfig).thenReturn(mockConfig);

      ApplicationConfig.TenantConfig result = ApplicationConfigUtil.getTenantConfig(tenant);

      Assert.assertNotNull(result);
      Assert.assertEquals(result.getName(), tenant.getValue());
    }
  }

  @Test
  public void testGetTenantConfig_WithNonExistentTenant_ReturnsNull() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationConfigProvider> mockedProvider =
        Mockito.mockStatic(ApplicationConfigProvider.class)) {
      ApplicationConfig mockConfig = new ApplicationConfig();
      // Remove the tenant from config
      mockConfig.setTenants(java.util.Collections.emptyList());
      mockedProvider.when(ApplicationConfigProvider::getApplicationConfig).thenReturn(mockConfig);

      ApplicationConfig.TenantConfig result = ApplicationConfigUtil.getTenantConfig(tenant);

      Assert.assertNull(result);
    }
  }

  @Test
  public void testIsAwsObjectStore_WithAwsConfig_ReturnsTrue() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");

    boolean result = ApplicationConfigUtil.isAwsObjectStore(tenantConfig);

    Assert.assertTrue(result);
  }

  @Test
  public void testIsAwsObjectStore_WithNullAwsConfig_ReturnsFalse() {
    ApplicationConfig.TenantConfig tenantConfig =
        ApplicationTestConfig.createMockTenantConfig("ABC");
    // Set AWS config to null
    tenantConfig.getObjectStore().setAws(null);

    boolean result = ApplicationConfigUtil.isAwsObjectStore(tenantConfig);

    Assert.assertFalse(result);
  }
}
