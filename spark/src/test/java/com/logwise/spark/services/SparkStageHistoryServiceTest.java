package com.logwise.spark.services;

import com.logwise.spark.base.MockConfigHelper;
import com.logwise.spark.clients.LogCentralOrchestratorClient;
import com.logwise.spark.constants.Constants;
import com.logwise.spark.dto.entity.SparkStageHistory;
import com.logwise.spark.dto.request.ScaleSparkClusterRequest;
import com.logwise.spark.dto.response.GetSparkStageHistoryResponse;
import com.typesafe.config.Config;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Unit tests for SparkStageHistoryService. */
public class SparkStageHistoryServiceTest {

    private LogCentralOrchestratorClient mockLogCentralOrchestratorClient;
    private Config mockConfig;
    private SparkStageHistoryService sparkStageHistoryService;

    @BeforeMethod
    public void setUp() {
        mockLogCentralOrchestratorClient = Mockito.mock(LogCentralOrchestratorClient.class);
        mockConfig = MockConfigHelper.createMinimalSparkConfig();
        sparkStageHistoryService = new SparkStageHistoryService(mockConfig, mockLogCentralOrchestratorClient);

        // Reset static state
        sparkStageHistoryService.setCurrentSparkStageHistory(null);
    }

    @Test
    public void testGetCurrentSparkStageHistory_Initially_ReturnsNull() {
        // Act
        SparkStageHistory result = sparkStageHistoryService.getCurrentSparkStageHistory();

        // Assert
        Assert.assertNull(result);
    }

    @Test
    public void testSetCurrentSparkStageHistory_WithValidHistory_SetsHistory() {
        // Arrange
        SparkStageHistory history = new SparkStageHistory();
        history.setCoresUsed(10);
        history.setStatus("succeeded");

        // Act
        sparkStageHistoryService.setCurrentSparkStageHistory(history);

        // Assert
        SparkStageHistory result = sparkStageHistoryService.getCurrentSparkStageHistory();
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getCoresUsed(), Integer.valueOf(10));
        Assert.assertEquals(result.getStatus(), "succeeded");
    }

    @Test
    public void testGetStageHistoryList_WithValidResponse_ReturnsSortedList() {
        // Arrange
        GetSparkStageHistoryResponse response = new GetSparkStageHistoryResponse();
        GetSparkStageHistoryResponse.ResponseData data = new GetSparkStageHistoryResponse.ResponseData();
        List<SparkStageHistory> historyList = new ArrayList<>();

        SparkStageHistory history1 = new SparkStageHistory();
        history1.setSubmissionTime(1000L);
        history1.setStatus("succeeded");
        historyList.add(history1);

        SparkStageHistory history2 = new SparkStageHistory();
        history2.setSubmissionTime(2000L);
        history2.setStatus("succeeded");
        historyList.add(history2);

        data.setSparkStageHistory(historyList);
        response.setData(data);

        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.X_TENANT_NAME, "test-tenant");
        headers.put("Content-Type", "application/json");

        Map<String, String> queryParam = new HashMap<>();
        queryParam.put("limit", Constants.SPARK_STAGE_HISTORY_LIMIT.toString());

        Mockito.when(mockLogCentralOrchestratorClient.getSparkStageHistory(headers, queryParam))
                .thenReturn(response);

        // Act
        List<SparkStageHistory> result = sparkStageHistoryService.getStageHistoryList();

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result.size(), 2);
        // Should be sorted in reverse order (newest first)
        Assert.assertEquals(result.get(0).getSubmissionTime(), Long.valueOf(2000L));
        Assert.assertEquals(result.get(1).getSubmissionTime(), Long.valueOf(1000L));
        Mockito.verify(mockLogCentralOrchestratorClient).getSparkStageHistory(Mockito.anyMap(), Mockito.anyMap());
    }

    @Test
    public void testGetStageHistoryList_WithException_ReturnsEmptyList() {
        // Arrange
        Mockito.when(mockLogCentralOrchestratorClient.getSparkStageHistory(Mockito.anyMap(), Mockito.anyMap()))
                .thenThrow(new RuntimeException("Connection error"));

        // Act
        List<SparkStageHistory> result = sparkStageHistoryService.getStageHistoryList();

        // Assert
        Assert.assertNotNull(result);
        Assert.assertTrue(result.isEmpty());
    }

    @Test
    public void testUpdateStageHistory_WithValidHistory_UpdatesSuccessfully() {
        // Arrange
        SparkStageHistory history = new SparkStageHistory();
        history.setCoresUsed(10);
        history.setStatus("succeeded");
        history.setOutputBytes(1000L);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");

        Mockito.when(mockLogCentralOrchestratorClient.postScaleSparkCluster(Mockito.anyMap(), Mockito.any()))
                .thenReturn(response);

        // Act
        sparkStageHistoryService.updateStageHistory(history);

        // Assert
        ArgumentCaptor<ScaleSparkClusterRequest> requestCaptor = ArgumentCaptor
                .forClass(ScaleSparkClusterRequest.class);
        Mockito.verify(mockLogCentralOrchestratorClient).postScaleSparkCluster(Mockito.anyMap(),
                requestCaptor.capture());

        ScaleSparkClusterRequest capturedRequest = requestCaptor.getValue();
        Assert.assertNotNull(capturedRequest);
        Assert.assertEquals(capturedRequest.getSparkStageHistory(), history);
    }

    @Test
    public void testUpdateStageHistory_WithException_HandlesGracefully() {
        // Arrange
        SparkStageHistory history = new SparkStageHistory();
        history.setCoresUsed(10);

        Mockito.when(mockLogCentralOrchestratorClient.postScaleSparkCluster(Mockito.anyMap(), Mockito.any()))
                .thenThrow(new RuntimeException("Connection error"));

        // Act - Should not throw exception
        sparkStageHistoryService.updateStageHistory(history);

        // Assert
        Mockito.verify(mockLogCentralOrchestratorClient).postScaleSparkCluster(Mockito.anyMap(), Mockito.any());
    }

    @Test
    public void testUpdateStageHistory_WithScaleConfig_IncludesScaleFlags() {
        // Arrange
        SparkStageHistory history = new SparkStageHistory();
        history.setCoresUsed(10);

        // Create config with scale flags
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("spark.scale.downscale.enable", true);
        configMap.put("spark.scale.upscale.enable", false);
        configMap.put("tenant.name", "test-tenant");
        Config config = MockConfigHelper.createConfig(configMap);

        SparkStageHistoryService service = new SparkStageHistoryService(config, mockLogCentralOrchestratorClient);

        Mockito.when(mockLogCentralOrchestratorClient.postScaleSparkCluster(Mockito.anyMap(), Mockito.any()))
                .thenReturn(new HashMap<>());

        // Act
        service.updateStageHistory(history);

        // Assert
        ArgumentCaptor<ScaleSparkClusterRequest> requestCaptor = ArgumentCaptor
                .forClass(ScaleSparkClusterRequest.class);
        Mockito.verify(mockLogCentralOrchestratorClient).postScaleSparkCluster(Mockito.anyMap(),
                requestCaptor.capture());

        ScaleSparkClusterRequest capturedRequest = requestCaptor.getValue();
        Assert.assertTrue(capturedRequest.getEnableDownScale());
        Assert.assertFalse(capturedRequest.getEnableUpScale());
    }
}
