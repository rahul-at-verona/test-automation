package club.verona.automation.tests;

import club.verona.automation.core.DriverFactory;
import club.verona.automation.flows.LoginFlow;
import io.appium.java_client.android.AndroidDriver;
import org.testng.annotations.BeforeClass;

/**
 * Base for suites that need an authenticated session. Before the class:
 *   1. starts a fresh Appium session      -> cold-starts to the landing screen
 *   2. clears app storage, then relaunches -> next launch is logged out
 *   3. runs LoginFlow                      -> phone 8799731416 / OTP 123456
 *                                             through interstitials to Home
 * Subclasses then start from the Home tab (logged in).
 */
public abstract class LoggedInBaseTest extends BaseTest {

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        driver = createFreshSession();
//        DriverFactory.clearAppStorage(driver);
        ((AndroidDriver) driver).activateApp("club.verona");
        LoginFlow.login(driver);           // land on Home, authenticated
    }
}
