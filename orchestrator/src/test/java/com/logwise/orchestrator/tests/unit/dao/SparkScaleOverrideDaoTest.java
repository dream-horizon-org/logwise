package com.logwise.orchestrator.tests.unit.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.orchestrator.dao.SparkScaleOverrideDao;
import com.logwise.orchestrator.dto.entity.SparkScaleOverride;
import com.logwise.orchestrator.enums.Tenant;
import com.logwise.orchestrator.mysql.reactivex.client.MysqlClient;
import com.logwise.orchestrator.util.ApplicationUtils;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.mysqlclient.MySQLPool;
import io.vertx.reactivex.sqlclient.RowSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.MockedStatic;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SparkScaleOverrideDaoTest {

  private SparkScaleOverrideDao dao;
  private MysqlClient mockMysqlClient;
  private ObjectMapper mockObjectMapper;
  private MySQLPool mockMasterPool;
  private MySQLPool mockSlavePool;

  @BeforeMethod
  public void setUp() {
    mockMysqlClient = mock(MysqlClient.class);
    mockObjectMapper = mock(ObjectMapper.class);
    mockMasterPool = mock(MySQLPool.class);
    mockSlavePool = mock(MySQLPool.class);
    
    when(mockMysqlClient.getMasterMysqlClient()).thenReturn(mockMasterPool);
    when(mockMysqlClient.getSlaveMysqlClient()).thenReturn(mockSlavePool);
    
    dao = new SparkScaleOverrideDao(mockMysqlClient, mockObjectMapper);
  }

  @Test
  public void testGetSparkScaleOverride_WithExistingOverride_ReturnsOverride() {
    Tenant tenant = Tenant.ABC;
    RowSet mockRowSet = mock(RowSet.class);
    io.vertx.reactivex.sqlclient.PreparedQuery<io.vertx.reactivex.sqlclient.RowSet<io.vertx.reactivex.sqlclient.Row>> mockPreparedQuery = 
        mock(io.vertx.reactivex.sqlclient.PreparedQuery.class);
    
    when(mockSlavePool.preparedQuery(anyString())).thenReturn(mockPreparedQuery);
    when(mockPreparedQuery.rxExecute(any())).thenReturn(Single.just(mockRowSet));
    
    Map<String, Object> rowMap = new HashMap<>();
    rowMap.put("tenant", "ABC");
    rowMap.put("upscale", true);
    rowMap.put("downscale", false);
    List<Map<String, Object>> rowList = new ArrayList<>();
    rowList.add(rowMap);
    
    SparkScaleOverride expectedOverride = SparkScaleOverride.builder()
        .tenant("ABC")
        .upscale(true)
        .downscale(false)
        .build();
    
    try (MockedStatic<ApplicationUtils> mockedUtils = mockStatic(ApplicationUtils.class)) {
      mockedUtils.when(() -> ApplicationUtils.rowSetToMapList(any(RowSet.class)))
          .thenReturn(rowList);
      
      when(mockObjectMapper.convertValue(any(), eq(SparkScaleOverride.class)))
          .thenReturn(expectedOverride);
      
      Single<SparkScaleOverride> result = dao.getSparkScaleOverride(tenant);
      
      SparkScaleOverride override = result.blockingGet();
      Assert.assertNotNull(override);
      Assert.assertEquals(override.getTenant(), "ABC");
    }
  }

  @Test
  public void testGetSparkScaleOverride_WithNoExistingOverride_ReturnsDefault() {
    Tenant tenant = Tenant.ABC;
    RowSet mockRowSet = mock(RowSet.class);
    io.vertx.reactivex.sqlclient.PreparedQuery<io.vertx.reactivex.sqlclient.RowSet<io.vertx.reactivex.sqlclient.Row>> mockPreparedQuery = 
        mock(io.vertx.reactivex.sqlclient.PreparedQuery.class);
    
    when(mockSlavePool.preparedQuery(anyString())).thenReturn(mockPreparedQuery);
    when(mockPreparedQuery.rxExecute(any())).thenReturn(Single.just(mockRowSet));
    
    try (MockedStatic<ApplicationUtils> mockedUtils = mockStatic(ApplicationUtils.class)) {
      mockedUtils.when(() -> ApplicationUtils.rowSetToMapList(any(RowSet.class)))
          .thenReturn(new ArrayList<>());
      
      Single<SparkScaleOverride> result = dao.getSparkScaleOverride(tenant);
      
      SparkScaleOverride override = result.blockingGet();
      Assert.assertNotNull(override);
      Assert.assertEquals(override.getTenant(), "ABC");
    }
  }

  @Test
  public void testUpdateSparkScaleOverride_WithValidOverride_UpdatesSuccessfully() {
    SparkScaleOverride override = SparkScaleOverride.builder()
        .tenant("ABC")
        .upscale(true)
        .downscale(false)
        .build();
    
    RowSet mockRowSet = mock(RowSet.class);
    io.vertx.reactivex.sqlclient.PreparedQuery<io.vertx.reactivex.sqlclient.RowSet<io.vertx.reactivex.sqlclient.Row>> mockPreparedQuery = 
        mock(io.vertx.reactivex.sqlclient.PreparedQuery.class);
    
    when(mockMasterPool.preparedQuery(anyString())).thenReturn(mockPreparedQuery);
    when(mockPreparedQuery.rxExecute(any())).thenReturn(Single.just(mockRowSet));
    when(mockRowSet.rowCount()).thenReturn(1);
    
    Completable result = dao.updateSparkScaleOverride(override);
    
    result.blockingAwait();
    verify(mockMasterPool, times(1)).preparedQuery(anyString());
  }

  @Test
  public void testUpdateSparkScaleOverride_WithNoRowsUpdated_ThrowsException() {
    SparkScaleOverride override = SparkScaleOverride.builder()
        .tenant("ABC")
        .upscale(true)
        .downscale(false)
        .build();
    
    RowSet mockRowSet = mock(RowSet.class);
    io.vertx.reactivex.sqlclient.PreparedQuery<io.vertx.reactivex.sqlclient.RowSet<io.vertx.reactivex.sqlclient.Row>> mockPreparedQuery = 
        mock(io.vertx.reactivex.sqlclient.PreparedQuery.class);
    
    when(mockMasterPool.preparedQuery(anyString())).thenReturn(mockPreparedQuery);
    when(mockPreparedQuery.rxExecute(any())).thenReturn(Single.just(mockRowSet));
    when(mockRowSet.rowCount()).thenReturn(0);
    
    Completable result = dao.updateSparkScaleOverride(override);
    
    try {
      result.blockingAwait();
      Assert.fail("Should have thrown exception");
    } catch (Exception e) {
      Assert.assertNotNull(e);
    }
  }
}

