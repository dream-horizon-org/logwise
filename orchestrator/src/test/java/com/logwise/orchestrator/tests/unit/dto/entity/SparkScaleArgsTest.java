package com.logwise.orchestrator.tests.unit.dto.entity;

import com.logwise.orchestrator.dto.entity.SparkScaleArgs;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SparkScaleArgsTest {

  @Test
  public void testSparkScaleArgs_WithBuilder_SetsAllFields() {
    SparkScaleArgs args = SparkScaleArgs.builder()
        .minWorkerCount(2)
        .maxWorkerCount(10)
        .workerCount(5)
        .enableDownscale(true)
        .enableUpscale(true)
        .build();
    
    Assert.assertEquals(args.getMinWorkerCount(), 2);
    Assert.assertEquals(args.getMaxWorkerCount(), 10);
    Assert.assertEquals(args.getWorkerCount(), Integer.valueOf(5));
    Assert.assertTrue(args.isEnableDownscale());
    Assert.assertTrue(args.isEnableUpscale());
  }

  @Test
  public void testSparkScaleArgs_WithDefaultValues_UsesDefaults() {
    SparkScaleArgs args = SparkScaleArgs.builder()
        .minWorkerCount(2)
        .maxWorkerCount(10)
        .workerCount(5)
        .build();
    
    Assert.assertTrue(args.isEnableDownscale());
    Assert.assertTrue(args.isEnableUpscale());
    Assert.assertNotNull(args.getMinimumDownscale());
    Assert.assertNotNull(args.getMaximumDownscale());
    Assert.assertNotNull(args.getDownscaleProportion());
    Assert.assertNotNull(args.getMinimumUpscale());
    Assert.assertNotNull(args.getMaximumUpscale());
  }

  @Test
  public void testSparkScaleArgs_WithDisabledScaling_SetsFlags() {
    SparkScaleArgs args = SparkScaleArgs.builder()
        .minWorkerCount(2)
        .maxWorkerCount(10)
        .workerCount(5)
        .enableDownscale(false)
        .enableUpscale(false)
        .build();
    
    Assert.assertFalse(args.isEnableDownscale());
    Assert.assertFalse(args.isEnableUpscale());
  }

  @Test
  public void testSparkScaleArgs_WithNullWorkerCount_HandlesGracefully() {
    SparkScaleArgs args = SparkScaleArgs.builder()
        .minWorkerCount(2)
        .maxWorkerCount(10)
        .workerCount(null)
        .build();
    
    Assert.assertNull(args.getWorkerCount());
    Assert.assertEquals(args.getMinWorkerCount(), 2);
    Assert.assertEquals(args.getMaxWorkerCount(), 10);
  }
}

