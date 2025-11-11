package com.dream11.logcentralorchestrator.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Route {

  String path();

  HttpMethod httpMethod() default HttpMethod.GET;

  String produces() default "application/json";

  String consumes() default "application/json";

  String[] requiredHeaders() default {};

  String[] requiredQueryParams() default {};

  String[] requiredBodyParams() default {};

  long timeout() default 20_000L;
}
