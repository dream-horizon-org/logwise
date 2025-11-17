package com.logwise.orchestrator.util;

import com.google.inject.ConfigurationException;
import com.logwise.orchestrator.common.app.AppContext;
import com.logwise.orchestrator.dto.entity.*;
import com.logwise.orchestrator.dto.entity.ServiceDetails;
import io.reactivex.Maybe;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowSet;
import java.time.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
public class ApplicationUtils {

  public List<Map<String, Object>> rowSetToMapList(RowSet<Row> rows) {
    return StreamSupport.stream(rows.spliterator(), true)
        .map(
            row -> {
              Map<String, Object> map = new HashMap<>();
              for (int i = 0; i < row.size(); i++) {
                Object value = row.getValue(i);
                if (value instanceof LocalDateTime) {
                  value =
                      Date.from(row.getLocalDateTime(i).atZone(ZoneId.systemDefault()).toInstant());
                }
                map.put(row.getColumnName(i), value);
              }
              return map;
            })
        .collect(Collectors.toList());
  }

  public <T> Maybe<T> executeBlockingCallable(Callable<T> blockingCallable) {
    return AppContext.getInstance(Vertx.class)
        .rxExecuteBlocking(
            promise -> {
              try {
                promise.complete(blockingCallable.call());
              } catch (Exception e) {
                promise.fail(e);
              }
            });
  }

  public ServiceDetails getServiceFromObjectKey(String logPath) {
    Pattern pattern =
        Pattern.compile("environment_name=(.+?)/component_type=(.+?)/service_name=(.+?)/");
    Matcher matcher = pattern.matcher(logPath);
    if (matcher.find()) {
      return ServiceDetails.builder()
          .environmentName(matcher.group(1))
          .componentType(matcher.group(2))
          .serviceName(matcher.group(3))
          .build();
    }
    log.error("Error in getting service details from object key: {}", logPath);
    return null;
  }

  public <T> T getGuiceInstance(Class<T> klazz, String name) {
    try {
      return AppContext.getInstance(klazz, name);
    } catch (ConfigurationException e) {
      log.debug("Guice instance for class: {} name: {} not found.", klazz, name);
      return null;
    } catch (Exception e) {
      log.error("Error in getting Guice instance for class: {} name: {}", klazz, name, e);
      return null;
    }
  }
}
