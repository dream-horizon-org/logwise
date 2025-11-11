package com.dream11.logcentralorchestrator.config;

import com.dream11.logcentralorchestrator.common.util.ConfigUtils;
import com.dream11.logcentralorchestrator.enums.Tenant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.*;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ApplicationConfigProvider {

  private static final ApplicationConfigProvider CONFIG_PROVIDER = new ApplicationConfigProvider();
  @NonFinal private ApplicationConfig applicationConfig = null;

  public static void initConfig() {
    try {
      log.info("Initializing ApplicationConfig...");
      CONFIG_PROVIDER.applicationConfig =
          ConfigUtils.fromConfigFile(
              "config/application/application-%s.conf", ApplicationConfig.class);
      CONFIG_PROVIDER.validateConfig();
    } catch (Exception e) {
      log.error("Failed to initialize ApplicationConfig", e);
      throw e;
    }
  }

  public static ApplicationConfig getApplicationConfig() {
    return CONFIG_PROVIDER.applicationConfig;
  }

  private void validateConfig() {
    log.info("Validating ApplicationConfig...");
    try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
      Validator validator = factory.getValidator();
      Set<ConstraintViolation<ApplicationConfig>> constraintViolations =
          validator.validate(applicationConfig);
      if (!constraintViolations.isEmpty()) {
        throw new ConstraintViolationException(constraintViolations);
      }
      validateTenantConfig();
    }
  }

  private void validateTenantConfig() {
    List<String> validTenants =
        Arrays.stream(Tenant.values()).map(Tenant::getValue).collect(Collectors.toList());
    applicationConfig
        .getTenants()
        .forEach(
            tenantConfig -> {
              if (!validTenants.contains(tenantConfig.getName())) {
                throw new IllegalArgumentException(
                    "Application config has invalid tenant: " + tenantConfig);
              }
            });
  }
}
