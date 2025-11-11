package com.logwise.orchestrator.tests.unit.common;

import com.logwise.orchestrator.common.util.MaintenanceUtils;
import com.logwise.orchestrator.setup.BaseTest;
import io.vertx.reactivex.core.Vertx;
import java.util.concurrent.atomic.AtomicBoolean;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for MaintenanceUtils. */
public class MaintenanceUtilsTest extends BaseTest {

  private Vertx vertx;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    vertx = BaseTest.getReactiveVertx();

    MaintenanceUtils.clearMaintenance(vertx);
  }

  @Test
  public void testSetMaintenance_SetsMaintenanceFlag() {

    MaintenanceUtils.setMaintenance(vertx);

    AtomicBoolean isUnderMaintenance = MaintenanceUtils.isUnderMaintenance(vertx);
    Assert.assertTrue(isUnderMaintenance.get());
  }

  @Test
  public void testClearMaintenance_ClearsMaintenanceFlag() {

    MaintenanceUtils.setMaintenance(vertx);

    MaintenanceUtils.clearMaintenance(vertx);

    AtomicBoolean isUnderMaintenance = MaintenanceUtils.isUnderMaintenance(vertx);
    Assert.assertFalse(isUnderMaintenance.get());
  }

  @Test
  public void testIsUnderMaintenance_Initially_ReturnsFalse() {

    AtomicBoolean isUnderMaintenance = MaintenanceUtils.isUnderMaintenance(vertx);

    Assert.assertNotNull(isUnderMaintenance);
    Assert.assertFalse(isUnderMaintenance.get());
  }

  @Test
  public void testIsUnderMaintenance_AfterSetMaintenance_ReturnsTrue() {

    MaintenanceUtils.setMaintenance(vertx);

    AtomicBoolean isUnderMaintenance = MaintenanceUtils.isUnderMaintenance(vertx);

    Assert.assertTrue(isUnderMaintenance.get());
  }

  @Test
  public void testSetMaintenance_ThenClearMaintenance_ReturnsFalse() {

    MaintenanceUtils.setMaintenance(vertx);
    Assert.assertTrue(MaintenanceUtils.isUnderMaintenance(vertx).get());

    MaintenanceUtils.clearMaintenance(vertx);

    Assert.assertFalse(MaintenanceUtils.isUnderMaintenance(vertx).get());
  }
}
