package com.logwise.orchestrator.enums;

import com.logwise.orchestrator.error.ServiceError;
import com.logwise.orchestrator.rest.exception.RestException;
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
