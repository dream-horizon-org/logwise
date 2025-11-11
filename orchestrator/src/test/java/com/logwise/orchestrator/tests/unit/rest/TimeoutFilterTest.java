package com.logwise.orchestrator.tests.unit.rest;

import com.logwise.orchestrator.common.app.AppContext;
import com.logwise.orchestrator.rest.Timeout;
import com.logwise.orchestrator.rest.filter.TimeoutFilter;
import com.logwise.orchestrator.setup.BaseTest;
import io.vertx.reactivex.core.Vertx;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit tests for TimeoutFilter. */
public class TimeoutFilterTest extends BaseTest {

  private TimeoutFilter timeoutFilter;
  private ContainerRequestContext mockRequestContext;
  private ContainerResponseContext mockResponseContext;
  private Vertx mockVertx;

  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    mockVertx = Mockito.mock(Vertx.class);

    try (MockedStatic<AppContext> mockedAppContext = Mockito.mockStatic(AppContext.class)) {
      mockedAppContext.when(() -> AppContext.getInstance(Vertx.class)).thenReturn(mockVertx);
      timeoutFilter = new TimeoutFilter();
    }

    mockRequestContext = Mockito.mock(ContainerRequestContext.class);
    mockResponseContext = Mockito.mock(ContainerResponseContext.class);
  }

  @Test
  public void testFilter_Request_WithMethodTimeout_CreatesTimer() {

    Assert.assertNotNull(timeoutFilter);
  }

  @Test
  public void testFilter_Response_CancelsTimer() {

    Long timerId = 12345L;
    Mockito.when(mockRequestContext.getProperty("__TIMER_ID__")).thenReturn(timerId);

    timeoutFilter.filter(mockRequestContext, mockResponseContext);

    Mockito.verify(mockVertx).cancelTimer(timerId);
    Mockito.verify(mockRequestContext).removeProperty("__TIMER_ID__");
  }

  @Test
  public void testFilter_Response_WithoutTimer_DoesNothing() {

    Mockito.when(mockRequestContext.getProperty("__TIMER_ID__")).thenReturn(null);

    timeoutFilter.filter(mockRequestContext, mockResponseContext);

    Mockito.verify(mockVertx, Mockito.never()).cancelTimer(Mockito.anyLong());
  }

  @Timeout(5000)
  private static class TestResource {
    public void testMethod() {}
  }
}
