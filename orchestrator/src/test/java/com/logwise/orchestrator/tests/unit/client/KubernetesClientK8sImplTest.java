package com.logwise.orchestrator.tests.unit.client;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.logwise.orchestrator.client.impl.KubernetesClientK8sImpl;
import com.logwise.orchestrator.common.util.CompletableFutureUtils;
import com.logwise.orchestrator.config.ApplicationConfig;
import com.logwise.orchestrator.helper.HelperTestUtils;
import com.logwise.orchestrator.setup.BaseTest;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.AppsV1Api.APIreadNamespacedDeploymentRequest;
import io.kubernetes.client.openapi.apis.AppsV1Api.APIreadNamespacedDeploymentScaleRequest;
import io.kubernetes.client.openapi.apis.AppsV1Api.APIreplaceNamespacedDeploymentScaleRequest;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1DeploymentSpec;
import io.kubernetes.client.openapi.models.V1Scale;
import io.kubernetes.client.openapi.models.V1ScaleSpec;
import io.reactivex.Completable;
import io.reactivex.Single;
import java.util.concurrent.CompletableFuture;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for KubernetesClientK8sImpl. */
public class KubernetesClientK8sImplTest extends BaseTest {

  private KubernetesClientK8sImpl kubernetesClientK8sImpl;
  private AppsV1Api mockAppsV1Api;
  private ApiClient mockApiClient;
  private ApplicationConfig.KubernetesConfig mockKubernetesConfig;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    kubernetesClientK8sImpl = new KubernetesClientK8sImpl();
    mockAppsV1Api = mock(AppsV1Api.class);
    mockApiClient = mock(ApiClient.class);
    mockKubernetesConfig = mock(ApplicationConfig.KubernetesConfig.class);

    // Inject mock clients using reflection
    HelperTestUtils.setPrivateField(kubernetesClientK8sImpl, "appsV1Api", mockAppsV1Api);
    HelperTestUtils.setPrivateField(kubernetesClientK8sImpl, "apiClient", mockApiClient);
    HelperTestUtils.setPrivateField(
        kubernetesClientK8sImpl, "kubernetesConfig", mockKubernetesConfig);
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testRxConnect_WithValidConfig_CompletesSuccessfully() {
    Completable result = kubernetesClientK8sImpl.rxConnect(mockKubernetesConfig);

    Assert.assertNotNull(result);
    result.blockingAwait();
  }

  @Test
  public void testRxConnect_WithNullConfig_CompletesSuccessfully() {
    Completable result = kubernetesClientK8sImpl.rxConnect(null);

    Assert.assertNotNull(result);
    result.blockingAwait();
  }

  @Test
  public void testScaleDeployment_WithValidReplicas_ScalesSuccessfully() throws Exception {
    String deploymentName = "test-deployment";
    String namespace = "test-namespace";
    int replicas = 5;

    V1Scale currentScale = new V1Scale();
    V1ScaleSpec scaleSpec = new V1ScaleSpec();
    scaleSpec.setReplicas(3);
    currentScale.setSpec(scaleSpec);

    APIreadNamespacedDeploymentScaleRequest mockReadRequest =
        mock(APIreadNamespacedDeploymentScaleRequest.class);
    APIreplaceNamespacedDeploymentScaleRequest mockReplaceRequest =
        mock(APIreplaceNamespacedDeploymentScaleRequest.class);

    when(mockAppsV1Api.readNamespacedDeploymentScale(eq(deploymentName), eq(namespace)))
        .thenReturn(mockReadRequest);
    when(mockReadRequest.execute()).thenReturn(currentScale);
    when(mockAppsV1Api.replaceNamespacedDeploymentScale(
            eq(deploymentName), eq(namespace), any(V1Scale.class)))
        .thenReturn(mockReplaceRequest);
    when(mockReplaceRequest.execute()).thenReturn(currentScale);

    Completable result =
        kubernetesClientK8sImpl.scaleDeployment(deploymentName, namespace, replicas);

    result.blockingAwait();
    verify(mockAppsV1Api, times(1))
        .replaceNamespacedDeploymentScale(eq(deploymentName), eq(namespace), any(V1Scale.class));
  }

  @Test
  public void testScaleDeployment_WithDefaultNames_UsesDefaults() throws Exception {
    int replicas = 3;

    V1Scale currentScale = new V1Scale();
    V1ScaleSpec scaleSpec = new V1ScaleSpec();
    scaleSpec.setReplicas(1);
    currentScale.setSpec(scaleSpec);

    APIreadNamespacedDeploymentScaleRequest mockReadRequest =
        mock(APIreadNamespacedDeploymentScaleRequest.class);
    APIreplaceNamespacedDeploymentScaleRequest mockReplaceRequest =
        mock(APIreplaceNamespacedDeploymentScaleRequest.class);

    when(mockAppsV1Api.readNamespacedDeploymentScale(eq("spark-worker"), eq("logwise")))
        .thenReturn(mockReadRequest);
    when(mockReadRequest.execute()).thenReturn(currentScale);
    when(mockAppsV1Api.replaceNamespacedDeploymentScale(
            eq("spark-worker"), eq("logwise"), any(V1Scale.class)))
        .thenReturn(mockReplaceRequest);
    when(mockReplaceRequest.execute()).thenReturn(currentScale);

    Completable result = kubernetesClientK8sImpl.scaleDeployment(null, null, replicas);

    result.blockingAwait();
    verify(mockAppsV1Api, times(1))
        .replaceNamespacedDeploymentScale(eq("spark-worker"), eq("logwise"), any(V1Scale.class));
  }

