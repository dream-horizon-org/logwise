package com.logwise.spark.feign.exceptions;

public class ServerErrorException extends RuntimeException {
  public ServerErrorException(int status, String body, String reason) {
    super(String.format("Server Error! Status: %d Body: %s Reason: %s", status, body, reason));
  }
}
