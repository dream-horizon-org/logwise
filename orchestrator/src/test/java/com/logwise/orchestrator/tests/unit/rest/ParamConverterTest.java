package com.logwise.orchestrator.tests.unit.rest;

import com.logwise.orchestrator.rest.TypeValidationError;
import com.logwise.orchestrator.rest.converter.*;
import com.logwise.orchestrator.rest.exception.RestException;
import java.lang.annotation.Annotation;
import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for all ParamConverter classes. */
public class ParamConverterTest {

  @Test
  public void testParseParam_WithValidInput_ReturnsParsedValue() {

    IntegerParamConverter converter = new IntegerParamConverter(new Annotation[0]);

    Integer result = converter.fromString("123");

    Assert.assertEquals(result, Integer.valueOf(123));
  }

  @Test
  public void testParseParam_WithInvalidInput_ThrowsException() {

    IntegerParamConverter converter = new IntegerParamConverter(new Annotation[0]);

    try {
      converter.fromString("invalid");
      Assert.fail("Should have thrown exception");
    } catch (NumberFormatException e) {

    }
  }

  @Test
  public void testParseParam_WithInvalidInputAndTypeValidationError_ThrowsRestException() {

    TypeValidationError annotation =
        new TypeValidationError() {
          @Override
          public Class<? extends Annotation> annotationType() {
            return TypeValidationError.class;
          }

          @Override
          public String code() {
            return "INVALID_PARAM";
          }

          @Override
          public String message() {
            return "Invalid parameter";
          }

          @Override
          public int httpStatusCode() {
            return 400;
          }
        };
    IntegerParamConverter converter = new IntegerParamConverter(new Annotation[] {annotation});

    try {
      converter.fromString("invalid");
      Assert.fail("Should have thrown RestException");
    } catch (RestException e) {
      Assert.assertEquals(e.getError().getCode(), "INVALID_PARAM");
      Assert.assertEquals(e.getHttpStatusCode(), 400);
    }
  }

  @Test
  public void testConstructor_WithTypeValidationError_SetsErrorAndStatusCode() {

    TypeValidationError annotation =
        new TypeValidationError() {
          @Override
          public Class<? extends Annotation> annotationType() {
            return TypeValidationError.class;
          }

          @Override
          public String code() {
            return "TEST_CODE";
          }

          @Override
          public String message() {
            return "Test message";
          }

          @Override
          public int httpStatusCode() {
            return 422;
          }
        };

    IntegerParamConverter converter = new IntegerParamConverter(new Annotation[] {annotation});

    Assert.assertNotNull(converter.getError());
    Assert.assertEquals(converter.getError().getCode(), "TEST_CODE");
    Assert.assertEquals(converter.getError().getMessage(), "Test message");
    Assert.assertEquals(converter.getHttpStatusCode(), 422);
  }

  @Test
  public void testConstructor_WithoutTypeValidationError_DoesNotSetError() {

    IntegerParamConverter converter = new IntegerParamConverter(new Annotation[0]);

    Assert.assertNull(converter.getError());
    Assert.assertEquals(converter.getHttpStatusCode(), 0);
  }

  @Test
  public void testIntegerParamConverter_FromString_WithValidInteger_ReturnsInteger() {
    IntegerParamConverter converter = new IntegerParamConverter(new Annotation[0]);
    Integer result = converter.fromString("123");
    Assert.assertEquals(result, Integer.valueOf(123));
  }

  @Test
  public void testIntegerParamConverter_FromString_WithNegativeInteger_ReturnsInteger() {
    IntegerParamConverter converter = new IntegerParamConverter(new Annotation[0]);
    Integer result = converter.fromString("-456");
    Assert.assertEquals(result, Integer.valueOf(-456));
  }

  @Test
  public void testIntegerParamConverter_FromString_WithInvalidInput_ThrowsException() {
    IntegerParamConverter converter = new IntegerParamConverter(new Annotation[0]);
    try {
      converter.fromString("not-a-number");
      Assert.fail("Should have thrown exception");
    } catch (NumberFormatException e) {

    }
  }

