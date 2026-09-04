package club.verona.automation.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Console/CI-log visibility into the run: when the suite starts and ends,
 * when each test method starts, and its final status + description when it
 * ends — independent of the ReportPortal dashboard.
 */
public class TestRunListener implements ISuiteListener, ITestListener {

    private static final Logger log = LoggerFactory.getLogger(TestRunListener.class);

    @Override
    public void onStart(ISuite suite) {
        log.info("===== TEST RUN STARTED: {} =====", suite.getName());
    }

    @Override
    public void onFinish(ISuite suite) {
        log.info("===== TEST RUN FINISHED: {} =====", suite.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        log.info("STARTED  [{}#{}] {}", className(result), result.getMethod().getMethodName(),
                description(result));
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        logOutcome("PASSED", result);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        logOutcome("FAILED", result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        logOutcome("SKIPPED", result);
    }

    private void logOutcome(String status, ITestResult result) {
        log.info("{}  [{}#{}] {}", status, className(result), result.getMethod().getMethodName(),
                description(result));
    }

    private String className(ITestResult result) {
        return result.getTestClass().getRealClass().getSimpleName();
    }

    private String description(ITestResult result) {
        String description = result.getMethod().getDescription();
        return description != null && !description.isBlank() ? description : "(no description)";
    }
}
