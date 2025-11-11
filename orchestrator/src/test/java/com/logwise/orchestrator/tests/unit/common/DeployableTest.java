package com.logwise.orchestrator.tests.unit.common;

import com.logwise.orchestrator.common.app.AppContext;
import com.logwise.orchestrator.common.app.Deployable;
import com.logwise.orchestrator.common.app.VerticleConfig;
import com.logwise.orchestrator.verticle.RestVerticle;
import io.vertx.core.AbstractVerticle;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for Deployable. */
public class DeployableTest {

  @BeforeMethod
  public void setUp() {

    try {
      AppContext.getInstance();
    } catch (Exception e) {

    }
  }

  @Test
  public void testConstructor_WithVerticleClass_CreatesDeployable() {

    Deployable deployable = new Deployable(RestVerticle.class);

    Assert.assertNotNull(deployable);
    Assert.assertEquals(deployable.getVerticleClass(), RestVerticle.class);
  }

  @Test
  public void testConstructor_WithConfigAndVerticleClass_CreatesDeployable() {

    VerticleConfig config = VerticleConfig.builder().instances(2).build();

    Deployable deployable = new Deployable(config, RestVerticle.class);

    Assert.assertNotNull(deployable);
    Assert.assertEquals(deployable.getVerticleClass(), RestVerticle.class);
    Assert.assertEquals(deployable.getConfig(), config);
  }

  @Test
  public void testGetVerticle_WithValidClass_ReturnsVerticle() {

    Deployable deployable = new Deployable(RestVerticle.class);

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      RestVerticle mockVerticle = Mockito.mock(RestVerticle.class);
      AppContext mockAppContext = Mockito.mock(AppContext.class);
      mockedAppContext.when(AppContext::getInstance).thenReturn(mockAppContext);
      Mockito.when(mockAppContext.getInstance(RestVerticle.class)).thenReturn(mockVerticle);

      AbstractVerticle verticle = deployable.getVerticle();

      Assert.assertNotNull(verticle);
      Assert.assertSame(verticle, mockVerticle);
    }
  }

  @Test
  public void testGetConfig_WithDefaultConfig_ReturnsDefault() {

    Deployable deployable = new Deployable(RestVerticle.class);

    VerticleConfig config = deployable.getConfig();

    Assert.assertNotNull(config);
    Assert.assertEquals(config, VerticleConfig.DEFAULT_CONFIG);
  }

  @Test
  public void testGetConfig_WithCustomConfig_ReturnsCustom() {

    VerticleConfig customConfig = VerticleConfig.builder().instances(3).build();
    Deployable deployable = new Deployable(customConfig, RestVerticle.class);

    VerticleConfig config = deployable.getConfig();

    Assert.assertNotNull(config);
    Assert.assertEquals(config, customConfig);
  }

  @Test
  public void testDeprecatedConstructors_Work() {

    AbstractVerticle mockVerticle = Mockito.mock(AbstractVerticle.class);
    VerticleConfig config = VerticleConfig.DEFAULT_CONFIG;

    Deployable deployable1 = new Deployable(mockVerticle);
    Deployable deployable2 = new Deployable(mockVerticle, config);

    Assert.assertNotNull(deployable1);
    Assert.assertNotNull(deployable2);
  }
}
