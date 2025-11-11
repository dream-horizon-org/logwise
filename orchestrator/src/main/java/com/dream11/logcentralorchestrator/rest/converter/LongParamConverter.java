package com.dream11.logcentralorchestrator.rest.converter;

import java.lang.annotation.Annotation;
import javax.ws.rs.ext.ParamConverter;

public class LongParamConverter extends BaseParamConverter implements ParamConverter<Long> {

  public LongParamConverter(Annotation[] annotations) {
    super(annotations);
  }

  @Override
  public Long fromString(String s) {
    return this.parseParam(s, Long::parseLong);
  }

  @Override
  public String toString(Long l) {
    return l.toString();
  }
}
