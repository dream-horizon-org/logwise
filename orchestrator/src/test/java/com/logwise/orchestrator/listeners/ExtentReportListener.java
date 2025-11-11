package com.logwise.orchestrator.listeners;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import java.io.File;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Extent Reports listener for TestNG to generate HTML test reports. This listener generates reports
 * similar to the example project structure.
 */
public class ExtentReportListener implements ITestListener {

  private static ExtentReports extent;
  private static ThreadLocal<ExtentTest> test = new ThreadLocal<>();

  @Override
  public void onStart(ITestContext context) {

    File testOutputDir = new File("test-output");
    if (!testOutputDir.exists()) {
      testOutputDir.mkdirs();
    }

    ExtentSparkReporter sparkReporter =
        new ExtentSparkReporter("test-output/TestReport/report.html");
    sparkReporter.config().setTheme(Theme.DARK);
    sparkReporter.config().setDocumentTitle("Test Report: log-central-orchestrator");
    sparkReporter.config().setReportName("Test Report: log-central-orchestrator");
    sparkReporter.config().setTimeStampFormat("EEEE, MMMM dd, yyyy, hh:mm a (z)");

    extent = new ExtentReports();
    extent.attachReporter(sparkReporter);
    extent.setSystemInfo("Service Name", "log-central-orchestrator");
    extent.setSystemInfo("Test Type Triggered", "UNIT_TESTS");
    extent.setSystemInfo("Environment", System.getProperty("env", "local"));
  }

  @Override
  public void onTestStart(ITestResult result) {
    String testName = getTestName(result);
    String description = result.getMethod().getDescription();
    if (description == null || description.isEmpty()) {
      description = testName;
    }

    ExtentTest extentTest = extent.createTest(testName, description);

    String className = result.getTestClass().getName();
    String author = className.substring(className.lastIndexOf('.') + 1);
    extentTest.assignAuthor(author);

    extentTest.assignCategory("TestType:Unit");
    extentTest.assignCategory("TestRunType:WhiteBox");

    String methodName = result.getMethod().getMethodName().toLowerCase();
    if (methodName.contains("failure")
        || methodName.contains("error")
        || methodName.contains("exception")
        || methodName.contains("invalid")) {
      extentTest.assignCategory("Purpose:Negative-Tests");
    } else {
      extentTest.assignCategory("Purpose:Positive-Tests");
    }

    if (className.contains("Component")) {
      extentTest.assignCategory("Route:componentSync");
    } else if (className.contains("Metrics")) {
      extentTest.assignCategory("Route:metrics");
    } else if (className.contains("MonitorSparkJob")) {
      extentTest.assignCategory("Route:monitorSparkJob");
    }

    test.set(extentTest);
  }

  @Override
  public void onTestSuccess(ITestResult result) {
    test.get().pass("Test passed");
  }

  @Override
  public void onTestFailure(ITestResult result) {
    test.get().fail(result.getThrowable());
    if (result.getThrowable() != null) {
      test.get().fail("Exception: " + result.getThrowable().getMessage());
    }
  }

  @Override
  public void onTestSkipped(ITestResult result) {
    test.get().skip(result.getThrowable());
    if (result.getThrowable() != null) {
      test.get().skip("Skipped: " + result.getThrowable().getMessage());
    }
  }

  @Override
  public void onFinish(ITestContext context) {
    extent.flush();
  }

  /**
   * Extracts a readable test name from the test result. Converts method names like
   * "testSyncHandler_WithApplicationComponentType_ReturnsSuccessResponse" to "Test sync handler
   * with application component type returns success response"
   */
  private String getTestName(ITestResult result) {
    String methodName = result.getMethod().getMethodName();

    if (methodName.startsWith("test")) {
      methodName = methodName.substring(4);
    }

    String[] parts = methodName.split("_");
    StringBuilder testName = new StringBuilder("Test ");

    for (int i = 0; i < parts.length; i++) {
      String part = parts[i];
      if (part.isEmpty()) continue;

      testName.append(Character.toUpperCase(part.charAt(0)));
      if (part.length() > 1) {
        testName.append(part.substring(1).toLowerCase());
      }

      if (i < parts.length - 1) {
        testName.append(" ");
      }
    }

    return testName.toString();
  }
}
