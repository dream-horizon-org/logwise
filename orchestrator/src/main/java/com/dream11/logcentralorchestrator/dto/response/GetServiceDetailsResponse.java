package com.dream11.logcentralorchestrator.dto.response;

import com.dream11.logcentralorchestrator.dto.entity.ServiceDetails;
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
public class GetServiceDetailsResponse {
  @NonFinal List<ServiceDetails> serviceDetails;
}
