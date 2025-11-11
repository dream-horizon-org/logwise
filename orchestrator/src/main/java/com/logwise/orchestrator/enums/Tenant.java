package com.logwise.orchestrator.enums;

import com.logwise.orchestrator.error.ServiceError;
import com.logwise.orchestrator.rest.exception.RestException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Tenant {
  ABC("ABC");
  private final String value;

  public static Tenant fromValue(String value) {
    for (Tenant tenant : Tenant.values()) {
      if (tenant.getValue().equals(value)) {
        return tenant;
      }
    }
    throw new RestException(ServiceError.INVALID_TENANT.format(value));
  }
}
