package com.logwise.orchestrator.tests.unit.util;

import com.logwise.orchestrator.common.app.AppContext;
import com.logwise.orchestrator.dto.entity.ServiceDetails;
import com.logwise.orchestrator.setup.BaseTest;
import com.logwise.orchestrator.util.ApplicationUtils;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ApplicationUtilsTest extends BaseTest {

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
  }

  @AfterClass
  public static void tearDownClass() {
    BaseTest.cleanup();
  }

  @Test
  public void testRowSetToMapList_WithValidRows_ReturnsMapList() {
    RowSet<Row> mockRowSet = mock(RowSet.class);
    Row mockRow1 = mock(Row.class);
    Row mockRow2 = mock(Row.class);
    
    when(mockRowSet.spliterator()).thenReturn(
        java.util.Arrays.asList(mockRow1, mockRow2).spliterator()
    );
    
    when(mockRow1.size()).thenReturn(2);
    when(mockRow1.getColumnName(0)).thenReturn("id");
    when(mockRow1.getValue(0)).thenReturn(1);
    when(mockRow1.getColumnName(1)).thenReturn("name");
    when(mockRow1.getValue(1)).thenReturn("test");
    
    when(mockRow2.size()).thenReturn(2);
    when(mockRow2.getColumnName(0)).thenReturn("id");
    when(mockRow2.getValue(0)).thenReturn(2);
    when(mockRow2.getColumnName(1)).thenReturn("name");
    when(mockRow2.getValue(1)).thenReturn("test2");
    
    List<Map<String, Object>> result = ApplicationUtils.rowSetToMapList(mockRowSet);
    
    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 2);
    Assert.assertEquals(result.get(0).get("id"), 1);
    Assert.assertEquals(result.get(0).get("name"), "test");
    Assert.assertEquals(result.get(1).get("id"), 2);
    Assert.assertEquals(result.get(1).get("name"), "test2");
  }

  @Test
  public void testRowSetToMapList_WithLocalDateTime_ConvertsToDate() {
    RowSet<Row> mockRowSet = mock(RowSet.class);
    Row mockRow = mock(Row.class);
    LocalDateTime localDateTime = LocalDateTime.of(2023, 1, 1, 12, 0, 0);
    
    when(mockRowSet.spliterator()).thenReturn(
        java.util.Arrays.asList(mockRow).spliterator()
    );
    
    when(mockRow.size()).thenReturn(1);
    when(mockRow.getColumnName(0)).thenReturn("created_at");
    when(mockRow.getValue(0)).thenReturn(localDateTime);
    when(mockRow.getLocalDateTime(0)).thenReturn(localDateTime);
    
    List<Map<String, Object>> result = ApplicationUtils.rowSetToMapList(mockRowSet);
    
    Assert.assertNotNull(result);
    Assert.assertEquals(result.size(), 1);
    Object dateValue = result.get(0).get("created_at");
    Assert.assertTrue(dateValue instanceof Date);
  }

  @Test
  public void testGetServiceFromObjectKey_WithValidKey_ReturnsServiceDetails() {
    String logPath = "s3://bucket/logs/service_name=my-service/2023/01/01/file.log";
    
    ServiceDetails result = ApplicationUtils.getServiceFromObjectKey(logPath);
    
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getServiceName(), "my-service");
  }

  @Test
  public void testGetServiceFromObjectKey_WithInvalidKey_ReturnsNull() {
    String logPath = "s3://bucket/logs/2023/01/01/file.log";
    
    ServiceDetails result = ApplicationUtils.getServiceFromObjectKey(logPath);
    
    Assert.assertNull(result);
  }

  @Test
  public void testGetServiceFromObjectKey_WithMultipleMatches_ReturnsFirstMatch() {
    String logPath = "s3://bucket/logs/service_name=first-service/service_name=second-service/file.log";
    
    ServiceDetails result = ApplicationUtils.getServiceFromObjectKey(logPath);
    
    Assert.assertNotNull(result);
    Assert.assertEquals(result.getServiceName(), "first-service");
  }

  @Test
  public void testGetIpAddresses_WithValidHostname_ReturnsIpAddresses() {
    try (MockedStatic<AppContext> mockedAppContext = mockStatic(AppContext.class)) {
      Vertx mockVertx = BaseTest.getReactiveVertx();
      mockedAppContext.when(() -> AppContext.getInstance(Vertx.class)).thenReturn(mockVertx);
      
      Single<List<String>> result = ApplicationUtils.getIpAddresses("localhost");
      
      List<String> ips = result.blockingGet();
      Assert.assertNotNull(ips);
      Assert.assertFalse(ips.isEmpty());
      Assert.assertTrue(ips.contains("127.0.0.1") || ips.contains("::1"));
    }
  }

  @Test
  public void testGetIpAddresses_WithInvalidHostname_ThrowsException() {
    try (MockedStatic<AppContext> mockedAppContext = mockStatic(AppContext.class)) {
      Vertx mockVertx = BaseTest.getReactiveVertx();
      mockedAppContext.when(() -> AppContext.getInstance(Vertx.class)).thenReturn(mockVertx);
      
      Single<List<String>> result = ApplicationUtils.getIpAddresses("invalid-hostname-xyz-123");
      
      try {
        result.blockingGet();
        Assert.fail("Should have thrown exception");
      } catch (RuntimeException e) {
        Assert.assertNotNull(e);
        Assert.assertTrue(e.getMessage().contains("Failed to resolve hostname"));
      }
    }
  }

  @Test
  public void testGetGuiceInstance_WithValidInstance_ReturnsInstance() {
    try (MockedStatic<AppContext> mockedAppContext = mockStatic(AppContext.class)) {
      String testInstance = "test-instance";
      mockedAppContext.when(() -> AppContext.getInstance(String.class, "test-name"))
          .thenReturn(testInstance);
      
      String result = ApplicationUtils.getGuiceInstance(String.class, "test-name");
      
      Assert.assertNotNull(result);
      Assert.assertEquals(result, testInstance);
    }
  }

  @Test
  public void testGetGuiceInstance_WithConfigurationException_ReturnsNull() {
    try (MockedStatic<AppContext> mockedAppContext = mockStatic(AppContext.class)) {
      mockedAppContext.when(() -> AppContext.getInstance(String.class, "test-name"))
          .thenThrow(new com.google.inject.ConfigurationException(java.util.Collections.emptyList()));
      
      String result = ApplicationUtils.getGuiceInstance(String.class, "test-name");
      
      Assert.assertNull(result);
    }
  }

  @Test
  public void testGetGuiceInstance_WithOtherException_ReturnsNull() {
    try (MockedStatic<AppContext> mockedAppContext = mockStatic(AppContext.class)) {
      mockedAppContext.when(() -> AppContext.getInstance(String.class, "test-name"))
          .thenThrow(new RuntimeException("Unexpected error"));
      
      String result = ApplicationUtils.getGuiceInstance(String.class, "test-name");
      
      Assert.assertNull(result);
    }
  }
}

