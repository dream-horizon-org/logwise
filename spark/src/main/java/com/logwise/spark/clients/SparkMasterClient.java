package com.logwise.spark.clients;

import com.logwise.spark.dto.response.SparkMasterJsonResponse;
import feign.RequestLine;

public interface SparkMasterClient {
  @RequestLine("GET /json")
  SparkMasterJsonResponse json();
}
