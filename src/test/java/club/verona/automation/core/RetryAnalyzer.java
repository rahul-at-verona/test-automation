package club.verona.automation.core;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Retries a failed {@code @Test} method once before letting it count as a
 * real failure. Applied to every test automatically via
 * {@link RetryTransformer} — no per-test {@code retryAnalyzer = ...} needed.
 *
 * TestNG instantiates a fresh analyzer per test-method invocation (since
 * {@link RetryTransformer} sets the CLASS via {@code setRetryAnalyzer},
 * not a shared instance), so retryCount here is safely scoped to one test
 * method, not the whole suite.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRIES = 1;

    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            return true;
        }
        return false;
    }
}
