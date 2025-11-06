package com.logwise.spark.schema;

import com.logwise.spark.constants.Constants;
import lombok.experimental.UtilityClass;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

@UtilityClass
public class Schema {

  public StructType getVectorApplicationLogsSchema() {
    return new StructType()
        .add(Constants.APPLICATION_LOG_COLUMN_MESSAGE, DataTypes.StringType)
        .add(Constants.APPLICATION_LOG_COLUMN_DDTAGS, DataTypes.StringType)
        .add(Constants.APPLICATION_LOG_COLUMN_TIMESTAMP, DataTypes.StringType)
        .add(Constants.APPLICATION_LOG_COLUMN_ENV, DataTypes.StringType)
        .add(Constants.APPLICATION_LOG_COLUMN_SERVICE_NAME, DataTypes.StringType)
        .add(Constants.APPLICATION_LOG_COLUMN_COMPONENT_NAME, DataTypes.StringType)
        .add(Constants.APPLICATION_LOG_COLUMN_HOSTNAME, DataTypes.StringType)
        .add(Constants.APPLICATION_LOG_COLUMN_DDSOURCE, DataTypes.StringType)
        .add(Constants.APPLICATION_LOG_COLUMN_SOURCE_TYPE, DataTypes.StringType)
        .add(Constants.APPLICATION_LOG_COLUMN_STATUS, DataTypes.StringType);
  }
}
