package com.dream11.logcentralorchestrator.enums;

import com.dream11.logcentralorchestrator.error.ServiceError;
import com.dream11.logcentralorchestrator.rest.exception.RestException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Tenant {
  D11_Prod_AWS("D11-Prod-AWS"),
  D11_STAG_AWS("D11-Stag-AWS"),
  DP_LOGS_AWS("DP-Logs-AWS"),
  HULK_PROD_AWS("Hulk-Prod-AWS"),
  DELIVR_AWS("Delivr-AWS");
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
