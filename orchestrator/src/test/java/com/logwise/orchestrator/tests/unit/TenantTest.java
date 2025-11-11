package com.logwise.orchestrator.tests.unit;

import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.error.ServiceError;
import com.logwise.orchestrator.rest.exception.RestException;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for Tenant enum. */
public class TenantTest {

  @Test
  public void testFromValue_WithValidTenant_ReturnsTenant() {

    Assert.assertEquals(Tenant.fromValue("ABC"), Tenant.ABC);
  }

  @Test
  public void testGetValue_ReturnsCorrectValue() {
    Assert.assertEquals(Tenant.ABC.getValue(), "ABC");
  }

  @Test(expectedExceptions = RestException.class)
  public void testFromValue_WithInvalidTenant_ThrowsRestException() {
    Tenant.fromValue("Invalid-Tenant");
  }

  @Test(expectedExceptions = RestException.class)
  public void testFromValue_WithNullValue_ThrowsRestException() {
    Tenant.fromValue(null);
  }

  @Test(expectedExceptions = RestException.class)
  public void testFromValue_WithEmptyString_ThrowsRestException() {
    Tenant.fromValue("");
  }

  @Test
  public void testValues_ReturnsAllTenants() {
    Tenant[] tenants = Tenant.values();
    Assert.assertEquals(tenants.length, 1);
    Assert.assertEquals(tenants[0], Tenant.ABC);
  }

  @Test
  public void testFromValue_WithCaseSensitiveMismatch_ThrowsRestException() {
    try {
      Tenant.fromValue("abc");
      Assert.fail("Should have thrown RestException for case mismatch");
    } catch (RestException e) {
      Assert.assertNotNull(e.getError());
      Assert.assertEquals(e.getError().getCode(), ServiceError.INVALID_TENANT.getErrorCode());
    }
  }
}
