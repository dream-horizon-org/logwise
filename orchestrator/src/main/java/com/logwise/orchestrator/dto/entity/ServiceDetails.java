package com.logwise.orchestrator.dto.entity;

import lombok.*;
import lombok.experimental.NonFinal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceDetails {
  @NonFinal @NonNull String environmentName;
  @NonFinal @NonNull String serviceName;
  @NonFinal String componentType;
  @NonFinal Integer retentionDays;
  @NonFinal String tenant;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ServiceDetails that = (ServiceDetails) o;
    return environmentName.equals(that.environmentName) && serviceName.equals(that.serviceName);
  }
}
