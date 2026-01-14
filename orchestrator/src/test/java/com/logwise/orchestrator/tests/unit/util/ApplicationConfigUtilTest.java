package com.logwise.orchestrator.tests.unit.util;

import static org.mockito.Mockito.*;

import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.config.ApplicationConfigProvider;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.util.ApplicationConfigUtil;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ApplicationConfigUtilTest {

  private ApplicationConfig mockApplicationConfig;
  private ApplicationConfig.TenantConfig mockTenantConfig;

  @BeforeMethod
  public void setUp() {
    mockApplicationConfig = mock(ApplicationConfig.class);
    mockTenantConfig = mock(ApplicationConfig.TenantConfig.class);
  }

  @Test
  public void testGetTenantConfig_WithValidTenant_ReturnsConfig() {
    when(mockTenantConfig.getName()).thenReturn("ABC");
    when(mockApplicationConfig.getTenants()).thenReturn(java.util.Arrays.asList(mockTenantConfig));

    try (MockedStatic<ApplicationConfigProvider> mockedProvider =
        mockStatic(ApplicationConfigProvider.class)) {
      mockedProvider
          .when(ApplicationConfigProvider::getApplicationConfig)
          .thenReturn(mockApplicationConfig);

      ApplicationConfig.TenantConfig result = ApplicationConfigUtil.getTenantConfig(Tenant.ABC);

      Assert.assertNotNull(result);
      Assert.assertEquals(result.getName(), "ABC");
    }
  }

  @Test
  public void testGetTenantConfig_WithInvalidTenant_ReturnsNull() {
    when(mockTenantConfig.getName()).thenReturn("XYZ");
    when(mockApplicationConfig.getTenants()).thenReturn(java.util.Arrays.asList(mockTenantConfig));

    try (MockedStatic<ApplicationConfigProvider> mockedProvider =
        mockStatic(ApplicationConfigProvider.class)) {
      mockedProvider
          .when(ApplicationConfigProvider::getApplicationConfig)
          .thenReturn(mockApplicationConfig);

      ApplicationConfig.TenantConfig result = ApplicationConfigUtil.getTenantConfig(Tenant.ABC);

      Assert.assertNull(result);
    }
  }

  @Test
  public void testIsAwsObjectStore_WithAwsConfig_ReturnsTrue() {
    ApplicationConfig.ObjectStoreConfig mockObjectStoreConfig =
        mock(ApplicationConfig.ObjectStoreConfig.class);
    ApplicationConfig.S3Config mockAwsConfig = mock(ApplicationConfig.S3Config.class);

    when(mockTenantConfig.getObjectStore()).thenReturn(mockObjectStoreConfig);
    when(mockObjectStoreConfig.getAws()).thenReturn(mockAwsConfig);

    boolean result = ApplicationConfigUtil.isAwsObjectStore(mockTenantConfig);

    Assert.assertTrue(result);
  }

  @Test
  public void testIsAwsObjectStore_WithoutAwsConfig_ReturnsFalse() {
    ApplicationConfig.ObjectStoreConfig mockObjectStoreConfig =
        mock(ApplicationConfig.ObjectStoreConfig.class);

    when(mockTenantConfig.getObjectStore()).thenReturn(mockObjectStoreConfig);
    when(mockObjectStoreConfig.getAws()).thenReturn(null);

    boolean result = ApplicationConfigUtil.isAwsObjectStore(mockTenantConfig);

    Assert.assertFalse(result);
  }

  @Test
  public void testIsAwsSparkCluster_WithAsgClusterType_ReturnsTrue() {
    ApplicationConfig.SparkConfig mockSparkConfig = mock(ApplicationConfig.SparkConfig.class);
    ApplicationConfig.SparkClusterConfig mockClusterConfig =
        mock(ApplicationConfig.SparkClusterConfig.class);

    when(mockTenantConfig.getSpark()).thenReturn(mockSparkConfig);
    when(mockSparkConfig.getCluster()).thenReturn(mockClusterConfig);
    when(mockClusterConfig.getClusterType()).thenReturn("asg");

    boolean result = ApplicationConfigUtil.isAwsSparkCluster(mockTenantConfig);

    Assert.assertTrue(result);
  }

  @Test
  public void testIsAwsSparkCluster_WithAsgConfigFallback_ReturnsTrue() {
    ApplicationConfig.SparkConfig mockSparkConfig = mock(ApplicationConfig.SparkConfig.class);
    ApplicationConfig.SparkClusterConfig mockClusterConfig =
        mock(ApplicationConfig.SparkClusterConfig.class);
    ApplicationConfig.AsgConfig mockAsgConfig = mock(ApplicationConfig.AsgConfig.class);
    ApplicationConfig.AsgAwsConfig mockAwsAsgConfig = mock(ApplicationConfig.AsgAwsConfig.class);

    when(mockTenantConfig.getSpark()).thenReturn(mockSparkConfig);
    when(mockSparkConfig.getCluster()).thenReturn(mockClusterConfig);
    when(mockClusterConfig.getClusterType()).thenReturn(null);
    when(mockClusterConfig.getAsg()).thenReturn(mockAsgConfig);
    when(mockAsgConfig.getAws()).thenReturn(mockAwsAsgConfig);

    boolean result = ApplicationConfigUtil.isAwsSparkCluster(mockTenantConfig);

    Assert.assertTrue(result);
  }

  @Test
  public void testIsKubernetesSparkCluster_WithKubernetesClusterType_ReturnsTrue() {
    ApplicationConfig.SparkConfig mockSparkConfig = mock(ApplicationConfig.SparkConfig.class);
    ApplicationConfig.SparkClusterConfig mockClusterConfig =
        mock(ApplicationConfig.SparkClusterConfig.class);

    when(mockTenantConfig.getSpark()).thenReturn(mockSparkConfig);
    when(mockSparkConfig.getCluster()).thenReturn(mockClusterConfig);
    when(mockClusterConfig.getClusterType()).thenReturn("kubernetes");

    boolean result = ApplicationConfigUtil.isKubernetesSparkCluster(mockTenantConfig);

    Assert.assertTrue(result);
  }

  @Test
  public void testIsKubernetesSparkCluster_WithK8sClusterType_ReturnsTrue() {
    ApplicationConfig.SparkConfig mockSparkConfig = mock(ApplicationConfig.SparkConfig.class);
    ApplicationConfig.SparkClusterConfig mockClusterConfig =
        mock(ApplicationConfig.SparkClusterConfig.class);

    when(mockTenantConfig.getSpark()).thenReturn(mockSparkConfig);
    when(mockSparkConfig.getCluster()).thenReturn(mockClusterConfig);
    when(mockClusterConfig.getClusterType()).thenReturn("k8s");

    boolean result = ApplicationConfigUtil.isKubernetesSparkCluster(mockTenantConfig);

    Assert.assertTrue(result);
  }

  @Test
  public void testIsKubernetesSparkCluster_WithKubernetesConfigFallback_ReturnsTrue() {
    ApplicationConfig.SparkConfig mockSparkConfig = mock(ApplicationConfig.SparkConfig.class);
    ApplicationConfig.SparkClusterConfig mockClusterConfig =
        mock(ApplicationConfig.SparkClusterConfig.class);
    ApplicationConfig.KubernetesConfig mockKubernetesConfig =
        mock(ApplicationConfig.KubernetesConfig.class);

    when(mockTenantConfig.getSpark()).thenReturn(mockSparkConfig);
    when(mockSparkConfig.getCluster()).thenReturn(mockClusterConfig);
    when(mockClusterConfig.getClusterType()).thenReturn(null);
    when(mockClusterConfig.getKubernetes()).thenReturn(mockKubernetesConfig);

    boolean result = ApplicationConfigUtil.isKubernetesSparkCluster(mockTenantConfig);

    Assert.assertTrue(result);
  }

  @Test
  public void testGetVmConfigFromAsgConfig_WithAwsConfig_ReturnsVmConfig() {
    ApplicationConfig.AsgConfig mockAsgConfig = mock(ApplicationConfig.AsgConfig.class);
    ApplicationConfig.AsgAwsConfig mockAwsAsgConfig = mock(ApplicationConfig.AsgAwsConfig.class);

    when(mockAsgConfig.getAws()).thenReturn(mockAwsAsgConfig);
    when(mockAwsAsgConfig.getRegion()).thenReturn("us-east-1");

    ApplicationConfig.VMConfig result =
        ApplicationConfigUtil.getVmConfigFromAsgConfig(mockAsgConfig);

    Assert.assertNotNull(result);
    Assert.assertNotNull(result.getAws());
    Assert.assertEquals(result.getAws().getRegion(), "us-east-1");
  }

  @Test
  public void testGetVmConfigFromAsgConfig_WithoutAwsConfig_ReturnsVmConfig() {
    ApplicationConfig.AsgConfig mockAsgConfig = mock(ApplicationConfig.AsgConfig.class);

    when(mockAsgConfig.getAws()).thenReturn(null);

    ApplicationConfig.VMConfig result =
        ApplicationConfigUtil.getVmConfigFromAsgConfig(mockAsgConfig);

    Assert.assertNotNull(result);
  }
}
