package com.logwise.spark.utils;

import com.logwise.spark.dto.entity.KafkaReadStreamOptions;
import com.logwise.spark.guice.injectors.ApplicationInjector;
import com.logwise.spark.listeners.SparkStageListener;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.scheduler.SparkListenerInterface;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

@Slf4j
@UtilityClass
public class SparkUtils {
  private final ObjectMapper mapper = new ObjectMapper();

  public List<SparkListenerInterface> getSparkListeners() {
    return Collections.singletonList(ApplicationInjector.getInstance(SparkStageListener.class));
  }

  public Dataset<Row> getKafkaReadStream(
      SparkSession sparkSession, KafkaReadStreamOptions kafkaReadStreamOptions) {
    Map<String, String> kafkaReadStreamOptionsMap =
        mapper
            .convertValue(kafkaReadStreamOptions, new TypeReference<Map<String, String>>() {})
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    log.info("Starting kafka stream with kafkaReadStreamOptions: " + kafkaReadStreamOptionsMap);
    return sparkSession.readStream().format("kafka").options(kafkaReadStreamOptionsMap).load();
  }
}
