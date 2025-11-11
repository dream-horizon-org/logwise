package com.dream11.logcentralorchestrator.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** RestException will following code and message will be thrown upon parsing error */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface TypeValidationError {
  String code();

  String message();

  int httpStatusCode() default 400;
}
