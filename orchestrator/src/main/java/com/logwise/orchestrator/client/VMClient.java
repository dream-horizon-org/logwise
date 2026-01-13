package com.logwise.orchestrator.client;

import com.logwise.orchestrator.config.ApplicationConfig;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.List;
import java.util.Map;

public interface VMClient {

  Completable rxConnect(ApplicationConfig.VMConfig config);

  Single<Map<String, String>> getInstanceIds(List<String> ips);

  Completable terminateInstances(List<String> instanceIds);

  Single<Map<String, String>> getInstanceIPs(List<String> ids);
}
