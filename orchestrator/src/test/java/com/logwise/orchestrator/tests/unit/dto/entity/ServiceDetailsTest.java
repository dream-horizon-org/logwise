package com.logwise.orchestrator.tests.unit.dto.entity;

import com.logwise.orchestrator.dto.entity.ServiceDetails;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ServiceDetailsTest {

  @Test
  public void testServiceDetails_WithBuilder_SetsAllFields() {
    ServiceDetails details =
        ServiceDetails.builder()
            .serviceName("test-service")
            .tenant("ABC")
            .retentionDays(30)
            .build();

    Assert.assertEquals(details.getServiceName(), "test-service");
    Assert.assertEquals(details.getTenant(), "ABC");
    Assert.assertEquals(details.getRetentionDays(), Integer.valueOf(30));
  }

  @Test
  public void testServiceDetails_Equals_WithSameServiceName_ReturnsTrue() {
    ServiceDetails details1 =
        ServiceDetails.builder().serviceName("test-service").tenant("ABC").build();

    ServiceDetails details2 =
        ServiceDetails.builder().serviceName("test-service").tenant("XYZ").build();

    Assert.assertEquals(details1, details2);
    Assert.assertEquals(details1.hashCode(), details2.hashCode());
  }

  @Test
  public void testServiceDetails_Equals_WithDifferentServiceName_ReturnsFalse() {
    ServiceDetails details1 =
        ServiceDetails.builder().serviceName("test-service-1").tenant("ABC").build();

    ServiceDetails details2 =
        ServiceDetails.builder().serviceName("test-service-2").tenant("ABC").build();

    Assert.assertNotEquals(details1, details2);
  }

  @Test
  public void testServiceDetails_Equals_WithNull_ReturnsFalse() {
    ServiceDetails details = ServiceDetails.builder().serviceName("test-service").build();

    Assert.assertNotEquals(details, null);
  }

  @Test
  public void testServiceDetails_Equals_WithSameInstance_ReturnsTrue() {
    ServiceDetails details = ServiceDetails.builder().serviceName("test-service").build();

    Assert.assertEquals(details, details);
  }

  @Test
  public void testServiceDetails_WithNullRetentionDays_HandlesGracefully() {
    ServiceDetails details =
        ServiceDetails.builder()
            .serviceName("test-service")
            .tenant("ABC")
            .retentionDays(null)
            .build();

    Assert.assertNull(details.getRetentionDays());
    Assert.assertEquals(details.getServiceName(), "test-service");
  }
}
