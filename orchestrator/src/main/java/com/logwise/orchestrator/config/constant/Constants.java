package com.logwise.orchestrator.config.constant;

import java.util.Optional;

public class Constants {
  // Note: These constants are created from environment variables at run time
  public static final String CONSUL_TOKEN = System.getenv("CONSUL_TOKEN");
  public static final String ENV =
      Optional.ofNullable(System.getenv("ENV")).orElse(System.getProperty("app.environment"));
  public static final String NAMESPACE =
      Optional.ofNullable(System.getenv("NAMESPACE")).orElse(System.getenv("CONFIG_BRANCH"));
  public static final String SERVICE_NAME =
      Optional.ofNullable(System.getenv("SERVICE_NAME")).orElse(System.getenv("service_name"));
  public static final String VAULT_TOKEN = System.getenv("VAULT_TOKEN");
}
