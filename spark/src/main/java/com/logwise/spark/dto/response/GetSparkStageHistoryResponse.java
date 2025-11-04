package com.logwise.spark.dto.response;

import com.logwise.spark.dto.entity.SparkStageHistory;
import java.util.List;
import lombok.Data;

@Data
public class GetSparkStageHistoryResponse {
    private ResponseData data;

    @Data
    public static class ResponseData {
        private List<SparkStageHistory> sparkStageHistory;
    }
}