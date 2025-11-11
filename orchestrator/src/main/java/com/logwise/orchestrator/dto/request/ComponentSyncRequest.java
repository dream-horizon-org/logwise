package com.logwise.orchestrator.dto.request;

import com.logwise.orchestrator.enums.ComponentType;
import com.logwise.orchestrator.error.ServiceError;
import com.logwise.orchestrator.rest.exception.RestException;
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
