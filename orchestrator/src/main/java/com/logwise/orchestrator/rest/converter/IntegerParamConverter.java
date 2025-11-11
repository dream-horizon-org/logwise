package com.logwise.orchestrator.rest.converter;

import java.lang.annotation.Annotation;
import javax.ws.rs.ext.ParamConverter;

public class IntegerParamConverter extends BaseParamConverter implements ParamConverter<Integer> {

  public IntegerParamConverter(Annotation[] annotations) {
    super(annotations);
  }

  @Override
  public Integer fromString(String s) {
    return this.parseParam(s, Integer::parseInt);
  }

  @Override
  public String toString(Integer i) {
    return i.toString();
  }
}
