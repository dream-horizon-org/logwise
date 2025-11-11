package com.logwise.orchestrator.rest.healthcheck;

import com.google.common.collect.ImmutableList;
import com.logwise.orchestrator.common.entity.VertxEntity;
import com.logwise.orchestrator.common.util.CollectionUtils;
import com.logwise.orchestrator.common.util.JsonUtils;
import com.logwise.orchestrator.common.util.MapUtils;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class HealthCheckResponse extends VertxEntity {

  @Getter private final List<Check> checks;

  @Override
  public JsonObject toJson() {
    val json =
        JsonUtils.jsonMerge(
            ImmutableList.of(
                JsonUtils.jsonFrom("status", getStatus().getName()),
                JsonUtils.jsonFrom(
                    "checks", JsonUtils.jsonFrom(MapUtils.map(Check::toJson, getChecksAsMap())))));
    // Adding this extra explict data key until we have a workaround for error case
    return JsonUtils.jsonFrom("data", json);
  }

  public Status getStatus() {
    val allChecksUp = CollectionUtils.all(kv -> Status.UP.equals(kv.getStatus()), this.checks);
    return Boolean.TRUE.equals(allChecksUp) ? Status.UP : Status.DOWN;
  }

  private Map<String, Check> getChecksAsMap() {
    return CollectionUtils.indexBy(Check::getType, checks);
  }

  @Getter
  @ToString
  @RequiredArgsConstructor()
  enum Status {
    UP("UP"),
    DOWN("DOWN");

    private final String name;
  }

  @Value
  static class Check extends VertxEntity {
    String error;
    JsonObject response;
    Status status;
    String type;

    Check(String type, Throwable error) {
      this.type = type;
      this.response = null;
      this.error = error.toString();
      this.status = Status.DOWN;
    }

    Check(String type, JsonObject response) {
      this.type = type;
      this.response = response;
      this.error = null;
      this.status = Status.UP;
    }

    @Override
    public JsonObject toJson() {
      return super.toJson(true);
    }
  }
}
