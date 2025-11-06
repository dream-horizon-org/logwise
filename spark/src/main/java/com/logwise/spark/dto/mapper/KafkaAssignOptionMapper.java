package com.logwise.spark.dto.mapper;

import com.logwise.spark.dto.entity.KafkaAssignOption;
import com.logwise.spark.dto.entity.StartingOffsetsByTimestampOption;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;

@UtilityClass
public class KafkaAssignOptionMapper {
  public Function<StartingOffsetsByTimestampOption, KafkaAssignOption> toKafkaAssignOption =
      offset -> {
        KafkaAssignOption kafkaAssignOption = new KafkaAssignOption();
        offset
            .getOffsetByTimestamp()
            .forEach(
                (topic, partitionMap) -> {
                  kafkaAssignOption.addTopic(
                      topic,
                      partitionMap.keySet().stream()
                          .map(Integer::valueOf)
                          .collect(Collectors.toList()));
                });
        return kafkaAssignOption;
      };
}