  @Test
  public void testIntegerParamConverter_ToString_WithValidInteger_ReturnsString() {
    IntegerParamConverter converter = new IntegerParamConverter(new Annotation[0]);
    String result = converter.toString(123);
    Assert.assertEquals(result, "123");
  }

  @Test
  public void testLongParamConverter_FromString_WithValidLong_ReturnsLong() {
    LongParamConverter converter = new LongParamConverter(new Annotation[0]);
    Long result = converter.fromString("1234567890123");
    Assert.assertEquals(result, Long.valueOf(1234567890123L));
  }

  @Test
  public void testLongParamConverter_FromString_WithNegativeLong_ReturnsLong() {
    LongParamConverter converter = new LongParamConverter(new Annotation[0]);
    Long result = converter.fromString("-4567890123");
    Assert.assertEquals(result, Long.valueOf(-4567890123L));
  }

  @Test
  public void testLongParamConverter_FromString_WithInvalidInput_ThrowsException() {
    LongParamConverter converter = new LongParamConverter(new Annotation[0]);
    try {
      converter.fromString("not-a-number");
      Assert.fail("Should have thrown exception");
    } catch (NumberFormatException e) {

    }
  }

  @Test
  public void testLongParamConverter_ToString_WithValidLong_ReturnsString() {
    LongParamConverter converter = new LongParamConverter(new Annotation[0]);
    String result = converter.toString(1234567890123L);
    Assert.assertEquals(result, "1234567890123");
  }

  @Test
  public void testDoubleParamConverter_FromString_WithValidDouble_ReturnsDouble() {
    DoubleParamConverter converter = new DoubleParamConverter(new Annotation[0]);
    Double result = converter.fromString("123.456");
    Assert.assertEquals(result, Double.valueOf(123.456));
  }

  @Test
  public void testDoubleParamConverter_FromString_WithNegativeDouble_ReturnsDouble() {
    DoubleParamConverter converter = new DoubleParamConverter(new Annotation[0]);
    Double result = converter.fromString("-456.789");
    Assert.assertEquals(result, Double.valueOf(-456.789));
  }

  @Test
  public void testDoubleParamConverter_FromString_WithInvalidInput_ThrowsException() {
    DoubleParamConverter converter = new DoubleParamConverter(new Annotation[0]);
    try {
      converter.fromString("not-a-number");
      Assert.fail("Should have thrown exception");
    } catch (NumberFormatException e) {

    }
  }

  @Test
  public void testDoubleParamConverter_ToString_WithValidDouble_ReturnsString() {
    DoubleParamConverter converter = new DoubleParamConverter(new Annotation[0]);
    String result = converter.toString(123.456);
    Assert.assertEquals(result, "123.456");
  }

  @Test
  public void testFloatParamConverter_FromString_WithValidFloat_ReturnsFloat() {
    FloatParamConverter converter = new FloatParamConverter(new Annotation[0]);
    Float result = converter.fromString("123.456");
    Assert.assertEquals(result, Float.valueOf(123.456f));
  }

  @Test
  public void testFloatParamConverter_FromString_WithNegativeFloat_ReturnsFloat() {
    FloatParamConverter converter = new FloatParamConverter(new Annotation[0]);
    Float result = converter.fromString("-456.789");
    Assert.assertEquals(result, Float.valueOf(-456.789f));
  }

  @Test
  public void testFloatParamConverter_FromString_WithInvalidInput_ThrowsException() {
    FloatParamConverter converter = new FloatParamConverter(new Annotation[0]);
    try {
      converter.fromString("not-a-number");
      Assert.fail("Should have thrown exception");
    } catch (NumberFormatException e) {

    }
  }

  @Test
  public void testFloatParamConverter_ToString_WithValidFloat_ReturnsString() {
    FloatParamConverter converter = new FloatParamConverter(new Annotation[0]);
    String result = converter.toString(123.456f);
    Assert.assertEquals(result, "123.456");
  }
}
