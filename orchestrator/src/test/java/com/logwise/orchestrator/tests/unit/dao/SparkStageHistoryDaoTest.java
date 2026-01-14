package com.logwise.orchestrator.tests.unit.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.orchestrator.dao.SparkStageHistoryDao;
import com.logwise.orchestrator.dto.entity.SparkStageHistory;
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
import static org.mockito.Mockito.*;

public class SparkStageHistoryDaoTest {

  private SparkStageHistoryDao dao;
  private MysqlClient mockMysqlClient;
  private ObjectMapper mockObjectMapper;
  private MySQLPool mockMasterPool;

  @BeforeMethod
  public void setUp() {
    mockMysqlClient = mock(MysqlClient.class);
    mockObjectMapper = mock(ObjectMapper.class);
    mockMasterPool = mock(MySQLPool.class);
    
    when(mockMysqlClient.getMasterMysqlClient()).thenReturn(mockMasterPool);
    
    dao = new SparkStageHistoryDao(mockMysqlClient, mockObjectMapper);
  }

  @Test
  public void testGetSparkStageHistory_WithValidTenant_ReturnsHistory() {
    Tenant tenant = Tenant.ABC;
    int limit = 10;
    RowSet mockRowSet = mock(RowSet.class);
    io.vertx.reactivex.sqlclient.PreparedQuery<io.vertx.reactivex.sqlclient.RowSet<io.vertx.reactivex.sqlclient.Row>> mockPreparedQuery = 
        mock(io.vertx.reactivex.sqlclient.PreparedQuery.class);
    
    when(mockMasterPool.preparedQuery(anyString())).thenReturn(mockPreparedQuery);
    when(mockPreparedQuery.rxExecute(any())).thenReturn(Single.just(mockRowSet));
    
    Map<String, Object> rowMap = new HashMap<>();
    rowMap.put("tenant", "ABC");
    rowMap.put("inputRecords", 1000L);
    List<Map<String, Object>> rowList = new ArrayList<>();
    rowList.add(rowMap);
    
    SparkStageHistory historyObj = new SparkStageHistory();
    historyObj.setTenant("ABC");
    historyObj.setInputRecords(1000L);
    
    try (MockedStatic<ApplicationUtils> mockedUtils = mockStatic(ApplicationUtils.class)) {
      mockedUtils.when(() -> ApplicationUtils.rowSetToMapList(any(RowSet.class)))
          .thenReturn(rowList);
      
      when(mockObjectMapper.convertValue(any(), eq(SparkStageHistory.class)))
          .thenReturn(historyObj);
      
      Single<java.util.List<SparkStageHistory>> result = dao.getSparkStageHistory(tenant, limit, true);
      
      java.util.List<SparkStageHistory> history = result.blockingGet();
      Assert.assertNotNull(history);
      verify(mockMasterPool, times(1)).preparedQuery(anyString());
    }
  }

  @Test
  public void testInsertSparkStageHistory_WithValidHistory_InsertsSuccessfully() {
    SparkStageHistory history = new SparkStageHistory();
    history.setTenant("ABC");
    history.setInputRecords(1000L);
    history.setOutputBytes(2000L);
    
    RowSet mockRowSet = mock(RowSet.class);
    io.vertx.reactivex.sqlclient.PreparedQuery<io.vertx.reactivex.sqlclient.RowSet<io.vertx.reactivex.sqlclient.Row>> mockPreparedQuery = 
        mock(io.vertx.reactivex.sqlclient.PreparedQuery.class);
    
    when(mockMasterPool.preparedQuery(anyString())).thenReturn(mockPreparedQuery);
    when(mockPreparedQuery.rxExecute(any())).thenReturn(Single.just(mockRowSet));
    
    Completable result = dao.insertSparkStageHistory(history);
    
    result.blockingAwait();
    verify(mockMasterPool, times(1)).preparedQuery(anyString());
  }
}

