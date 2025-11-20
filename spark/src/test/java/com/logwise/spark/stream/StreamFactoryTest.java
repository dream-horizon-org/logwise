package com.logwise.spark.stream;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.google.inject.AbstractModule;
import com.logwise.spark.constants.StreamName;
import com.logwise.spark.guice.injectors.ApplicationInjector;
import com.logwise.spark.services.KafkaService;
import com.logwise.spark.stream.impl.ApplicationLogsStreamToS3;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.lang.reflect.Field;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for StreamFactory.
 *
 * <p>Tests verify that the factory correctly creates stream instances based on stream name.
 */
public class StreamFactoryTest {

  @BeforeMethod
  public void setUp() {
    // Reset the ApplicationInjector before each test
    resetApplicationInjector();

    // Create a test module with mocked dependencies
    AbstractModule testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            // Create a mock config
            Config mockConfig =
                ConfigFactory.parseString(
                    "spark.processing.time.seconds = 60\n"
                        + "s3.path.checkpoint.application = \"/test/checkpoint\"\n"
                        + "s3.path.logs.application = \"/test/logs\"");

            // Create a mock KafkaService
            KafkaService mockKafkaService = mock(KafkaService.class);

            // Bind the mocked dependencies
            bind(Config.class).toInstance(mockConfig);
            bind(KafkaService.class).toInstance(mockKafkaService);
          }
        };

    // Initialize the ApplicationInjector with the test module
    ApplicationInjector.initInjection(testModule);
  }

  @AfterMethod
  public void tearDown() {
    // Clean up the ApplicationInjector after each test
    resetApplicationInjector();
  }

  /** Resets ApplicationInjector singleton using reflection. */
  private static void resetApplicationInjector() {
    try {
      Field field = ApplicationInjector.class.getDeclaredField("applicationInjector");
      field.setAccessible(true);
      field.set(null, null);
    } catch (Exception e) {
      // Ignore reflection errors - reset is best effort
    }
  }

  @Test
  public void testGetStream_WithApplicationLogsStreamToS3_ReturnsCorrectStream() {
    // Act
    Stream stream = StreamFactory.getStream(StreamName.APPLICATION_LOGS_STREAM_TO_S3);

    // Assert
    assertNotNull(stream, "Stream should not be null");
    assertTrue(
        stream instanceof ApplicationLogsStreamToS3,
        "Stream should be instance of ApplicationLogsStreamToS3");
    assertEquals(
        stream.getClass(),
        ApplicationLogsStreamToS3.class,
        "Should create ApplicationLogsStreamToS3 instance");
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testGetStream_WithNullStreamName_ThrowsException() {
    // Act - should throw NullPointerException (switch on null throws NPE)
    // Note: The source code doesn't check for null before the switch statement,
    // so switching on null will throw NullPointerException
    StreamFactory.getStream(null);
  }

  @Test
  public void testGetStream_CalledMultipleTimes_CreatesStreams() {
    // Act - Get streams multiple times
    Stream stream1 = StreamFactory.getStream(StreamName.APPLICATION_LOGS_STREAM_TO_S3);
    Stream stream2 = StreamFactory.getStream(StreamName.APPLICATION_LOGS_STREAM_TO_S3);

    // Assert - Both should be non-null and correct type
    assertNotNull(stream1, "First stream should not be null");
    assertNotNull(stream2, "Second stream should not be null");
    assertTrue(stream1 instanceof ApplicationLogsStreamToS3, "First stream should be correct type");
    assertTrue(
        stream2 instanceof ApplicationLogsStreamToS3, "Second stream should be correct type");
    // Note: Whether they are the same instance or not depends on Guice
    // configuration
    // (singleton vs prototype scope). We just verify they are created successfully.
  }

  @Test
  public void testStreamFactory_IsUtilityClass() {
    // Assert - Verify that StreamFactory follows utility class pattern
    // A utility class should not be instantiable (constructor is private)
    // This is enforced by Lombok's @UtilityClass annotation

    try {
      // Try to get constructor - should fail or be inaccessible
      java.lang.reflect.Constructor<?> constructor = StreamFactory.class.getDeclaredConstructor();

      // If we got here, verify constructor is not public
      assertFalse(
          java.lang.reflect.Modifier.isPublic(constructor.getModifiers()),
          "Utility class constructor should not be public");
    } catch (NoSuchMethodException e) {
      // This is also acceptable - no public constructor exists
      // Test passes
    }
  }

  // Note: We cannot directly test the default case in StreamFactory.getStream()
  // because all enum values are valid. The default case is defensive code
  // that would only be hit if a new enum value is added without updating the
  // switch.
  // This is acceptable - the branch exists for safety but cannot be tested
  // without
  // modifying the enum, which would break the code.
}
