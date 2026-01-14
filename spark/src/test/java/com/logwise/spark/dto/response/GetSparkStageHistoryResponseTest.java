package com.logwise.spark.dto.response;

import com.logwise.spark.dto.entity.SparkStageHistory;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GetSparkStageHistoryResponseTest {

  @Test
  public void testGetSparkStageHistoryResponse_WithValidData_SetsData() {
    GetSparkStageHistoryResponse response = new GetSparkStageHistoryResponse();
    GetSparkStageHistoryResponse.ResponseData data =
        new GetSparkStageHistoryResponse.ResponseData();
    List<SparkStageHistory> historyList = new ArrayList<>();

    SparkStageHistory history = new SparkStageHistory();
    history.setTenant("ABC");
    history.setInputRecords(1000L);
    historyList.add(history);

    data.setSparkStageHistory(historyList);
    response.setData(data);

    Assert.assertNotNull(response.getData());
    Assert.assertNotNull(response.getData().getSparkStageHistory());
    Assert.assertEquals(response.getData().getSparkStageHistory().size(), 1);
    Assert.assertEquals(response.getData().getSparkStageHistory().get(0).getTenant(), "ABC");
  }

  @Test
  public void testGetSparkStageHistoryResponse_WithEmptyList_SetsEmptyList() {
    GetSparkStageHistoryResponse response = new GetSparkStageHistoryResponse();
    GetSparkStageHistoryResponse.ResponseData data =
        new GetSparkStageHistoryResponse.ResponseData();

    data.setSparkStageHistory(new ArrayList<>());
    response.setData(data);

    Assert.assertNotNull(response.getData());
    Assert.assertNotNull(response.getData().getSparkStageHistory());
    Assert.assertTrue(response.getData().getSparkStageHistory().isEmpty());
  }

  @Test
  public void testGetSparkStageHistoryResponse_WithNullData_HandlesGracefully() {
    GetSparkStageHistoryResponse response = new GetSparkStageHistoryResponse();
    response.setData(null);

    Assert.assertNull(response.getData());
  }

  @Test
  public void testResponseData_WithMultipleHistoryItems_SetsAllItems() {
    GetSparkStageHistoryResponse.ResponseData data =
        new GetSparkStageHistoryResponse.ResponseData();
    List<SparkStageHistory> historyList = new ArrayList<>();

    SparkStageHistory history1 = new SparkStageHistory();
    history1.setTenant("ABC");
    history1.setInputRecords(1000L);
    historyList.add(history1);

    SparkStageHistory history2 = new SparkStageHistory();
    history2.setTenant("XYZ");
    history2.setInputRecords(2000L);
    historyList.add(history2);

    data.setSparkStageHistory(historyList);

    Assert.assertEquals(data.getSparkStageHistory().size(), 2);
    Assert.assertEquals(data.getSparkStageHistory().get(0).getTenant(), "ABC");
    Assert.assertEquals(data.getSparkStageHistory().get(1).getTenant(), "XYZ");
  }
}
