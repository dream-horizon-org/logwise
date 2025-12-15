package com.logwise.orchestrator.client;

import com.logwise.orchestrator.config.ApplicationConfig;
import io.reactivex.Completable;
import io.reactivex.Single;

public interface KubernetesClient {
  Completable rxConnect(ApplicationConfig.KubernetesConfig config);

  Completable scaleDeployment(String deploymentName, String namespace, int replicas);

  Single<Integer> getCurrentReplicas(String deploymentName, String namespace);
}

