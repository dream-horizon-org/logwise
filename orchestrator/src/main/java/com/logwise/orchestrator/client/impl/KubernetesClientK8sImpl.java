package com.logwise.orchestrator.client.impl;

import com.logwise.orchestrator.client.KubernetesClient;
import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.config.ApplicationConfig;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Scale;
import io.kubernetes.client.openapi.models.V1ScaleSpec;
import io.kubernetes.client.util.Config;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KubernetesClientK8sImpl implements KubernetesClient {
  @NonFinal ApplicationConfig.KubernetesConfig kubernetesConfig;
  @NonFinal ApiClient apiClient;
  @NonFinal AppsV1Api appsV1Api;
  private static final String DEFAULT_NAMESPACE = "logwise";
  private static final String DEFAULT_DEPLOYMENT_NAME = "spark-worker";

  @Override
  public Completable rxConnect(ApplicationConfig.KubernetesConfig config) {
    this.kubernetesConfig = config;
    if (kubernetesConfig != null) {
      log.debug("Connecting to Kubernetes with config: {}", config);
      return Completable.fromAction(
          () -> {
            try {
              // Config.defaultClient() automatically detects:
              // 1. In-cluster config (when running in Kubernetes pod)
              // 2. Kubeconfig from ~/.kube/config or KUBECONFIG env var (when running locally/kind)
              this.apiClient = Config.defaultClient();
              this.appsV1Api = new AppsV1Api(apiClient);
              log.info(
                  "Successfully initialized Kubernetes client (works with kind/local and in-cluster)");
            } catch (IOException e) {
              log.error(
                  "Failed to initialize Kubernetes client. Make sure kubectl is configured or running in-cluster.",
                  e);
              throw new RuntimeException("Failed to initialize Kubernetes client", e);
            }
          });
    } else {
      log.info("Kubernetes config is not present");
    }
    return Completable.complete();
  }

  @Override
  public Completable scaleDeployment(String deploymentName, String namespace, int replicas) {
    // Validate input parameters
    if (replicas < 0) {
      log.error("Invalid replica count: {}. Replica count must be non-negative.", replicas);
      return Completable.error(
          new IllegalArgumentException(
              String.format(
                  "Invalid replica count: %d. Replica count must be non-negative.", replicas)));
    }

    String finalDeploymentName =
        deploymentName != null && !deploymentName.trim().isEmpty()
            ? deploymentName.trim()
            : DEFAULT_DEPLOYMENT_NAME;
    String finalNamespace =
        namespace != null && !namespace.trim().isEmpty() ? namespace.trim() : DEFAULT_NAMESPACE;

    log.info(
        "Scaling deployment '{}' in namespace '{}' to {} replicas",
        finalDeploymentName,
        finalNamespace,
        replicas);

    if (appsV1Api == null) {
      log.error("Kubernetes API client is not initialized. Call rxConnect() first.");
      return Completable.error(
          new IllegalStateException(
              "Kubernetes API client is not initialized. Call rxConnect() first."));
    }

    return Completable.fromFuture(
        CompletableFuture.supplyAsync(
            () -> {
              try {
                // Get current scale to preserve metadata
                log.debug(
                    "Reading current scale for deployment '{}' in namespace '{}'",
                    finalDeploymentName,
                    finalNamespace);
                V1Scale currentScale =
                    appsV1Api
                        .readNamespacedDeploymentScale(finalDeploymentName, finalNamespace)
                        .execute();

                if (currentScale == null) {
                  throw new RuntimeException(
                      String.format(
                          "Failed to read scale for deployment '%s' in namespace '%s': scale object is null",
                          finalDeploymentName, finalNamespace));
                }

                // Check current replica count
                Integer currentReplicas =
                    currentScale.getSpec() != null && currentScale.getSpec().getReplicas() != null
                        ? currentScale.getSpec().getReplicas()
                        : 0;
                log.info("Current replicas: {}", currentReplicas);
                if (currentReplicas != null && currentReplicas == replicas) {
                  log.info(
                      "Deployment '{}' in namespace '{}' already has {} replicas. No scaling needed.",
                      finalDeploymentName,
                      finalNamespace,
                      replicas);
                  return null;
                }

                log.info(
                    "Scaling deployment '{}' in namespace '{}' from {} to {} replicas",
                    finalDeploymentName,
                    finalNamespace,
                    currentReplicas,
                    replicas);

                // Update scale specification
                V1ScaleSpec scaleSpec = new V1ScaleSpec();
                scaleSpec.setReplicas(replicas);
                currentScale.setSpec(scaleSpec);

                // Apply the updated scale
                log.debug(
                    "Applying scale update for deployment '{}' in namespace '{}'",
                    finalDeploymentName,
                    finalNamespace);
                appsV1Api
                    .replaceNamespacedDeploymentScale(
                        finalDeploymentName, finalNamespace, currentScale)
                    .execute();

                log.info(
                    "Successfully scaled deployment '{}' in namespace '{}' to {} replicas",
                    finalDeploymentName,
                    finalNamespace,
                    replicas);
                return null;
              } catch (ApiException e) {
                String errorMessage =
                    String.format(
                        "Failed to scale deployment '%s' in namespace '%s' to %d replicas. HTTP status: %d, Response body: %s",
                        finalDeploymentName,
                        finalNamespace,
                        replicas,
                        e.getCode(),
                        e.getResponseBody());
                log.error(errorMessage, e);

                // Provide more specific error messages based on HTTP status codes
                if (e.getCode() == 404) {
                  throw new RuntimeException(
                      String.format(
                          "Deployment '%s' not found in namespace '%s'. Please verify the deployment name and namespace.",
                          finalDeploymentName, finalNamespace),
                      e);
                } else if (e.getCode() == 403) {
                  throw new RuntimeException(
                      String.format(
                          "Permission denied when scaling deployment '%s' in namespace '%s'. Please check RBAC permissions.",
                          finalDeploymentName, finalNamespace),
                      e);
                } else {
                  throw new RuntimeException(errorMessage, e);
                }
              } catch (Exception e) {
                log.error(
                    "Unexpected error while scaling deployment '{}' in namespace '{}'",
                    finalDeploymentName,
                    finalNamespace,
                    e);
                throw new RuntimeException(
                    String.format(
                        "Unexpected error while scaling deployment '%s': %s",
                        finalDeploymentName, e.getMessage()),
                    e);
              }
            }));
  }

  @Override
  public Single<Integer> getCurrentReplicas(String deploymentName, String namespace) {
    String finalDeploymentName =
        deploymentName != null && !deploymentName.trim().isEmpty()
            ? deploymentName.trim()
            : DEFAULT_DEPLOYMENT_NAME;
    String finalNamespace =
        namespace != null && !namespace.trim().isEmpty() ? namespace.trim() : DEFAULT_NAMESPACE;

    log.debug(
        "Getting current replicas for deployment '{}' in namespace '{}'",
        finalDeploymentName,
        finalNamespace);

    if (appsV1Api == null) {
      log.error("Kubernetes API client is not initialized. Call rxConnect() first.");
      return Single.error(
          new IllegalStateException(
              "Kubernetes API client is not initialized. Call rxConnect() first."));
    }

    return CompletableFutureUtils.toSingle(
        CompletableFuture.supplyAsync(
            () -> {
              try {
                log.debug(
                    "Reading deployment '{}' from namespace '{}'",
                    finalDeploymentName,
                    finalNamespace);
                V1Deployment deployment =
                    appsV1Api
                        .readNamespacedDeployment(finalDeploymentName, finalNamespace)
                        .execute();

                if (deployment == null) {
                  throw new RuntimeException(
                      String.format(
                          "Deployment '%s' not found in namespace '%s'",
                          finalDeploymentName, finalNamespace));
                }

                Integer replicas =
                    deployment.getSpec() != null && deployment.getSpec().getReplicas() != null
                        ? deployment.getSpec().getReplicas()
                        : 0;

                log.info(
                    "Current replicas for deployment '{}' in namespace '{}': {}",
                    finalDeploymentName,
                    finalNamespace,
                    replicas);
                return replicas;
              } catch (ApiException e) {
                String errorMessage =
                    String.format(
                        "Failed to get replicas for deployment '%s' in namespace '%s'. HTTP status: %d, Response body: %s",
                        finalDeploymentName, finalNamespace, e.getCode(), e.getResponseBody());
                log.error(errorMessage, e);

                // Provide more specific error messages based on HTTP status codes
                if (e.getCode() == 404) {
                  throw new RuntimeException(
                      String.format(
                          "Deployment '%s' not found in namespace '%s'. Please verify the deployment name and namespace.",
                          finalDeploymentName, finalNamespace),
                      e);
                } else if (e.getCode() == 403) {
                  throw new RuntimeException(
                      String.format(
                          "Permission denied when reading deployment '%s' in namespace '%s'. Please check RBAC permissions.",
                          finalDeploymentName, finalNamespace),
                      e);
                } else {
                  throw new RuntimeException(errorMessage, e);
                }
              } catch (Exception e) {
                log.error(
                    "Unexpected error while getting replicas for deployment '{}' in namespace '{}'",
                    finalDeploymentName,
                    finalNamespace,
                    e);
                throw new RuntimeException(
                    String.format(
                        "Unexpected error while getting replicas for deployment '%s': %s",
                        finalDeploymentName, e.getMessage()),
                    e);
              }
            }));
  }
}
