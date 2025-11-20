package com.logwise.orchestrator.tests.unit;

import static org.mockito.Mockito.*;

import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.config.ApplicationConfigProvider;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import java.util.ArrayList;
import java.util.List;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for ApplicationConfigUtil. */
public class ApplicationConfigUtilTest {

  private ApplicationConfig mockConfig;

  @BeforeMethod
  public void setUp() {
    mockConfig = mock(ApplicationConfig.class);
  }

  @Test
  public void testGetTenantConfig_WithValidTenant_ReturnsTenantConfig() {
    ApplicationConfig.TenantConfig tenantConfig = mock(ApplicationConfig.TenantConfig.class);
    when(tenantConfig.getName()).thenReturn("ABC");

    List<ApplicationConfig.TenantConfig> tenants = new ArrayList<>();
    tenants.add(tenantConfig);
    when(mockConfig.getTenants()).thenReturn(tenants);

    try (MockedStatic<ApplicationConfigProvider> mockedProvider =
        Mockito.mockStatic(ApplicationConfigProvider.class)) {
      mockedProvider.when(ApplicationConfigProvider::getApplicationConfig).thenReturn(mockConfig);

      ApplicationConfig.TenantConfig result = ApplicationConfigUtil.getTenantConfig(Tenant.ABC);

      Assert.assertNotNull(result);
      Assert.assertEquals(result.getName(), "ABC");
    }
  }

  @Test
  public void testGetTenantConfig_WithInvalidTenant_ReturnsNull() {
    ApplicationConfig.TenantConfig tenantConfig = mock(ApplicationConfig.TenantConfig.class);
    when(tenantConfig.getName()).thenReturn("ABCD");

    List<ApplicationConfig.TenantConfig> tenants = new ArrayList<>();
    tenants.add(tenantConfig);
    when(mockConfig.getTenants()).thenReturn(tenants);

    try (MockedStatic<ApplicationConfigProvider> mockedProvider =
        Mockito.mockStatic(ApplicationConfigProvider.class)) {
      mockedProvider.when(ApplicationConfigProvider::getApplicationConfig).thenReturn(mockConfig);

      ApplicationConfig.TenantConfig result = ApplicationConfigUtil.getTenantConfig(Tenant.ABC);

      Assert.assertNull(result);
    }
  }

  @Test
  public void testGetTenantConfig_WithEmptyTenants_ReturnsNull() {
    when(mockConfig.getTenants()).thenReturn(new ArrayList<>());

    try (MockedStatic<ApplicationConfigProvider> mockedProvider =
        Mockito.mockStatic(ApplicationConfigProvider.class)) {
      mockedProvider.when(ApplicationConfigProvider::getApplicationConfig).thenReturn(mockConfig);

      ApplicationConfig.TenantConfig result = ApplicationConfigUtil.getTenantConfig(Tenant.ABC);

      Assert.assertNull(result);
    }
  }

  @Test
  public void testIsAwsObjectStore_WithAwsObjectStore_ReturnsTrue() {
    ApplicationConfig.TenantConfig tenantConfig = mock(ApplicationConfig.TenantConfig.class);
    ApplicationConfig.ObjectStoreConfig objectStoreConfig =
        mock(ApplicationConfig.ObjectStoreConfig.class);
    ApplicationConfig.S3Config s3Config = mock(ApplicationConfig.S3Config.class);

    when(tenantConfig.getObjectStore()).thenReturn(objectStoreConfig);
    when(objectStoreConfig.getAws()).thenReturn(s3Config);

    boolean result = ApplicationConfigUtil.isAwsObjectStore(tenantConfig);

    Assert.assertTrue(result);
  }

  @Test
  public void testIsAwsObjectStore_WithNullAws_ReturnsFalse() {
    ApplicationConfig.TenantConfig tenantConfig = mock(ApplicationConfig.TenantConfig.class);
    ApplicationConfig.ObjectStoreConfig objectStoreConfig =
        mock(ApplicationConfig.ObjectStoreConfig.class);

    when(tenantConfig.getObjectStore()).thenReturn(objectStoreConfig);
    when(objectStoreConfig.getAws()).thenReturn(null);

    boolean result = ApplicationConfigUtil.isAwsObjectStore(tenantConfig);

    Assert.assertFalse(result);
  }

  @Test
  public void testIsAwsObjectStore_WithNullObjectStore_ReturnsFalse() {
    ApplicationConfig.TenantConfig tenantConfig = mock(ApplicationConfig.TenantConfig.class);
    when(tenantConfig.getObjectStore()).thenReturn(null);

    try {
      ApplicationConfigUtil.isAwsObjectStore(tenantConfig);
      Assert.fail("Should have thrown NullPointerException");
    } catch (NullPointerException e) {
    }
  }
}
