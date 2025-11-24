package com.logwise.orchestrator.tests.unit.rest;

import com.logwise.orchestrator.rest.converter.DoubleParamConverter;
import com.logwise.orchestrator.rest.converter.FloatParamConverter;
import com.logwise.orchestrator.rest.converter.IntegerParamConverter;
import com.logwise.orchestrator.rest.converter.LongParamConverter;
import com.logwise.orchestrator.rest.provider.ParamConverterProvider;
import javax.ws.rs.ext.ParamConverter;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for ParamConverterProvider. */
public class ParamConverterProviderTest {

  private ParamConverterProvider provider;

  @Test
  public void setUp() {
    provider = new ParamConverterProvider();
  }

  @Test
  public void testGetConverter_WithLong_ReturnsLongParamConverter() {

    setUp();

    ParamConverter<Long> converter =
        provider.getConverter(Long.class, null, new java.lang.annotation.Annotation[0]);

    Assert.assertNotNull(converter);
    Assert.assertTrue(converter instanceof LongParamConverter);
  }

  @Test
  public void testGetConverter_WithInteger_ReturnsIntegerParamConverter() {

    setUp();

    ParamConverter<Integer> converter =
        provider.getConverter(Integer.class, null, new java.lang.annotation.Annotation[0]);

    Assert.assertNotNull(converter);
    Assert.assertTrue(converter instanceof IntegerParamConverter);
  }

  @Test
  public void testGetConverter_WithDouble_ReturnsDoubleParamConverter() {

    setUp();

    ParamConverter<Double> converter =
        provider.getConverter(Double.class, null, new java.lang.annotation.Annotation[0]);

    Assert.assertNotNull(converter);
    Assert.assertTrue(converter instanceof DoubleParamConverter);
  }

  @Test
  public void testGetConverter_WithFloat_ReturnsFloatParamConverter() {

    setUp();

    ParamConverter<Float> converter =
        provider.getConverter(Float.class, null, new java.lang.annotation.Annotation[0]);

    Assert.assertNotNull(converter);
    Assert.assertTrue(converter instanceof FloatParamConverter);
  }

  @Test
  public void testGetConverter_WithUnsupportedType_ReturnsNull() {

    setUp();

    ParamConverter<String> converter =
        provider.getConverter(String.class, null, new java.lang.annotation.Annotation[0]);

    Assert.assertNull(converter);
  }

  @Test
  public void testGetConverter_WithAnnotations_PassesAnnotations() {

    setUp();
    java.lang.annotation.Annotation[] annotations = new java.lang.annotation.Annotation[0];

    ParamConverter<Long> converter = provider.getConverter(Long.class, null, annotations);

    Assert.assertNotNull(converter);
    Assert.assertTrue(converter instanceof LongParamConverter);
  }
}
