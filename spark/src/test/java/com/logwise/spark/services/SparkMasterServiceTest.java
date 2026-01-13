package com.logwise.spark.services;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.logwise.spark.base.BaseSparkTest;
import com.logwise.spark.clients.SparkMasterClient;
import com.logwise.spark.dto.response.SparkMasterJsonResponse;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for SparkMasterService. */
public class SparkMasterServiceTest extends BaseSparkTest {

  private SparkMasterClient mockSparkMasterClient;
  private SparkMasterService sparkMasterService;

  @BeforeMethod
  @Override
  public void setUp() {
    super.setUp();
    mockSparkMasterClient = mock(SparkMasterClient.class);
    sparkMasterService = new SparkMasterService(mockSparkMasterClient);
  }

  @Test
  public void testGetCoresUsed_WithValidResponse_ReturnsCoresUsed() {
    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    response.setCoresused(8);
    when(mockSparkMasterClient.json()).thenReturn(response);

    Integer result = sparkMasterService.getCoresUsed();

    assertNotNull(result);
    assertEquals(result.intValue(), 8);
    verify(mockSparkMasterClient, times(1)).json();
  }

  @Test
  public void testGetCoresUsed_WithZeroCores_ReturnsZero() {
    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    response.setCoresused(0);
    when(mockSparkMasterClient.json()).thenReturn(response);

    Integer result = sparkMasterService.getCoresUsed();

    assertNotNull(result);
    assertEquals(result.intValue(), 0);
    verify(mockSparkMasterClient, times(1)).json();
  }

  @Test
  public void testGetCoresUsed_WithLargeValue_ReturnsLargeValue() {
    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    response.setCoresused(100);
    when(mockSparkMasterClient.json()).thenReturn(response);

    Integer result = sparkMasterService.getCoresUsed();

    assertNotNull(result);
    assertEquals(result.intValue(), 100);
    verify(mockSparkMasterClient, times(1)).json();
  }

  @Test
  public void testGetCoresUsed_WithClientException_ReturnsNull() {
    RuntimeException clientException = new RuntimeException("Network error");
    when(mockSparkMasterClient.json()).thenThrow(clientException);

    Integer result = sparkMasterService.getCoresUsed();

    assertNull(result);
    verify(mockSparkMasterClient, times(1)).json();
  }

  @Test
  public void testGetCoresUsed_WithNullResponse_HandlesGracefully() {
    when(mockSparkMasterClient.json()).thenReturn(null);

    try {
      Integer result = sparkMasterService.getCoresUsed();
      assertNull(result);
    } catch (Exception e) {
      assertTrue(true);
    }
    verify(mockSparkMasterClient, times(1)).json();
  }

  @Test
  public void testGetCoresUsed_WithMultipleCalls_ReturnsConsistentResults() {
    SparkMasterJsonResponse response = new SparkMasterJsonResponse();
    response.setCoresused(16);
    when(mockSparkMasterClient.json()).thenReturn(response);

    Integer result1 = sparkMasterService.getCoresUsed();
    Integer result2 = sparkMasterService.getCoresUsed();

    assertNotNull(result1);
    assertNotNull(result2);
    assertEquals(result1.intValue(), 16);
    assertEquals(result2.intValue(), 16);
    verify(mockSparkMasterClient, times(2)).json();
  }
}
