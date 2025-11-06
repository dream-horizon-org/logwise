package com.logwise.spark.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import java.net.InetAddress;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ApplicationUtils {
  private final ObjectMapper mapper = new ObjectMapper();

  @SneakyThrows
  public List<String> getIpAddresses(String domainName) {
    log.info("Getting IP addresses for domain: {}", domainName);
    return Arrays.stream(InetAddress.getAllByName(domainName))
        .map(InetAddress::getHostAddress)
        .collect(Collectors.toList());
  }

  public String removeSurroundingQuotes(String input) {
    if (input != null && input.startsWith("\"") && input.endsWith("\"")) {
      return input.substring(1, input.length() - 1);
    }
    return input;
  }

  public String convertProtoTimestampToIso(Timestamp ts) {
    Instant instant = Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    return DateTimeFormatter.ISO_INSTANT.format(instant);
  }

  public static java.sql.Timestamp convertProtoTimestampToSqlTimestamp(Timestamp ts) {
    Instant instant = Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos());
    return java.sql.Timestamp.from(instant);
  }

  public String convertMapToJsonString(Map<String, String> map) {
    try {
      return mapper.writeValueAsString(map);
    } catch (Exception e) {
      return "{}";
    }
  }
}
