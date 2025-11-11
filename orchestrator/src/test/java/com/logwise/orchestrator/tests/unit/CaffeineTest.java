package com.logwise.orchestrator.tests.unit;

import com.logwise.orchestrator.CaffeineCacheFactory;
import com.logwise.orchestrator.CaffeineConfig;
import java.util.concurrent.TimeUnit;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for CaffeineCacheFactory and CaffeineConfig. */
public class CaffeineTest {

  @Test
  public void testCaffeineConfig_DefaultValues_AreSet() {

    CaffeineConfig config = new CaffeineConfig();

    Assert.assertFalse(config.getWeakKeys());
    Assert.assertFalse(config.getWeakValues());
    Assert.assertFalse(config.getSoftValues());
  }

  @Test
  public void testCaffeineConfig_SetRefreshAfterWriteSeconds_SetsDurationAndTimeUnit() {

    CaffeineConfig config = new CaffeineConfig();

    config.setRefreshAfterWriteSeconds(60L);

    Assert.assertEquals(config.getRefreshAfterWriteDuration(), Long.valueOf(60L));
    Assert.assertEquals(config.getRefreshAfterWriteTimeUnit(), TimeUnit.SECONDS);
  }

  @Test
  public void testCaffeineConfig_SetExpireAfterWriteSeconds_SetsDurationAndTimeUnit() {

    CaffeineConfig config = new CaffeineConfig();

    config.setExpireAfterWriteSeconds(120L);

    Assert.assertEquals(config.getExpireAfterWriteDuration(), Long.valueOf(120L));
    Assert.assertEquals(config.getExpireAfterWriteTimeUnit(), TimeUnit.SECONDS);
  }

  @Test
  public void testCaffeineConfig_SetExpireAfterAccessSeconds_SetsDurationAndTimeUnit() {

    CaffeineConfig config = new CaffeineConfig();

    config.setExpireAfterAccessSeconds(180L);

    Assert.assertEquals(config.getExpireAfterAccessDuration(), Long.valueOf(180L));
    Assert.assertEquals(config.getExpireAfterAccessTimeUnit(), TimeUnit.SECONDS);
  }

  @Test
  public void
      testCaffeineConfig_SetRefreshAfterWriteDuration_WithExistingSeconds_ThrowsException() {

    CaffeineConfig config = new CaffeineConfig();
    config.setRefreshAfterWriteSeconds(60L);

    try {
      config.setRefreshAfterWriteDuration(120L);
      Assert.fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("cannot be set together"));
    }
  }

  @Test
  public void testCaffeineConfig_SetExpireAfterWriteDuration_WithExistingSeconds_ThrowsException() {

    CaffeineConfig config = new CaffeineConfig();
    config.setExpireAfterWriteSeconds(60L);

    try {
      config.setExpireAfterWriteDuration(120L);
      Assert.fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("cannot be set together"));
    }
  }

  @Test
  public void
      testCaffeineConfig_SetExpireAfterAccessDuration_WithExistingSeconds_ThrowsException() {

    CaffeineConfig config = new CaffeineConfig();
    config.setExpireAfterAccessSeconds(60L);

    try {
      config.setExpireAfterAccessDuration(120L);
      Assert.fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("cannot be set together"));
    }
  }

  @Test
  public void
      testCaffeineConfig_SetRefreshAfterWriteTimeUnit_WithExistingSeconds_ThrowsException() {

    CaffeineConfig config = new CaffeineConfig();
    config.setRefreshAfterWriteSeconds(60L);

    try {
      config.setRefreshAfterWriteTimeUnit(TimeUnit.MINUTES);
      Assert.fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      Assert.assertTrue(e.getMessage().contains("cannot be set together"));
    }
  }

  @Test
  public void testCaffeineConfig_Setters_Work() {

    CaffeineConfig config = new CaffeineConfig();

    config.setInitialCapacity(100);
    config.setMaximumSize(1000);
    config.setWeakKeys(true);
    config.setWeakValues(true);
    config.setSoftValues(true);

    Assert.assertEquals(config.getInitialCapacity(), Integer.valueOf(100));
    Assert.assertEquals(config.getMaximumSize(), Integer.valueOf(1000));
    Assert.assertTrue(config.getWeakKeys());
    Assert.assertTrue(config.getWeakValues());
    Assert.assertTrue(config.getSoftValues());
  }

  @Test
  public void testCaffeineCacheFactory_ClassExists() {

    Assert.assertNotNull(CaffeineCacheFactory.class);
  }
}
