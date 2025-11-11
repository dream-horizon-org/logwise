package com.dream11.logcentralorchestrator.enums;

import com.dream11.logcentralorchestrator.error.ServiceError;
import com.dream11.logcentralorchestrator.rest.exception.RestException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ComponentType {
  APPLICATION("application");
  private final String value;

  public static ComponentType fromValue(String value) {
    for (ComponentType componentType : ComponentType.values()) {
      if (componentType.getValue().equals(value)) {
        return componentType;
      }
    }
    throw new RestException(ServiceError.INVALID_COMPONENT_TYPE.format(value));
  }
}
