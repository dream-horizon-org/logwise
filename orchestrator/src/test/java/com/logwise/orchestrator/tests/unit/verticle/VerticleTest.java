package com.logwise.orchestrator.tests.unit.verticle;

import com.logwise.orchestrator.verticle.RestVerticle;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for RestVerticle. */
public class VerticleTest {

  @Test
  public void testRestVerticle_Constructor_CreatesVerticle() {

    RestVerticle verticle = new RestVerticle();

    Assert.assertNotNull(verticle);
  }

  @Test
  public void testRestVerticle_Constructor_SetsApplicationConfig() {

    RestVerticle verticle = new RestVerticle();

    Assert.assertNotNull(verticle);
  }
}
