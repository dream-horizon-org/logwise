package com.logwise.orchestrator;

public class CacheNotFoundException extends RuntimeException {
  public CacheNotFoundException(String message) {
    super(message);
  }
}
