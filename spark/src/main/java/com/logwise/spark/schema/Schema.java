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
//
//string message = 1;
//map<string, string> ddtags = 2;
//google.protobuf.Timestamp timestamp = 3;
//string env = 4;
//string service_name = 5;
//string component_name = 6;
//optional string hostname = 7;
//optional string ddsource = 8;
//optional string source_type = 9;
//optional string status = 10;
//map<string, string> extra = 11;