  @Test
  public void testScaleDeployment_WithSameReplicas_DoesNotScale() throws Exception {
    String deploymentName = "test-deployment";
    String namespace = "test-namespace";
    int replicas = 3;

    V1Scale currentScale = new V1Scale();
    V1ScaleSpec scaleSpec = new V1ScaleSpec();
    scaleSpec.setReplicas(3);
    currentScale.setSpec(scaleSpec);

    APIreadNamespacedDeploymentScaleRequest mockReadRequest =
        mock(APIreadNamespacedDeploymentScaleRequest.class);

    when(mockAppsV1Api.readNamespacedDeploymentScale(eq(deploymentName), eq(namespace)))
        .thenReturn(mockReadRequest);
    when(mockReadRequest.execute()).thenReturn(currentScale);

    Completable result =
        kubernetesClientK8sImpl.scaleDeployment(deploymentName, namespace, replicas);

    result.blockingAwait();
    verify(mockAppsV1Api, never())
        .replaceNamespacedDeploymentScale(anyString(), anyString(), any(V1Scale.class));
  }

  @Test
  public void testScaleDeployment_WithNegativeReplicas_ReturnsError() {
    String deploymentName = "test-deployment";
    String namespace = "test-namespace";
    int replicas = -1;

    Completable result =
        kubernetesClientK8sImpl.scaleDeployment(deploymentName, namespace, replicas);

    try {
      result.blockingAwait();
      Assert.fail("Should have thrown exception");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("Invalid replica count"));
    }
  }

  @Test
  public void testScaleDeployment_WithNullApiClient_ReturnsError() throws Exception {
    String deploymentName = "test-deployment";
    String namespace = "test-namespace";
    int replicas = 3;

    // Set appsV1Api to null
    HelperTestUtils.setPrivateField(kubernetesClientK8sImpl, "appsV1Api", null);

    Completable result =
        kubernetesClientK8sImpl.scaleDeployment(deploymentName, namespace, replicas);

    try {
      result.blockingAwait();
      Assert.fail("Should have thrown exception");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("not initialized"));
    }
  }

  @Test
  public void testScaleDeployment_WithApiException404_ReturnsError() throws Exception {
    String deploymentName = "test-deployment";
    String namespace = "test-namespace";
    int replicas = 5;

    ApiException apiException = new ApiException(404, "Not Found");

    APIreadNamespacedDeploymentScaleRequest mockReadRequest =
        mock(APIreadNamespacedDeploymentScaleRequest.class);

    when(mockAppsV1Api.readNamespacedDeploymentScale(eq(deploymentName), eq(namespace)))
        .thenReturn(mockReadRequest);
    when(mockReadRequest.execute()).thenThrow(apiException);

    Completable result =
        kubernetesClientK8sImpl.scaleDeployment(deploymentName, namespace, replicas);

    try {
      result.blockingAwait();
      Assert.fail("Should have thrown exception");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("not found"));
    }
  }

  @Test
  public void testScaleDeployment_WithApiException403_ReturnsError() throws Exception {
    String deploymentName = "test-deployment";
    String namespace = "test-namespace";
    int replicas = 5;

    V1Scale currentScale = new V1Scale();
    V1ScaleSpec scaleSpec = new V1ScaleSpec();
    scaleSpec.setReplicas(3);
    currentScale.setSpec(scaleSpec);

    ApiException apiException = new ApiException(403, "Forbidden");

    APIreadNamespacedDeploymentScaleRequest mockReadRequest =
        mock(APIreadNamespacedDeploymentScaleRequest.class);
    APIreplaceNamespacedDeploymentScaleRequest mockReplaceRequest =
        mock(APIreplaceNamespacedDeploymentScaleRequest.class);

    when(mockAppsV1Api.readNamespacedDeploymentScale(eq(deploymentName), eq(namespace)))
        .thenReturn(mockReadRequest);
    when(mockReadRequest.execute()).thenReturn(currentScale);
    when(mockAppsV1Api.replaceNamespacedDeploymentScale(
            eq(deploymentName), eq(namespace), any(V1Scale.class)))
        .thenReturn(mockReplaceRequest);
    when(mockReplaceRequest.execute()).thenThrow(apiException);

    Completable result =
        kubernetesClientK8sImpl.scaleDeployment(deploymentName, namespace, replicas);

    try {
      result.blockingAwait();
      Assert.fail("Should have thrown exception");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("Permission denied"));
    }
  }

  @Test
  public void testGetCurrentReplicas_WithValidDeployment_ReturnsReplicas() throws Exception {
    String deploymentName = "test-deployment";
    String namespace = "test-namespace";
    int expectedReplicas = 5;

    V1Deployment deployment = new V1Deployment();
    V1DeploymentSpec spec = new V1DeploymentSpec();
    spec.setReplicas(expectedReplicas);
    deployment.setSpec(spec);

    APIreadNamespacedDeploymentRequest mockRequest = mock(APIreadNamespacedDeploymentRequest.class);

    when(mockAppsV1Api.readNamespacedDeployment(eq(deploymentName), eq(namespace)))
        .thenReturn(mockRequest);
    when(mockRequest.execute()).thenReturn(deployment);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(
              invocation -> {
                CompletableFuture<?> future = invocation.getArgument(0);
                return Single.fromFuture(future);
              });

      Single<Integer> result =
          kubernetesClientK8sImpl.getCurrentReplicas(deploymentName, namespace);

      Integer replicas = result.blockingGet();
      Assert.assertNotNull(replicas);
      Assert.assertEquals(replicas.intValue(), expectedReplicas);
    }
  }

  @Test
  public void testGetCurrentReplicas_WithDefaultNames_UsesDefaults() throws Exception {
    int expectedReplicas = 3;

    V1Deployment deployment = new V1Deployment();
    V1DeploymentSpec spec = new V1DeploymentSpec();
    spec.setReplicas(expectedReplicas);
    deployment.setSpec(spec);

    APIreadNamespacedDeploymentRequest mockRequest = mock(APIreadNamespacedDeploymentRequest.class);

    when(mockAppsV1Api.readNamespacedDeployment(eq("spark-worker"), eq("logwise")))
        .thenReturn(mockRequest);
    when(mockRequest.execute()).thenReturn(deployment);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(
              invocation -> {
                CompletableFuture<?> future = invocation.getArgument(0);
                return Single.fromFuture(future);
              });

      Single<Integer> result = kubernetesClientK8sImpl.getCurrentReplicas(null, null);

      Integer replicas = result.blockingGet();
      Assert.assertNotNull(replicas);
      Assert.assertEquals(replicas.intValue(), expectedReplicas);
    }
  }

  @Test
  public void testGetCurrentReplicas_WithNullSpec_ReturnsZero() throws Exception {
    String deploymentName = "test-deployment";
    String namespace = "test-namespace";

    V1Deployment deployment = new V1Deployment();
    deployment.setSpec(null);

    APIreadNamespacedDeploymentRequest mockRequest = mock(APIreadNamespacedDeploymentRequest.class);

    when(mockAppsV1Api.readNamespacedDeployment(eq(deploymentName), eq(namespace)))
        .thenReturn(mockRequest);
    when(mockRequest.execute()).thenReturn(deployment);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(
              invocation -> {
                CompletableFuture<?> future = invocation.getArgument(0);
                return Single.fromFuture(future);
              });

      Single<Integer> result =
          kubernetesClientK8sImpl.getCurrentReplicas(deploymentName, namespace);

      Integer replicas = result.blockingGet();
      Assert.assertNotNull(replicas);
      Assert.assertEquals(replicas.intValue(), 0);
    }
  }

  @Test
  public void testGetCurrentReplicas_WithNullApiClient_ReturnsError() throws Exception {
    String deploymentName = "test-deployment";
    String namespace = "test-namespace";

    // Set appsV1Api to null
    HelperTestUtils.setPrivateField(kubernetesClientK8sImpl, "appsV1Api", null);

    Single<Integer> result = kubernetesClientK8sImpl.getCurrentReplicas(deploymentName, namespace);

    try {
      result.blockingGet();
      Assert.fail("Should have thrown exception");
    } catch (Exception e) {
      Assert.assertTrue(e.getMessage().contains("not initialized"));
    }
  }

  @Test
  public void testGetCurrentReplicas_WithApiException404_ReturnsError() throws Exception {
    String deploymentName = "test-deployment";
    String namespace = "test-namespace";

    ApiException apiException = new ApiException(404, "Not Found");

    APIreadNamespacedDeploymentRequest mockRequest = mock(APIreadNamespacedDeploymentRequest.class);

    when(mockAppsV1Api.readNamespacedDeployment(eq(deploymentName), eq(namespace)))
        .thenReturn(mockRequest);
    when(mockRequest.execute()).thenThrow(apiException);

    try (MockedStatic<CompletableFutureUtils> mockedUtils =
        Mockito.mockStatic(CompletableFutureUtils.class)) {
      mockedUtils
          .when(() -> CompletableFutureUtils.toSingle(any(CompletableFuture.class)))
          .thenAnswer(
              invocation -> {
                CompletableFuture<?> future = invocation.getArgument(0);
                return Single.fromFuture(future);
              });

      Single<Integer> result =
          kubernetesClientK8sImpl.getCurrentReplicas(deploymentName, namespace);

      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (Exception e) {
        Assert.assertTrue(e.getMessage().contains("not found"));
      }
    }
  }
}
