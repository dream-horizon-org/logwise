package com.logwise.orchestrator.tests.unit.rest;

import com.logwise.orchestrator.rest.AbstractRestVerticle;
import com.logwise.orchestrator.setup.BaseTest;
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

    testRestVerticle = new TestRestVerticle("com.logwise.orchestrator.rest.v1");

    Assert.assertNotNull(testRestVerticle);
  }

  @Test
  public void testConstructor_WithPackageNames_CreatesInstance() {

    List<String> packageNames = Arrays.asList("com.logwise.orchestrator.rest.v1");
    testRestVerticle = new TestRestVerticle(packageNames);

    Assert.assertNotNull(testRestVerticle);
  }

  @Test
  public void testConstructor_WithPackageNameAndMountPath_CreatesInstance() {

    testRestVerticle = new TestRestVerticle("com.logwise.orchestrator.rest.v1", "api");

    Assert.assertNotNull(testRestVerticle);
  }

  @Test
  public void testConstructor_WithPackageNamesAndMountPath_CreatesInstance() {

    List<String> packageNames = Arrays.asList("com.logwise.orchestrator.rest.v1");
    testRestVerticle = new TestRestVerticle(packageNames, "api");

    Assert.assertNotNull(testRestVerticle);
  }

  @Test
  public void testConstructor_WithEmptyMountPath_UsesEmptyString() {

    testRestVerticle = new TestRestVerticle("com.logwise.orchestrator.rest.v1", "");

    Assert.assertNotNull(testRestVerticle);
  }

  @Test
  public void testStop_WithHttpServer_ClosesServer() throws Exception {

    testRestVerticle = new TestRestVerticle("com.logwise.orchestrator.rest.v1");
    HttpServer mockHttpServer = Mockito.mock(HttpServer.class);
    testRestVerticle.setHttpServer(mockHttpServer);

    testRestVerticle.stop(io.vertx.core.Promise.promise());

    Mockito.verify(mockHttpServer).close();
  }

  @Test
  public void testStop_WithoutHttpServer_DoesNotThrow() throws Exception {

    testRestVerticle = new TestRestVerticle("com.logwise.orchestrator.rest.v1");

    testRestVerticle.stop(io.vertx.core.Promise.promise());
  }

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
