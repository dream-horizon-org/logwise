package com.logwise.orchestrator.dto.response;

import com.logwise.orchestrator.dto.entity.SparkStageHistory;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.NonFinal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetSparkStageHistoryResponse {
  @NonFinal List<SparkStageHistory> sparkStageHistory;
}
