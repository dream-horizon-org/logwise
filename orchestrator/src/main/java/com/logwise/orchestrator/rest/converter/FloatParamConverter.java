package com.logwise.orchestrator.rest.converter;

import java.lang.annotation.Annotation;
import javax.ws.rs.ext.ParamConverter;

public class FloatParamConverter extends BaseParamConverter implements ParamConverter<Float> {

  public FloatParamConverter(Annotation[] annotations) {
    super(annotations);
  }

  @Override
  public Float fromString(String s) {
    return this.parseParam(s, Float::parseFloat);
  }

  @Override
  public String toString(Float f) {
    return f.toString();
  }
}
