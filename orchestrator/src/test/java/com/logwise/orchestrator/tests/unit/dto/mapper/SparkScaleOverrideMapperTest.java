package com.logwise.orchestrator.tests.unit.dto.mapper;

import com.logwise.orchestrator.dto.entity.SparkScaleOverride;
import com.logwise.orchestrator.dto.mapper.SparkScaleOverrideMapper;
import com.logwise.orchestrator.dto.request.UpdateSparkScaleOverrideRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SparkScaleOverrideMapperTest {

  @Test
  public void testToSparkScaleOverride_WithValidRequest_ReturnsOverride() {
    String tenant = "ABC";
    UpdateSparkScaleOverrideRequest request = new UpdateSparkScaleOverrideRequest();
    request.setEnableUpScale(true);
    request.setEnableDownScale(false);

    SparkScaleOverride result = SparkScaleOverrideMapper.toSparkScaleOverride(tenant, request);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getTenant(), tenant);
    Assert.assertEquals(result.getUpscale(), true);
    Assert.assertEquals(result.getDownscale(), false);
  }

  @Test
  public void testToSparkScaleOverride_WithNullFlags_HandlesGracefully() {
    String tenant = "ABC";
    UpdateSparkScaleOverrideRequest request = new UpdateSparkScaleOverrideRequest();
    request.setEnableUpScale(null);
    request.setEnableDownScale(null);

    SparkScaleOverride result = SparkScaleOverrideMapper.toSparkScaleOverride(tenant, request);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getTenant(), tenant);
    Assert.assertNull(result.getUpscale());
    Assert.assertNull(result.getDownscale());
  }

  @Test
  public void testToSparkScaleOverride_WithBothFlagsTrue_ReturnsBothTrue() {
    String tenant = "XYZ";
    UpdateSparkScaleOverrideRequest request = new UpdateSparkScaleOverrideRequest();
    request.setEnableUpScale(true);
    request.setEnableDownScale(true);

    SparkScaleOverride result = SparkScaleOverrideMapper.toSparkScaleOverride(tenant, request);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getTenant(), tenant);
    Assert.assertEquals(result.getUpscale(), true);
    Assert.assertEquals(result.getDownscale(), true);
  }

  @Test
  public void testToSparkScaleOverride_WithBothFlagsFalse_ReturnsBothFalse() {
    String tenant = "XYZ";
    UpdateSparkScaleOverrideRequest request = new UpdateSparkScaleOverrideRequest();
    request.setEnableUpScale(false);
    request.setEnableDownScale(false);

    SparkScaleOverride result = SparkScaleOverrideMapper.toSparkScaleOverride(tenant, request);

    Assert.assertNotNull(result);
    Assert.assertEquals(result.getTenant(), tenant);
    Assert.assertEquals(result.getUpscale(), false);
    Assert.assertEquals(result.getDownscale(), false);
  }
}
