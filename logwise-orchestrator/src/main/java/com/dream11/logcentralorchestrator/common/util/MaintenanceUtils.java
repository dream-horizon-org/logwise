package com.dream11.logcentralorchestrator.common.util;

import io.vertx.reactivex.core.Vertx;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public final class MaintenanceUtils {

  private static final String MAINTENANCE_FLAG = "__maintenance_flag";

  public static void setMaintenance(Vertx vertx) {
    log.info("Turning on maintenance mode");
    val isUnderMaintenance =
        com.dream11.logcentralorchestrator.common.util.SharedDataUtils.getOrCreate(
            vertx, MAINTENANCE_FLAG, MaintenanceUtils::getAtomicBoolean);
    isUnderMaintenance.set(true);
  }

  public static AtomicBoolean isUnderMaintenance(Vertx vertx) {
    return com.dream11.logcentralorchestrator.common.util.SharedDataUtils.getOrCreate(
        vertx, MAINTENANCE_FLAG, MaintenanceUtils::getAtomicBoolean);
  }

  public static void clearMaintenance(Vertx vertx) {
    log.info("Turning off maintenance mode");
    val isUnderMaintenance =
        SharedDataUtils.getOrCreate(vertx, MAINTENANCE_FLAG, MaintenanceUtils::getAtomicBoolean);
    isUnderMaintenance.set(false);
  }

  private static AtomicBoolean getAtomicBoolean() {
    val atomicBoolean = new AtomicBoolean();
    atomicBoolean.set(false);
    return atomicBoolean;
  }
}
