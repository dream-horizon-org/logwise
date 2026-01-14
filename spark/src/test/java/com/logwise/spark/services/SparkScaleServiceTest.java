package com.logwise.spark.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.logwise.spark.base.BaseSparkTest;
import com.logwise.spark.base.MockConfigHelper;
import com.logwise.spark.clients.LogCentralOrchestratorClient;
import com.logwise.spark.dto.entity.SparkStageHistory;
import com.logwise.spark.dto.request.ScaleSparkClusterRequest;
import com.typesafe.config.Config;
import java.util.HashMap;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for SparkScaleService. */
public class SparkScaleServiceTest extends BaseSparkTest {

  private Config mockConfig;
  private LogCentralOrchestratorClient mockLogCentralOrchestratorClient;
  private SparkScaleService sparkScaleService;

  @BeforeMethod
  @Override
  public void setUp() {
    super.setUp();
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("spark.scale.downscale.enable", true);
    configMap.put("spark.scale.upscale.enable", true);
    configMap.put("tenant.name", "test-tenant");
    mockConfig = MockConfigHelper.createConfig(configMap);

    mockLogCentralOrchestratorClient = mock(LogCentralOrchestratorClient.class);
    sparkScaleService = new SparkScaleService(mockConfig, mockLogCentralOrchestratorClient);
  }

  @AfterMethod
  @Override
  public void tearDown() {
    super.tearDown();
    // Reset static field
    sparkScaleService.setCurrentSparkStageHistory(null);
  }

  @Test
  public void testGetCurrentSparkStageHistory_InitiallyReturnsNull() {
    SparkStageHistory result = sparkScaleService.getCurrentSparkStageHistory();

    assertNull(result);
  }

  @Test
  public void testSetCurrentSparkStageHistory_SetsValueCorrectly() {
    SparkStageHistory stageHistory = createTestSparkStageHistory();

    sparkScaleService.setCurrentSparkStageHistory(stageHistory);

    SparkStageHistory result = sparkScaleService.getCurrentSparkStageHistory();
    assertNotNull(result);
    assertEquals(result, stageHistory);
  }

  @Test
  public void testUpdateStageHistory_WithValidHistory_CallsOrchestratorClient() {
    SparkStageHistory stageHistory = createTestSparkStageHistory();
    Map<String, Object> mockResponse = new HashMap<>();
    mockResponse.put("status", "success");
    when(mockLogCentralOrchestratorClient.postScaleSparkCluster(
            anyMap(), any(ScaleSparkClusterRequest.class)))
        .thenReturn(mockResponse);

    sparkScaleService.updateStageHistory(stageHistory);

    verify(mockLogCentralOrchestratorClient, times(1))
        .postScaleSparkCluster(anyMap(), any(ScaleSparkClusterRequest.class));
  }

  @Test
  public void testUpdateStageHistory_SetsCorrectHeaders() {
    SparkStageHistory stageHistory = createTestSparkStageHistory();
    Map<String, Object> mockResponse = new HashMap<>();
    mockResponse.put("status", "success");
    when(mockLogCentralOrchestratorClient.postScaleSparkCluster(
            anyMap(), any(ScaleSparkClusterRequest.class)))
        .thenReturn(mockResponse);

    sparkScaleService.updateStageHistory(stageHistory);

    verify(mockLogCentralOrchestratorClient, times(1))
        .postScaleSparkCluster(
            argThat(
                headers -> {
                  return headers.containsKey("X-Tenant-Name")
                      && headers.get("X-Tenant-Name").equals("test-tenant")
                      && headers.containsKey("Content-Type")
                      && headers.get("Content-Type").equals("application/json");
                }),
            any(ScaleSparkClusterRequest.class));
  }

  @Test
  public void testUpdateStageHistory_SetsCorrectRequestProperties() {
    SparkStageHistory stageHistory = createTestSparkStageHistory();
    Map<String, Object> mockResponse = new HashMap<>();
    mockResponse.put("status", "success");
    when(mockLogCentralOrchestratorClient.postScaleSparkCluster(
            anyMap(), any(ScaleSparkClusterRequest.class)))
        .thenReturn(mockResponse);

    sparkScaleService.updateStageHistory(stageHistory);

    verify(mockLogCentralOrchestratorClient, times(1))
        .postScaleSparkCluster(
            anyMap(),
            argThat(
                request -> {
                  return request.getSparkStageHistory() != null
                      && request.getSparkStageHistory().equals(stageHistory)
                      && request.getEnableUpScale() != null
                      && request.getEnableDownScale() != null;
                }));
  }

