package club.verona.automation.tests;

import club.verona.automation.core.DriverFactory;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Fresh Appium session per test class, with explicit cleanup of the
 * UiAutomator2 instrumentation between sessions. Reusing one session across
 * classes — or starting a new one while the previous (possibly hung) server
 * instance lingers on the device — reliably breaks on this app.
 */
public abstract class BaseTest {

    protected static AppiumDriver driver;

    @BeforeClass(alwaysRun = true)
    public void setUp() {
        // Every suite starts logged out. Clearing here (once per class) means
        // classes that don't reset per-method still get a clean slate.
        // Clearing needs a live session (the BestQ grid has no local adb
        // access, so it clears via the session's own driver), so create the
        // session first, then wipe storage and relaunch into the fresh state.
        driver = createFreshSession();
        DriverFactory.clearAppStorage(driver);
        ((AndroidDriver) driver).activateApp("club.verona");
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        quitQuietly();
    }

    /** Quit any old session, kill leftover instrumentation, start clean. */
    protected static AppiumDriver createFreshSession() {
        quitQuietly();
        DriverFactory.cleanupInstrumentation();
        pause(3_000);
        try {
            driver = DriverFactory.create();
        } catch (Exception first) {
            System.out.println("Rahullll erooor "+first.getMessage());
            DriverFactory.cleanupInstrumentation();
            pause(10_000);
            driver = DriverFactory.create();
        }
        return driver;
    }

    /** For mid-class recovery when the instrumentation hangs. */
    protected static AppiumDriver resetSession() {
        return createFreshSession();
    }

    private static void quitQuietly() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) { }
            driver = null;
        }
    }

    private static void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
