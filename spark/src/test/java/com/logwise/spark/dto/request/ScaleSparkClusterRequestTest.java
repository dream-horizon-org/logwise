package com.logwise.spark.dto.request;

import static org.testng.Assert.*;

import com.logwise.spark.dto.entity.SparkStageHistory;
import org.testng.annotations.Test;

public class ScaleSparkClusterRequestTest {

  @Test
  public void testSettersAndGetters_WorkCorrectly() {
    ScaleSparkClusterRequest request = new ScaleSparkClusterRequest();

    request.setEnableUpScale(true);
    request.setEnableDownScale(false);

    SparkStageHistory history = new SparkStageHistory();
    history.setTenant("ABC");
    history.setInputRecords(1000L);
    request.setSparkStageHistory(history);

    assertTrue(request.getEnableUpScale());
    assertFalse(request.getEnableDownScale());
    assertNotNull(request.getSparkStageHistory());
    assertEquals(request.getSparkStageHistory().getTenant(), "ABC");
  }

  @Test
  public void testSettersAndGetters_WithNullValues_HandlesGracefully() {
    ScaleSparkClusterRequest request = new ScaleSparkClusterRequest();

    request.setEnableUpScale(null);
    request.setEnableDownScale(null);
    request.setSparkStageHistory(null);

    assertNull(request.getEnableUpScale());
    assertNull(request.getEnableDownScale());
    assertNull(request.getSparkStageHistory());
  }

  @Test
  public void testSettersAndGetters_WithBothFlagsEnabled() {
    ScaleSparkClusterRequest request = new ScaleSparkClusterRequest();

    request.setEnableUpScale(true);
    request.setEnableDownScale(true);

    assertTrue(request.getEnableUpScale());
    assertTrue(request.getEnableDownScale());
  }

  @Test
  public void testSettersAndGetters_WithBothFlagsDisabled() {
    ScaleSparkClusterRequest request = new ScaleSparkClusterRequest();

    request.setEnableUpScale(false);
    request.setEnableDownScale(false);

    assertFalse(request.getEnableUpScale());
    assertFalse(request.getEnableDownScale());
  }
}
