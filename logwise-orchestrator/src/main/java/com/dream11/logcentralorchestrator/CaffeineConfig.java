package com.dream11.logcentralorchestrator;

import com.typesafe.config.Optional;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class CaffeineConfig {

  @Optional
  @Setter(AccessLevel.NONE)
  private Long refreshAfterWriteSeconds;

  @Optional
  @Setter(AccessLevel.NONE)
  private Long refreshAfterWriteDuration;

  @Optional
  @Setter(AccessLevel.NONE)
  private TimeUnit refreshAfterWriteTimeUnit;

  @Optional
  @Setter(AccessLevel.NONE)
  private Long expireAfterWriteSeconds;

  @Optional
  @Setter(AccessLevel.NONE)
  private Long expireAfterWriteDuration;

  @Optional
  @Setter(AccessLevel.NONE)
  private TimeUnit expireAfterWriteTimeUnit;

  @Optional
  @Setter(AccessLevel.NONE)
  private Long expireAfterAccessSeconds;

  @Optional
  @Setter(AccessLevel.NONE)
  private Long expireAfterAccessDuration;

  @Optional
  @Setter(AccessLevel.NONE)
  private TimeUnit expireAfterAccessTimeUnit;

  @Optional private Integer initialCapacity;

  @Optional private Integer maximumSize;

  @Optional @NonNull private Boolean weakKeys = false;

  @Optional @NonNull private Boolean weakValues = false;

  @Optional @NonNull private Boolean softValues = false;

  public void setRefreshAfterWriteSeconds(Long refreshAfterWriteSeconds) {
    if (this.refreshAfterWriteDuration != null) {
      throw new IllegalArgumentException(
          "refreshAfterWriteSeconds and refreshAfterWriteDuration cannot be set together");
    }
    if (this.refreshAfterWriteTimeUnit != null)
      throw new IllegalArgumentException(
          "refreshAfterWriteSeconds and refreshAfterWriteTimeUnit cannot be set together");

    log.warn(
        "refreshAfterWriteSeconds is deprecated and use refreshAfterWriteDuration and refreshAfterWriteTimeUnit instead");
    this.refreshAfterWriteSeconds = refreshAfterWriteSeconds;
    this.refreshAfterWriteDuration = refreshAfterWriteSeconds;
    this.refreshAfterWriteTimeUnit = TimeUnit.SECONDS;
  }

  public void setExpireAfterAccessSeconds(Long expireAfterAccessSeconds) {
    if (this.expireAfterAccessDuration != null) {
      throw new IllegalArgumentException(
          "expireAfterAccessSeconds and expireAfterAccessDuration cannot be set together");
    }
    if (this.expireAfterAccessTimeUnit != null)
      throw new IllegalArgumentException(
          "expireAfterAccessSeconds and expireAfterAccessTimeUnit cannot be set together");

    log.warn(
        "expireAfterAccessSeconds is deprecated and use expireAfterAccessDuration and expireAfterAccessTimeUnit instead");
    this.expireAfterAccessSeconds = expireAfterAccessSeconds;
    this.expireAfterAccessDuration = expireAfterAccessSeconds;
    this.expireAfterAccessTimeUnit = TimeUnit.SECONDS;
  }

  public void setExpireAfterWriteSeconds(Long expireAfterWriteSeconds) {
    if (this.expireAfterWriteDuration != null) {
      throw new IllegalArgumentException(
          "expireAfterWriteSeconds and expireAfterWriteDuration cannot be set together");
    }
    if (this.expireAfterWriteTimeUnit != null)
      throw new IllegalArgumentException(
          "expireAfterWriteSeconds and expireAfterWriteTimeUnit cannot be set together");

    log.warn(
        "expireAfterWriteSeconds is deprecated and use expireAfterWriteDuration and expireAfterWriteTimeUnit instead");
    this.expireAfterWriteSeconds = expireAfterWriteSeconds;
    this.expireAfterWriteDuration = expireAfterWriteSeconds;
    this.expireAfterWriteTimeUnit = TimeUnit.SECONDS;
  }

  public void setRefreshAfterWriteDuration(Long refreshAfterWriteDuration) {
    if (this.refreshAfterWriteSeconds != null) {
      throw new IllegalArgumentException(
          "refreshAfterWriteSeconds and refreshAfterWriteDuration cannot be set together");
    }
    this.refreshAfterWriteDuration = refreshAfterWriteDuration;
  }

  public void setExpireAfterAccessDuration(Long expireAfterAccessDuration) {
    if (this.expireAfterAccessSeconds != null) {
      throw new IllegalArgumentException(
          "expireAfterAccessSeconds and expireAfterAccessDuration cannot be set together");
    }
    this.expireAfterAccessDuration = expireAfterAccessDuration;
  }

  public void setExpireAfterWriteDuration(Long expireAfterWriteDuration) {
    if (this.expireAfterWriteSeconds != null) {
      throw new IllegalArgumentException(
          "expireAfterWriteSeconds and expireAfterWriteDuration cannot be set together");
    }
    this.expireAfterWriteDuration = expireAfterWriteDuration;
  }

  public void setRefreshAfterWriteTimeUnit(TimeUnit refreshAfterWriteTimeUnit) {
    if (this.refreshAfterWriteSeconds != null)
      throw new IllegalArgumentException(
          "refreshAfterWriteTimeUnit and refreshAfterWriteSeconds cannot be set together");
    this.refreshAfterWriteTimeUnit = refreshAfterWriteTimeUnit;
  }

  public void setExpireAfterAccessTimeUnit(TimeUnit expireAfterAccessTimeUnit) {
    if (this.expireAfterAccessSeconds != null)
      throw new IllegalArgumentException(
          "expireAfterAccessTimeUnit and expireAfterAccessSeconds cannot be set together");
    this.expireAfterAccessTimeUnit = expireAfterAccessTimeUnit;
  }

  public void setExpireAfterWriteTimeUnit(TimeUnit expireAfterWriteTimeUnit) {
    if (this.expireAfterWriteSeconds != null)
      throw new IllegalArgumentException(
          "expireAfterWriteTimeUnit and expireAfterWriteSeconds cannot be set together");
    this.expireAfterWriteTimeUnit = expireAfterWriteTimeUnit;
  }
}
