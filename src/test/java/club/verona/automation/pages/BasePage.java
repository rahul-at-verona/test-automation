package club.verona.automation.pages;

import club.verona.automation.pages.editors.UiSnapshot;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.function.Predicate;

/**
 * Base for all page objects. Holds the driver and the shared interaction
 * helpers so individual pages contain only locators and screen-specific flow.
 *
 * Interaction strategy (learned the hard way on this app):
 *  - CLICKS: standard WebElement.click() behind a visible+clickable wait.
 *  - READS:  one getPageSource() snapshot parsed client-side (UiSnapshot) —
 *            repeated on-device queries hang the instrumentation on animated
 *            screens, single one-shot lookups for clicks are fine.
 *  - FALLBACK: coordinate taps (tapByCoordinates) for elements that have no
 *            usable locator (e.g. the unlabeled consent checkbox) or screens
 *            that cannot be queried at all (the Home feed).
 */
public abstract class BasePage {

    protected static final Duration TIMEOUT = Duration.ofSeconds(15);
    protected static final Duration POLL = Duration.ofSeconds(2);

    protected final AppiumDriver driver;

    protected BasePage(AppiumDriver driver) {
        this.driver = driver;
    }

    // ---------------- clicking ----------------

    /** Waits until the element is visible AND clickable, then clicks it. */
    protected void click(By locator) {
        WebDriverWait wait = newWait();
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        try {
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        } catch (StaleElementReferenceException e) {
            // re-render between resolve and click — one retry
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        }
    }

    /** Click by accessibility id (content-desc). */
    protected void clickByDesc(String desc) {
        click(AppiumBy.accessibilityId(desc));
    }

    /** Alias of clickByDesc — kept for readability at call sites. */
    protected void tapByDesc(String desc) {
        clickByDesc(desc);
    }

    /** Coordinate-tap fallback (bypasses element resolution entirely). */
    protected void tapByCoordinates(UiSnapshot.Snap node) {
        UiSnapshot.tap(driver, node);
    }

    protected void tapByCoordinates(int x, int y) {
        UiSnapshot.tap(driver, x, y);
    }

    // ---------------- typing ----------------

    /** Waits for the field, clears it, types the text. */
    protected void type(By locator, String text) {
        WebElement el = newWait().until(
                ExpectedConditions.visibilityOfElementLocated(locator));
        el.clear();
        el.sendKeys(text);
    }

    // ---------------- scrolling ----------------

    /**
     * Scrolls the first scrollable container until an element with the exact
     * text is on screen (native UiScrollable — reliable on the profile
     * ScrollView, unlike coordinate swipes).
     */
    protected void scrollIntoViewByText(String text) {
        driver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true))"
                        + ".scrollIntoView(new UiSelector().text(\"" + text + "\"))"));
    }

    // ---------------- snapshot reads & waits ----------------

    protected UiSnapshot snapshot() {
        return UiSnapshot.capture(driver);
    }

    protected UiSnapshot waitForSnapshot(Predicate<UiSnapshot> condition,
                                         long timeoutMs, String description) {
        return UiSnapshot.waitFor(driver, condition, timeoutMs, description);
    }

    protected void pause(long ms) {
        UiSnapshot.sleep(ms);
    }

    protected WebDriverWait newWait() {
        WebDriverWait wait = new WebDriverWait(driver, TIMEOUT);
        wait.pollingEvery(POLL);
        wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }
}
