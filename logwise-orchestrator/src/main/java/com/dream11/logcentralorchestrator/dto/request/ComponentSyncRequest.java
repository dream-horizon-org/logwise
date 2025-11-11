package com.dream11.logcentralorchestrator.dto.request;

import com.dream11.logcentralorchestrator.enums.ComponentType;
import com.dream11.logcentralorchestrator.error.ServiceError;
import com.dream11.logcentralorchestrator.rest.exception.RestException;
import javax.ws.rs.QueryParam;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.NonFinal;

@Data
@NoArgsConstructor
public class ComponentSyncRequest {
  @QueryParam("componentType")
  @NonFinal
  String componentType;

  public void validateParam() {
    if (componentType == null) {
      throw new RestException(
          ServiceError.INVALID_REQUEST_ERROR.format("componentType is required"));
    }
    ComponentType.fromValue(componentType);
  }
}
