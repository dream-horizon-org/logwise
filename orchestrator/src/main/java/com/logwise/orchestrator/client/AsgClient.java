package com.logwise.orchestrator.client;

import com.logwise.orchestrator.config.ApplicationConfig;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;

public interface AsgClient {
  Completable rxConnect(ApplicationConfig.AsgConfig config);

  Completable updateDesiredCapacity(String asgName, int desiredCapacity);

  Completable removeInstances(String asgName, List<String> instanceIds, boolean decrementCount);

  Single<List<String>> getAllInServiceVmIdInAsg(String asgName);
}
