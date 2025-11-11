package com.logwise.orchestrator.rest.converter;

import java.lang.annotation.Annotation;
import javax.ws.rs.ext.ParamConverter;

public class DoubleParamConverter extends BaseParamConverter implements ParamConverter<Double> {

  public DoubleParamConverter(Annotation[] annotations) {
    super(annotations);
  }

  @Override
  public Double fromString(String s) {
    return this.parseParam(s, Double::parseDouble);
  }

  @Override
  public String toString(Double d) {
    return d.toString();
  }
}
