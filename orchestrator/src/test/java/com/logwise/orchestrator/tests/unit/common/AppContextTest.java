package com.logwise.orchestrator.tests.unit.common;

import static org.testng.Assert.*;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.logwise.orchestrator.common.app.AppContext;
import java.util.Arrays;
import java.util.Collections;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for AppContext singleton class. */
public class AppContextTest {

  @BeforeMethod
  public void setUp() {
    // Reset AppContext before each test
    AppContext.reset();
  }

  @AfterMethod
  public void tearDown() {
    // Clean up after each test
    AppContext.reset();
  }

  @Test
  public void testInitialize_FirstCall_InitializesContext() {
    // Arrange
    Module testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(String.class).toInstance("test-value");
          }
        };

    // Act
    AppContext.initialize(Collections.singletonList(testModule));

    // Assert
    String instance = AppContext.getInstance(String.class);
    assertNotNull(instance);
    assertEquals(instance, "test-value");
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testInitialize_SecondCall_ThrowsException() {
    // Arrange
    Module testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(String.class).toInstance("test-value");
          }
        };

    // Act
    AppContext.initialize(Collections.singletonList(testModule));
    // Second initialization should throw exception
    AppContext.initialize(Collections.singletonList(testModule));
  }

  @Test
  public void testInitialize_WithMultipleModules_InitializesSuccessfully() {
    // Arrange
    Module module1 =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(String.class).toInstance("value1");
          }
        };
    Module module2 =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(Integer.class).toInstance(42);
          }
        };

    // Act
    AppContext.initialize(Arrays.asList(module1, module2));

    // Assert
    String stringValue = AppContext.getInstance(String.class);
    Integer intValue = AppContext.getInstance(Integer.class);
    assertEquals(stringValue, "value1");
    assertEquals(intValue, Integer.valueOf(42));
  }

  @Test
  public void testGetInstance_WithClass_ReturnsInstance() {
    // Arrange
    Module testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(String.class).toInstance("test-instance");
          }
        };
    AppContext.initialize(Collections.singletonList(testModule));

    // Act
    String instance = AppContext.getInstance(String.class);

    // Assert
    assertNotNull(instance);
    assertEquals(instance, "test-instance");
  }

  @Test
  public void testGetInstance_WithClassAndName_ReturnsNamedInstance() {
    // Arrange
    Module testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(String.class)
                .annotatedWith(com.google.inject.name.Names.named("test-name"))
                .toInstance("named-instance");
          }
        };
    AppContext.initialize(Collections.singletonList(testModule));

    // Act
    String instance = AppContext.getInstance(String.class, "test-name");

    // Assert
    assertNotNull(instance);
    assertEquals(instance, "named-instance");
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testGetInstance_BeforeInitialization_ThrowsException() {
    // Act - Should throw exception if not initialized
    AppContext.getInstance(String.class);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testGetInstance_WithName_BeforeInitialization_ThrowsException() {
    // Act - Should throw exception if not initialized
    AppContext.getInstance(String.class, "test-name");
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testGetInstance_DeprecatedMethod_ReturnsAppContext() {
    // Arrange
    Module testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            // Empty module
          }
        };
    AppContext.initialize(Collections.singletonList(testModule));

    // Act
    AppContext instance = AppContext.getInstance();

    // Assert
    assertNotNull(instance);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testGetInstance_DeprecatedMethod_BeforeInitialization_ThrowsException() {
    // Act - Should throw exception if not initialized
    try {
      AppContext.getInstance();
      fail("Should have thrown RuntimeException");
    } catch (RuntimeException e) {
      assertTrue(e.getMessage().contains("not initialized"));
    }
  }

  @Test
  public void testReset_AfterInitialization_AllowsReinitialization() {
    // Arrange
    Module testModule1 =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(String.class).toInstance("value1");
          }
        };
    AppContext.initialize(Collections.singletonList(testModule1));
    AppContext.reset();

    // Act - Should be able to initialize again after reset
    Module testModule2 =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(String.class).toInstance("value2");
          }
        };
    AppContext.initialize(Collections.singletonList(testModule2));

    // Assert
    String instance = AppContext.getInstance(String.class);
    assertEquals(instance, "value2");
  }

  @Test
  public void testReset_MultipleCalls_DoesNotThrowException() {
    // Arrange
    Module testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(String.class).toInstance("test");
          }
        };
    AppContext.initialize(Collections.singletonList(testModule));

    // Act - Multiple reset calls should not throw
    AppContext.reset();
    AppContext.reset();
    AppContext.reset();

    // Assert - Should not throw exception
    assertTrue(true, "Multiple reset calls should not throw exception");
  }

  @Test
  public void testGetInstance_MultipleCalls_ReturnsSameInstance() {
    // Arrange
    Module testModule =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(String.class).toInstance("singleton");
          }
        };
    AppContext.initialize(Collections.singletonList(testModule));

    // Act
    String instance1 = AppContext.getInstance(String.class);
    String instance2 = AppContext.getInstance(String.class);
    String instance3 = AppContext.getInstance(String.class);

    // Assert - Guice returns same instance for singleton binding
    assertSame(instance1, instance2);
    assertSame(instance2, instance3);
  }
}
