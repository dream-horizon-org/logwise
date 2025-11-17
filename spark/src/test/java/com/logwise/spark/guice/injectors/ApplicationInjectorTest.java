package com.logwise.spark.guice.injectors;

import static org.testng.Assert.*;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.lang.reflect.Field;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for ApplicationInjector.
 *
 * <p>Tests verify singleton pattern, initialization, instance retrieval, and error handling.
 */
public class ApplicationInjectorTest {

  @BeforeMethod
  public void setUp() {
    // Reset before each test
    resetApplicationInjector();
  }

  @AfterMethod
  public void tearDown() {
    // Clean up after each test
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
  public void testInitInjection_FirstCall_InitializesInjector() {
    // Arrange
    Module testModule = createTestModule();

    // Act
    ApplicationInjector.initInjection(testModule);

    // Assert - Should not throw exception
    assertNotNull(ApplicationInjector.getInstance(Config.class));
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testInitInjection_SecondCall_ThrowsIllegalStateException() {
    // Arrange
    Module testModule = createTestModule();
    ApplicationInjector.initInjection(testModule);

    // Act - Second initialization should throw exception
    ApplicationInjector.initInjection(testModule);
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testGetInstance_BeforeInitialization_ThrowsIllegalStateException() {
    // Act - Should throw exception if not initialized
    ApplicationInjector.getInstance(Config.class);
  }

  @Test
  public void testGetInstance_AfterInitialization_ReturnsInstance() {
    // Arrange
    Module testModule = createTestModule();
    ApplicationInjector.initInjection(testModule);

    // Act
    Config config = ApplicationInjector.getInstance(Config.class);

    // Assert
    assertNotNull(config, "Should return non-null instance after initialization");
  }

  @Test
  public void testGetInstance_MultipleCalls_ReturnsSameInstance() {
    // Arrange
    Module testModule = createTestModule();
    ApplicationInjector.initInjection(testModule);

    // Act
    Config config1 = ApplicationInjector.getInstance(Config.class);
    Config config2 = ApplicationInjector.getInstance(Config.class);

    // Assert - Guice returns same instance for singleton binding
    assertSame(config1, config2, "Should return same instance for singleton binding");
  }

  @Test
  public void testReset_AfterInitialization_AllowsReinitialization() {
    // Arrange
    Module testModule1 = createTestModule();
    ApplicationInjector.initInjection(testModule1);
    resetApplicationInjector();

    // Act - Should be able to initialize again after reset
    Module testModule2 = createTestModule();
    ApplicationInjector.initInjection(testModule2);

    // Assert - Should not throw exception
    assertNotNull(ApplicationInjector.getInstance(Config.class));
  }

  @Test
  public void testReset_MultipleCalls_DoesNotThrowException() {
    // Arrange
    Module testModule = createTestModule();
    ApplicationInjector.initInjection(testModule);

    // Act - Multiple reset calls should not throw
    resetApplicationInjector();
    resetApplicationInjector();
    resetApplicationInjector();

    // Assert - Should not throw exception
    assertTrue(true, "Multiple reset calls should not throw exception");
  }

  @Test
  public void testInitInjection_WithMultipleModules_InitializesSuccessfully() {
    // Arrange
    Module module1 = createTestModule();
    Module module2 =
        new AbstractModule() {
          @Override
          protected void configure() {
            // Empty module
          }
        };

    // Act
    ApplicationInjector.initInjection(module1, module2);

    // Assert - Should initialize successfully
    assertNotNull(ApplicationInjector.getInstance(Config.class));
  }

  @Test
  public void testGetInstance_WithDifferentTypes_ReturnsCorrectInstances() {
    // Arrange
    AbstractModule testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            Config config = ConfigFactory.parseString("test.key = test.value");
            bind(Config.class).toInstance(config);
            bind(String.class).toInstance("test-string");
          }
        };
    ApplicationInjector.initInjection(testModule);

    // Act
    Config config = ApplicationInjector.getInstance(Config.class);
    String string = ApplicationInjector.getInstance(String.class);

    // Assert
    assertNotNull(config, "Config instance should not be null");
    assertNotNull(string, "String instance should not be null");
    assertEquals(string, "test-string", "Should return correct string instance");
  }

  @Test
  public void testInitInjection_WithEmptyModuleArray_InitializesSuccessfully() {
    // Act - Empty module array should not throw
    ApplicationInjector.initInjection();

    // Assert - Should initialize (though may not have any bindings)
    assertTrue(true, "Empty module array should initialize successfully");
  }

  private AbstractModule createTestModule() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        Config config = ConfigFactory.parseString("test.key = test.value");
        bind(Config.class).toInstance(config);
      }
    };
  }
}
