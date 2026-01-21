package com.logwise.spark.feign.exceptions;

public class FeignClientException extends RuntimeException {
  public FeignClientException(int status, String body, String reason) {
    super(
        String.format(
            "Unexpected Feign Client Error! Status: %d Body: %s Reason: %s", status, body, reason));
  }
}
