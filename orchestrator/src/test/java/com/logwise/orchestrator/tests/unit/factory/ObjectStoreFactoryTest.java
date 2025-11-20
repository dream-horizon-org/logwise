package com.logwise.orchestrator.tests.unit.factory;

import com.logwise.orchestrator.client.ObjectStoreClient;
import com.logwise.orchestrator.constant.ApplicationConstants;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.factory.ObjectStoreFactory;
import com.logwise.orchestrator.util.ApplicationUtils;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for ObjectStoreFactory. */
public class ObjectStoreFactoryTest {

  @Test
  public void testGetClient_WithValidTenant_ReturnsClient() {
    Tenant tenant = Tenant.ABC;
    ObjectStoreClient mockClient = Mockito.mock(ObjectStoreClient.class);

    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      String injectorName =
          ApplicationConstants.OBJECT_STORE_INJECTOR_NAME.apply(tenant.getValue());
      mockedUtils
          .when(() -> ApplicationUtils.getGuiceInstance(ObjectStoreClient.class, injectorName))
          .thenReturn(mockClient);

      ObjectStoreClient result = ObjectStoreFactory.getClient(tenant);

      Assert.assertNotNull(result);
      Assert.assertEquals(result, mockClient);
    }
  }

  @Test
  public void testGetClient_WithNullTenant_ThrowsException() {
    try {
      ObjectStoreFactory.getClient(null);
      Assert.fail("Should have thrown NullPointerException");
    } catch (NullPointerException e) {
      // Expected
    }
  }

  @Test
  public void testGetClient_WithSameTenant_CallsWithCorrectInjectorName() {
    Tenant tenant = Tenant.ABC;
    ObjectStoreClient mockClient = Mockito.mock(ObjectStoreClient.class);

    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      String injectorName =
          ApplicationConstants.OBJECT_STORE_INJECTOR_NAME.apply(tenant.getValue());

      mockedUtils
          .when(() -> ApplicationUtils.getGuiceInstance(ObjectStoreClient.class, injectorName))
          .thenReturn(mockClient);

      ObjectStoreClient result1 = ObjectStoreFactory.getClient(tenant);
      ObjectStoreClient result2 = ObjectStoreFactory.getClient(tenant);

      Assert.assertNotNull(result1);
      Assert.assertNotNull(result2);
      // Both should return the same mock since it's the same tenant
      Assert.assertEquals(result1, result2);
    }
  }

  @Test
  public void testGetClient_WithNonExistentClient_ReturnsNull() {
    Tenant tenant = Tenant.ABC;

    try (MockedStatic<ApplicationUtils> mockedUtils = Mockito.mockStatic(ApplicationUtils.class)) {
      String injectorName =
          ApplicationConstants.OBJECT_STORE_INJECTOR_NAME.apply(tenant.getValue());
      mockedUtils
          .when(() -> ApplicationUtils.getGuiceInstance(ObjectStoreClient.class, injectorName))
          .thenReturn(null);

      ObjectStoreClient result = ObjectStoreFactory.getClient(tenant);

      Assert.assertNull(result);
    }
  }
}
