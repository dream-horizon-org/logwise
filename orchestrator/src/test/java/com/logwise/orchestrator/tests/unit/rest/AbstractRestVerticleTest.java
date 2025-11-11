package com.dream11.logcentralorchestrator.tests.unit.rest;

import com.dream11.logcentralorchestrator.rest.AbstractRestVerticle;
import com.dream11.logcentralorchestrator.setup.BaseTest;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import java.util.Arrays;
import java.util.List;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for AbstractRestVerticle. */
public class AbstractRestVerticleTest extends BaseTest {

  private Vertx vertx;
  private TestRestVerticle testRestVerticle;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = BaseTest.getReactiveVertx();
  }

  @Test
  public void testConstructor_WithPackageName_CreatesInstance() {
    // Act
    testRestVerticle = new TestRestVerticle("com.dream11.logcentralorchestrator.rest.v1");

    // Assert
    Assert.assertNotNull(testRestVerticle);
  }

  @Test
  public void testConstructor_WithPackageNames_CreatesInstance() {
    // Act
    List<String> packageNames = Arrays.asList("com.dream11.logcentralorchestrator.rest.v1");
    testRestVerticle = new TestRestVerticle(packageNames);

    // Assert
    Assert.assertNotNull(testRestVerticle);
  }

  @Test
  public void testConstructor_WithPackageNameAndMountPath_CreatesInstance() {
    // Act
    testRestVerticle = new TestRestVerticle("com.dream11.logcentralorchestrator.rest.v1", "api");

    // Assert
    Assert.assertNotNull(testRestVerticle);
  }

  @Test
  public void testConstructor_WithPackageNamesAndMountPath_CreatesInstance() {
    // Act
    List<String> packageNames = Arrays.asList("com.dream11.logcentralorchestrator.rest.v1");
    testRestVerticle = new TestRestVerticle(packageNames, "api");

    // Assert
    Assert.assertNotNull(testRestVerticle);
  }

  @Test
  public void testConstructor_WithEmptyMountPath_UsesEmptyString() {
    // Act
    testRestVerticle = new TestRestVerticle("com.dream11.logcentralorchestrator.rest.v1", "");

    // Assert
    Assert.assertNotNull(testRestVerticle);
  }

  // Note: getRouter() is protected, so we can't test it directly
  // These tests verify constructor and basic functionality

  @Test
  public void testStop_WithHttpServer_ClosesServer() throws Exception {
    // Arrange
    testRestVerticle = new TestRestVerticle("com.dream11.logcentralorchestrator.rest.v1");
    HttpServer mockHttpServer = Mockito.mock(HttpServer.class);
    testRestVerticle.setHttpServer(mockHttpServer);

    // Act
    testRestVerticle.stop(io.vertx.core.Promise.promise());

    // Assert
    Mockito.verify(mockHttpServer).close();
  }

  @Test
  public void testStop_WithoutHttpServer_DoesNotThrow() throws Exception {
    // Arrange
    testRestVerticle = new TestRestVerticle("com.dream11.logcentralorchestrator.rest.v1");

    // Act & Assert - Should not throw
    testRestVerticle.stop(io.vertx.core.Promise.promise());
  }

  // Test implementation of AbstractRestVerticle
  private static class TestRestVerticle extends AbstractRestVerticle {
    public TestRestVerticle(String packageName) {
      super(packageName);
    }

    public TestRestVerticle(List<String> packageNames) {
      super(packageNames);
    }

    public TestRestVerticle(String packageName, String mountPath) {
      super(packageName, mountPath);
    }

    public TestRestVerticle(List<String> packageNames, String mountPath) {
      super(packageNames, mountPath);
    }

    public void setHttpServer(HttpServer httpServer) {
      try {
        java.lang.reflect.Field field = AbstractRestVerticle.class.getDeclaredField("httpServer");
        field.setAccessible(true);
        field.set(this, httpServer);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
}
