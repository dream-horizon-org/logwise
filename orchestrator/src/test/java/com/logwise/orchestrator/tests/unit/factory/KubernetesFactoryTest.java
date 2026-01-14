package com.logwise.orchestrator.tests.unit.factory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.client.KubernetesClient;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.KubernetesFactory;
import com.logwise.orchestrator.util.ApplicationUtils;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.Test;

public class KubernetesFactoryTest {

  @Test
  public void testGetSparkClient_WithValidTenant_ReturnsClient() {
    try (MockedStatic<ApplicationUtils> mockedUtils = mockStatic(ApplicationUtils.class)) {
      KubernetesClient mockClient = mock(KubernetesClient.class);
      mockedUtils
          .when(() -> ApplicationUtils.getGuiceInstance(any(Class.class), anyString()))
          .thenReturn(mockClient);

      KubernetesClient result = KubernetesFactory.getSparkClient(Tenant.ABC);

      Assert.assertNotNull(result);
      Assert.assertEquals(result, mockClient);
      mockedUtils.verify(
          () -> ApplicationUtils.getGuiceInstance(any(Class.class), anyString()), times(1));
    }
  }

  @Test
  public void testGetSparkClient_WithNullReturn_ReturnsNull() {
    try (MockedStatic<ApplicationUtils> mockedUtils = mockStatic(ApplicationUtils.class)) {
      mockedUtils
          .when(() -> ApplicationUtils.getGuiceInstance(any(Class.class), anyString()))
          .thenReturn(null);

      KubernetesClient result = KubernetesFactory.getSparkClient(Tenant.ABC);

      Assert.assertNull(result);
    }
  }
}
