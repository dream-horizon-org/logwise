package com.logwise.spark.services;

import com.logwise.spark.clients.SparkMasterClient;
import com.logwise.spark.dto.response.SparkMasterJsonResponse;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for SparkMasterService. */
public class SparkMasterServiceTest {

    private SparkMasterClient mockSparkMasterClient;
    private SparkMasterService sparkMasterService;

    @BeforeMethod
    public void setUp() {
        mockSparkMasterClient = Mockito.mock(SparkMasterClient.class);
        sparkMasterService = new SparkMasterService(mockSparkMasterClient);
    }

    @Test
    public void testGetCoresUsed_WithValidResponse_ReturnsCoresUsed() {
        // Arrange
        SparkMasterJsonResponse response = new SparkMasterJsonResponse();
        response.setCoresused(10);
        Mockito.when(mockSparkMasterClient.json()).thenReturn(response);

        // Act
        Integer result = sparkMasterService.getCoresUsed();

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result, Integer.valueOf(10));
        Mockito.verify(mockSparkMasterClient).json();
    }

    @Test
    public void testGetCoresUsed_WithZeroCores_ReturnsZero() {
        // Arrange
        SparkMasterJsonResponse response = new SparkMasterJsonResponse();
        response.setCoresused(0);
        Mockito.when(mockSparkMasterClient.json()).thenReturn(response);

        // Act
        Integer result = sparkMasterService.getCoresUsed();

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(result, Integer.valueOf(0));
    }

    @Test
    public void testGetCoresUsed_WithException_ReturnsNull() {
        // Arrange
        Mockito.when(mockSparkMasterClient.json()).thenThrow(new RuntimeException("Connection error"));

        // Act
        Integer result = sparkMasterService.getCoresUsed();

        // Assert
        Assert.assertNull(result);
        Mockito.verify(mockSparkMasterClient).json();
    }

    @Test
    public void testGetCoresUsed_WithNullResponse_HandlesGracefully() {
        // Arrange
        Mockito.when(mockSparkMasterClient.json()).thenReturn(null);

        // Act
        Integer result = sparkMasterService.getCoresUsed();

        // Assert
        // The method will try to call getCoresused() on null, which will throw NPE
        // and be caught, returning null
        Assert.assertNull(result);
    }
}
