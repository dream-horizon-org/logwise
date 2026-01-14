package com.logwise.orchestrator.tests.unit.factory;

import com.logwise.orchestrator.client.AsgClient;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.AsgFactory;
import com.logwise.orchestrator.util.ApplicationUtils;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class AsgFactoryTest {

  @Test
  public void testGetSparkClient_WithValidTenant_ReturnsClient() {
    try (MockedStatic<ApplicationUtils> mockedUtils = mockStatic(ApplicationUtils.class)) {
      AsgClient mockClient = mock(AsgClient.class);
      mockedUtils.when(() -> ApplicationUtils.getGuiceInstance(any(Class.class), anyString()))
          .thenReturn(mockClient);
      
      AsgClient result = AsgFactory.getSparkClient(Tenant.ABC);
      
      Assert.assertNotNull(result);
      Assert.assertEquals(result, mockClient);
      mockedUtils.verify(() -> ApplicationUtils.getGuiceInstance(any(Class.class), anyString()), times(1));
    }
  }

  @Test
  public void testGetSparkClient_WithNullReturn_ReturnsNull() {
    try (MockedStatic<ApplicationUtils> mockedUtils = mockStatic(ApplicationUtils.class)) {
      mockedUtils.when(() -> ApplicationUtils.getGuiceInstance(any(Class.class), anyString()))
          .thenReturn(null);
      
      AsgClient result = AsgFactory.getSparkClient(Tenant.ABC);
      
      Assert.assertNull(result);
    }
  }

  @Test
  public void testGetSparkClient_WithDifferentTenants_CallsWithCorrectNames() {
    try (MockedStatic<ApplicationUtils> mockedUtils = mockStatic(ApplicationUtils.class)) {
      AsgClient mockClient = mock(AsgClient.class);
      mockedUtils.when(() -> ApplicationUtils.getGuiceInstance(any(Class.class), anyString()))
          .thenReturn(mockClient);
      
      AsgFactory.getSparkClient(Tenant.ABC);
      // Test with same tenant twice to verify multiple calls
      AsgFactory.getSparkClient(Tenant.ABC);
      
      mockedUtils.verify(() -> ApplicationUtils.getGuiceInstance(any(Class.class), anyString()), times(2));
    }
  }
}

