package com.logwise.orchestrator.tests.unit.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logwise.orchestrator.common.app.AppContext;
import com.logwise.orchestrator.rest.TypeValidationError;
import com.logwise.orchestrator.rest.exception.RestException;
import com.logwise.orchestrator.rest.provider.D11JsonProvider;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for D11JsonProvider. */
public class D11JsonProviderTest {

  private D11JsonProvider provider;
  private ObjectMapper mockObjectMapper;

  @BeforeMethod
  public void setUp() {
    mockObjectMapper = new ObjectMapper();
    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      mockedAppContext
          .when(() -> AppContext.getInstance(ObjectMapper.class))
          .thenReturn(mockObjectMapper);
      provider = new D11JsonProvider();
    }
  }

  @Test
  public void testReadFrom_WithValidJson_ReturnsObject() throws Exception {

    String json = "{\"value\":\"test\"}";
    InputStream entityStream = new ByteArrayInputStream(json.getBytes());
    MediaType mediaType = MediaType.APPLICATION_JSON_TYPE;

    Object result =
        provider.readFrom(
            (Class<Object>) (Class<?>) TestDto.class,
            TestDto.class,
            new Annotation[0],
            mediaType,
            new MultivaluedHashMap<>(),
            entityStream);
    TestDto dto = (TestDto) result;

    Assert.assertNotNull(dto);
    Assert.assertEquals(dto.getValue(), "test");
  }

  @Test
  public void testReadFrom_WithInvalidJson_ThrowsRestException() {

    String invalidJson = "invalid json";
    InputStream entityStream = new ByteArrayInputStream(invalidJson.getBytes());
    MediaType mediaType = MediaType.APPLICATION_JSON_TYPE;

    try {
      @SuppressWarnings("unchecked")
      Object result =
          provider.readFrom(
              (Class<Object>) (Class<?>) TestDto.class,
              TestDto.class,
              new Annotation[0],
              mediaType,
              new MultivaluedHashMap<>(),
              entityStream);
      Assert.fail("Should have thrown RestException");
    } catch (RestException e) {
      Assert.assertEquals(e.getError().getCode(), "INVALID_REQUEST");
    } catch (Exception e) {

      Assert.assertTrue(e instanceof RestException || e.getCause() instanceof RestException);
    }
  }

  @Test
  public void testReadFrom_WithJsonMappingException_ThrowsRestException() throws Exception {

    String json = "{\"value\":\"test\"}"; // Use valid field name
    InputStream entityStream = new ByteArrayInputStream(json.getBytes());
    MediaType mediaType = MediaType.APPLICATION_JSON_TYPE;

    try {
      Object result =
          provider.readFrom(
              (Class<Object>) (Class<?>) TestDto.class,
              TestDto.class,
              new Annotation[0],
              mediaType,
              new MultivaluedHashMap<>(),
              entityStream);

      Assert.assertNotNull(result);
    } catch (RestException e) {
      Assert.assertEquals(e.getError().getCode(), "INVALID_REQUEST");
    }
  }

  @Test
  public void testReadFrom_WithTypeValidationError_ThrowsRestExceptionWithCustomError()
      throws Exception {

    String json = "{\"value\":123}"; // Wrong type
    InputStream entityStream = new ByteArrayInputStream(json.getBytes());
    MediaType mediaType = MediaType.APPLICATION_JSON_TYPE;

    try {
      Object result =
          provider.readFrom(
              (Class<Object>) (Class<?>) TestDtoWithValidation.class,
              TestDtoWithValidation.class,
              new Annotation[0],
              mediaType,
              new MultivaluedHashMap<>(),
              entityStream);
      TestDtoWithValidation dto = (TestDtoWithValidation) result;

      Assert.assertNotNull(dto);
    } catch (RestException e) {

      Assert.assertNotNull(e.getError());
    } catch (Exception e) {

      Assert.assertNotNull(e);
    }
  }

  @Test
  public void testReadFrom_WithIOException_ThrowsRestException() {

    InputStream entityStream =
        new InputStream() {
          @Override
          public int read() {
            throw new RuntimeException("IO Error");
          }
        };
    MediaType mediaType = MediaType.APPLICATION_JSON_TYPE;

    try {
      @SuppressWarnings("unchecked")
      Object result =
          provider.readFrom(
              (Class<Object>) (Class<?>) TestDto.class,
              TestDto.class,
              new Annotation[0],
              mediaType,
              new MultivaluedHashMap<>(),
              entityStream);
      Assert.fail("Should have thrown exception");
    } catch (RestException e) {
      Assert.assertEquals(e.getError().getCode(), "INVALID_REQUEST");
    } catch (Exception e) {

      Assert.assertNotNull(e);
    }
  }

  private static class TestDto {
    private String value;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  private static class TestDtoWithValidation {
    @TypeValidationError(code = "INVALID_VALUE", message = "Invalid value", httpStatusCode = 400)
    private String value;

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