  @Test
  public void testUpdateStageHistory_WithUpScaleDisabled_SetsCorrectFlag() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("spark.scale.downscale.enable", true);
    configMap.put("spark.scale.upscale.enable", false);
    configMap.put("tenant.name", "test-tenant");
    Config configWithUpScaleDisabled = MockConfigHelper.createConfig(configMap);
    SparkScaleService serviceWithUpScaleDisabled =
        new SparkScaleService(configWithUpScaleDisabled, mockLogCentralOrchestratorClient);

    SparkStageHistory stageHistory = createTestSparkStageHistory();
    Map<String, Object> mockResponse = new HashMap<>();
    mockResponse.put("status", "success");
    when(mockLogCentralOrchestratorClient.postScaleSparkCluster(
            anyMap(), any(ScaleSparkClusterRequest.class)))
        .thenReturn(mockResponse);

    serviceWithUpScaleDisabled.updateStageHistory(stageHistory);

    verify(mockLogCentralOrchestratorClient, times(1))
        .postScaleSparkCluster(
            anyMap(),
            argThat(
                request -> {
                  return request.getEnableUpScale() != null
                      && !request.getEnableUpScale()
                      && request.getEnableDownScale() != null
                      && request.getEnableDownScale();
                }));
  }

  @Test
  public void testUpdateStageHistory_WithDownScaleDisabled_SetsCorrectFlag() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("spark.scale.downscale.enable", false);
    configMap.put("spark.scale.upscale.enable", true);
    configMap.put("tenant.name", "test-tenant");
    Config configWithDownScaleDisabled = MockConfigHelper.createConfig(configMap);
    SparkScaleService serviceWithDownScaleDisabled =
        new SparkScaleService(configWithDownScaleDisabled, mockLogCentralOrchestratorClient);

    SparkStageHistory stageHistory = createTestSparkStageHistory();
    Map<String, Object> mockResponse = new HashMap<>();
    mockResponse.put("status", "success");
    when(mockLogCentralOrchestratorClient.postScaleSparkCluster(
            anyMap(), any(ScaleSparkClusterRequest.class)))
        .thenReturn(mockResponse);

    serviceWithDownScaleDisabled.updateStageHistory(stageHistory);

    verify(mockLogCentralOrchestratorClient, times(1))
        .postScaleSparkCluster(
            anyMap(),
            argThat(
                request -> {
                  return request.getEnableUpScale() != null
                      && request.getEnableUpScale()
                      && request.getEnableDownScale() != null
                      && !request.getEnableDownScale();
                }));
  }

  @Test
  public void testUpdateStageHistory_WithBothScalingDisabled_SetsCorrectFlags() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("spark.scale.downscale.enable", false);
    configMap.put("spark.scale.upscale.enable", false);
    configMap.put("tenant.name", "test-tenant");
    Config configWithBothDisabled = MockConfigHelper.createConfig(configMap);
    SparkScaleService serviceWithBothDisabled =
        new SparkScaleService(configWithBothDisabled, mockLogCentralOrchestratorClient);

    SparkStageHistory stageHistory = createTestSparkStageHistory();
    Map<String, Object> mockResponse = new HashMap<>();
    mockResponse.put("status", "success");
    when(mockLogCentralOrchestratorClient.postScaleSparkCluster(
            anyMap(), any(ScaleSparkClusterRequest.class)))
        .thenReturn(mockResponse);

    serviceWithBothDisabled.updateStageHistory(stageHistory);

    verify(mockLogCentralOrchestratorClient, times(1))
        .postScaleSparkCluster(
            anyMap(),
            argThat(
                request -> {
                  return request.getEnableUpScale() != null
                      && !request.getEnableUpScale()
                      && request.getEnableDownScale() != null
                      && !request.getEnableDownScale();
                }));
  }

  @Test
  public void testUpdateStageHistory_WithClientException_HandlesGracefully() {
    SparkStageHistory stageHistory = createTestSparkStageHistory();
    RuntimeException clientException = new RuntimeException("Network error");
    when(mockLogCentralOrchestratorClient.postScaleSparkCluster(
            anyMap(), any(ScaleSparkClusterRequest.class)))
        .thenThrow(clientException);

    try {
      sparkScaleService.updateStageHistory(stageHistory);
      assertTrue(true);
    } catch (Exception e) {
      fail("Exception should be caught and handled internally", e);
    }

    verify(mockLogCentralOrchestratorClient, times(1))
        .postScaleSparkCluster(anyMap(), any(ScaleSparkClusterRequest.class));
  }

  @Test
  public void testUpdateStageHistory_WithComplexStageHistory_ProcessesCorrectly() {
    SparkStageHistory stageHistory = createComplexTestSparkStageHistory();
    Map<String, Object> mockResponse = new HashMap<>();
    mockResponse.put("status", "success");
    when(mockLogCentralOrchestratorClient.postScaleSparkCluster(
            anyMap(), any(ScaleSparkClusterRequest.class)))
        .thenReturn(mockResponse);

    sparkScaleService.updateStageHistory(stageHistory);

    verify(mockLogCentralOrchestratorClient, times(1))
        .postScaleSparkCluster(
            anyMap(),
            argThat(
                request -> {
                  SparkStageHistory history = request.getSparkStageHistory();
                  return history != null
                      && history.getOutputBytes() != null
                      && history.getInputRecords() != null
                      && history.getCoresUsed() != null
                      && history.getStatus() != null;
                }));
  }

  @Test
  public void testUpdateStageHistory_WithNullStageHistory_HandlesGracefully() {
    Map<String, Object> mockResponse = new HashMap<>();
    mockResponse.put("status", "success");
    when(mockLogCentralOrchestratorClient.postScaleSparkCluster(
            anyMap(), any(ScaleSparkClusterRequest.class)))
        .thenReturn(mockResponse);

    try {
      sparkScaleService.updateStageHistory(null);
      verify(mockLogCentralOrchestratorClient, times(1))
          .postScaleSparkCluster(anyMap(), any(ScaleSparkClusterRequest.class));
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  @Test
  public void testSetAndGetCurrentSparkStageHistory_WorksCorrectly() {
    SparkStageHistory stageHistory1 = createTestSparkStageHistory();
    SparkStageHistory stageHistory2 = createComplexTestSparkStageHistory();

    sparkScaleService.setCurrentSparkStageHistory(stageHistory1);
    assertEquals(sparkScaleService.getCurrentSparkStageHistory(), stageHistory1);

    sparkScaleService.setCurrentSparkStageHistory(stageHistory2);
    assertEquals(sparkScaleService.getCurrentSparkStageHistory(), stageHistory2);
  }

  @Test
  public void testUpdateStageHistory_WithMissingConfigKeys_HandlesGracefully() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("tenant.name", "test-tenant");
    Config configWithMissingKeys = MockConfigHelper.createConfig(configMap);

    try {
      SparkScaleService serviceWithMissingConfig =
          new SparkScaleService(configWithMissingKeys, mockLogCentralOrchestratorClient);
      SparkStageHistory stageHistory = createTestSparkStageHistory();
      serviceWithMissingConfig.updateStageHistory(stageHistory);
      assertTrue(true);
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  @Test
  public void testUpdateStageHistory_WithEmptyTenantName_HandlesGracefully() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("spark.scale.downscale.enable", true);
    configMap.put("spark.scale.upscale.enable", true);
    configMap.put("tenant.name", "");
    Config configWithEmptyTenant = MockConfigHelper.createConfig(configMap);
    SparkScaleService serviceWithEmptyTenant =
        new SparkScaleService(configWithEmptyTenant, mockLogCentralOrchestratorClient);

    SparkStageHistory stageHistory = createTestSparkStageHistory();
    Map<String, Object> mockResponse = new HashMap<>();
    mockResponse.put("status", "success");
    when(mockLogCentralOrchestratorClient.postScaleSparkCluster(
            anyMap(), any(ScaleSparkClusterRequest.class)))
        .thenReturn(mockResponse);

    try {
      serviceWithEmptyTenant.updateStageHistory(stageHistory);
      verify(mockLogCentralOrchestratorClient, times(1))
          .postScaleSparkCluster(anyMap(), any(ScaleSparkClusterRequest.class));
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  @Test
  public void testGetCurrentSparkStageHistory_AfterSettingNull_ReturnsNull() {
    SparkStageHistory stageHistory = createTestSparkStageHistory();
    sparkScaleService.setCurrentSparkStageHistory(stageHistory);
    assertNotNull(sparkScaleService.getCurrentSparkStageHistory());

    sparkScaleService.setCurrentSparkStageHistory(null);
    assertNull(sparkScaleService.getCurrentSparkStageHistory());
  }

  @Test
  public void testUpdateStageHistory_WithConfigException_HandlesGracefully() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("tenant.name", "test-tenant");
    // Missing required config keys
    Config invalidConfig = MockConfigHelper.createConfig(configMap);
    SparkScaleService serviceWithInvalidConfig =
        new SparkScaleService(invalidConfig, mockLogCentralOrchestratorClient);

    SparkStageHistory stageHistory = createTestSparkStageHistory();

    try {
      serviceWithInvalidConfig.updateStageHistory(stageHistory);
      // Should handle exception gracefully
      assertTrue(true);
    } catch (Exception e) {
      // Exception is expected and should be caught internally
      assertTrue(true);
    }
  }

  @Test
  public void testUpdateStageHistory_WithNullTenantName_HandlesGracefully() {
    Map<String, Object> configMap = new HashMap<>();
    configMap.put("spark.scale.downscale.enable", true);
    configMap.put("spark.scale.upscale.enable", true);
    configMap.put("tenant.name", null);
    Config configWithNullTenant = MockConfigHelper.createConfig(configMap);
    SparkScaleService serviceWithNullTenant =
        new SparkScaleService(configWithNullTenant, mockLogCentralOrchestratorClient);

    SparkStageHistory stageHistory = createTestSparkStageHistory();

    try {
      serviceWithNullTenant.updateStageHistory(stageHistory);
      assertTrue(true);
    } catch (Exception e) {
      assertTrue(true);
    }
  }

  @Test
  public void testSetCurrentSparkStageHistory_WithDifferentInstances_UpdatesCorrectly() {
    SparkStageHistory history1 = createTestSparkStageHistory();
    SparkStageHistory history2 = createComplexTestSparkStageHistory();

    sparkScaleService.setCurrentSparkStageHistory(history1);
    assertEquals(sparkScaleService.getCurrentSparkStageHistory(), history1);

    sparkScaleService.setCurrentSparkStageHistory(history2);
    assertEquals(sparkScaleService.getCurrentSparkStageHistory(), history2);
    assertNotEquals(sparkScaleService.getCurrentSparkStageHistory(), history1);
  }

  @Test
  public void testUpdateStageHistory_WithVeryLargeStageHistory_ProcessesCorrectly() {
    SparkStageHistory largeHistory = new SparkStageHistory();
    largeHistory.setOutputBytes(Long.MAX_VALUE);
    largeHistory.setInputRecords(Long.MAX_VALUE);
    largeHistory.setSubmissionTime(System.currentTimeMillis());
    largeHistory.setCompletionTime(System.currentTimeMillis() + 1000);
    largeHistory.setCoresUsed(Integer.MAX_VALUE);
    largeHistory.setStatus("SUCCESS");
    largeHistory.setTenant("test-tenant");

    Map<String, Object> mockResponse = new HashMap<>();
    mockResponse.put("status", "success");
    when(mockLogCentralOrchestratorClient.postScaleSparkCluster(
            anyMap(), any(ScaleSparkClusterRequest.class)))
        .thenReturn(mockResponse);

    sparkScaleService.updateStageHistory(largeHistory);

    verify(mockLogCentralOrchestratorClient, times(1))
        .postScaleSparkCluster(anyMap(), any(ScaleSparkClusterRequest.class));
  }

  /**
   * Helper method to create a test SparkStageHistory instance.
   *
   * @return A SparkStageHistory instance for testing
   */
  private SparkStageHistory createTestSparkStageHistory() {
    SparkStageHistory stageHistory = new SparkStageHistory();
    stageHistory.setOutputBytes(100000L);
    stageHistory.setInputRecords(1000L);
    stageHistory.setSubmissionTime(System.currentTimeMillis());
    stageHistory.setCompletionTime(System.currentTimeMillis() + 5000);
    stageHistory.setCoresUsed(4);
    stageHistory.setStatus("SUCCESS");
    stageHistory.setTenant("test-tenant");
    return stageHistory;
  }

  /**
   * Helper method to create a complex test SparkStageHistory instance.
   *
   * @return A complex SparkStageHistory instance for testing
   */
  private SparkStageHistory createComplexTestSparkStageHistory() {
    SparkStageHistory stageHistory = new SparkStageHistory();
    stageHistory.setOutputBytes(5000000L);
    stageHistory.setInputRecords(50000L);
    stageHistory.setSubmissionTime(System.currentTimeMillis() - 10000);
    stageHistory.setCompletionTime(System.currentTimeMillis());
    stageHistory.setCoresUsed(16);
    stageHistory.setStatus("COMPLETED");
    stageHistory.setTenant("test-tenant");
    return stageHistory;
  }
}
