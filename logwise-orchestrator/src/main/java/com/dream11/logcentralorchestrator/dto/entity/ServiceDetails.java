package com.dream11.logcentralorchestrator.dto.entity;

import lombok.*;
import lombok.experimental.NonFinal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceDetails {
  @NonFinal @NonNull String env;
  @NonFinal @NonNull String serviceName;
  @NonFinal @NonNull String componentName;
  @NonFinal Integer retentionDays;
  @NonFinal String tenant;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ServiceDetails that = (ServiceDetails) o;
    return env.equals(that.env)
        && serviceName.equals(that.serviceName)
        && componentName.equals(that.componentName);
  }
}
