package com.logwise.spark.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.spark.dto.entity.KafkaReadStreamOptions;
import com.logwise.spark.guice.injectors.ApplicationInjector;
import com.logwise.spark.listeners.SparkStageListener;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.scheduler.SparkListenerInterface;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

@Slf4j
public class SparkUtils {
  private static final SparkUtils INSTANCE = new SparkUtils();

  private final ObjectMapper mapper;
  private final Supplier<SparkListenerInterface> listenerSupplier;

  // Default constructor for production use
  public SparkUtils() {
    this(new ObjectMapper(), () -> ApplicationInjector.getInstance(SparkStageListener.class));
  }

  // Package-private constructor for testing
  SparkUtils(ObjectMapper mapper, Supplier<SparkListenerInterface> listenerSupplier) {
    this.mapper = mapper;
    this.listenerSupplier = listenerSupplier;
  }

  // Static convenience methods for backward compatibility
  public static List<SparkListenerInterface> getSparkListeners() {
    return INSTANCE.getSparkListenersInstance();
  }

  public static Dataset<Row> getKafkaReadStream(
      SparkSession sparkSession, KafkaReadStreamOptions kafkaReadStreamOptions) {
    return INSTANCE.getKafkaReadStreamInstance(sparkSession, kafkaReadStreamOptions);
  }

  // Instance methods
  public List<SparkListenerInterface> getSparkListenersInstance() {
    return Collections.singletonList(listenerSupplier.get());
  }

  public Dataset<Row> getKafkaReadStreamInstance(
      SparkSession sparkSession, KafkaReadStreamOptions kafkaReadStreamOptions) {
    return getKafkaReadStream(
        sparkSession,
        kafkaReadStreamOptions,
        session ->
            session
                .readStream()
                .format("kafka")
                .options(getKafkaOptionsMap(kafkaReadStreamOptions))
                .load());
  }

  // Package-private method for testing - allows injecting stream reader behavior
  Dataset<Row> getKafkaReadStream(
      SparkSession sparkSession,
      KafkaReadStreamOptions kafkaReadStreamOptions,
      Function<SparkSession, Dataset<Row>> streamReaderFunction) {
    Map<String, String> kafkaReadStreamOptionsMap = getKafkaOptionsMap(kafkaReadStreamOptions);
    log.info("Starting kafka stream with kafkaReadStreamOptions: " + kafkaReadStreamOptionsMap);
    return streamReaderFunction.apply(sparkSession);
  }

  private Map<String, String> getKafkaOptionsMap(KafkaReadStreamOptions kafkaReadStreamOptions) {
    return mapper
        .convertValue(kafkaReadStreamOptions, new TypeReference<Map<String, String>>() {})
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
