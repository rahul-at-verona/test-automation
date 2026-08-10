package club.verona.automation.tests;

import club.verona.automation.core.DriverFactory;
import club.verona.automation.flows.LoginFlow;
import org.testng.annotations.BeforeClass;

/**
 * Base for suites that need an authenticated session. Before the class:
 *   1. clears app storage (adb pm clear)  -> next launch is logged out
 *   2. starts a fresh Appium session      -> cold-starts to the landing screen
 *   3. runs LoginFlow                      -> phone 8799731416 / OTP 123456
 *                                             through interstitials to Home
 * Subclasses then start from the Home tab (logged in).
 */
public abstract class LoggedInBaseTest extends BaseTest {

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        DriverFactory.clearAppStorage();   // wipe before the session cold-starts
        driver = createFreshSession();
        LoginFlow.login(driver);           // land on Home, authenticated
    }
}
