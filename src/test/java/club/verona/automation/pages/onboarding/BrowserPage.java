package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Chrome, as opened by the app's external links (Terms of Use / Privacy
 * Policy). Chrome's native views are stable, so standard locators and
 * isDisplayed() work here.
 *
 * Verified locators (live dump):
 *   - URL bar:  resource-id com.android.chrome:id/url_bar
 *               shows the URL without the scheme (e.g. 'verona.club/pp.html'
 *               for https://verona.club/pp.html)
 *   - page content renders as TextViews (e.g. 'VERONA PRIVACY NOTICE')
 */
public class BrowserPage extends BasePage {

    public static final String CHROME_PACKAGE = "com.android.chrome";
    public static final By URL_BAR = AppiumBy.id("com.android.chrome:id/url_bar");

    public static By pageText(String text) {
        return AppiumBy.xpath("//android.widget.TextView[@text='" + text + "']");
    }

    private final WebDriverWait wait;

    public BrowserPage(AppiumDriver driver) {
        super(driver);
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        this.wait.pollingEvery(Duration.ofSeconds(2));
        this.wait.ignoring(StaleElementReferenceException.class);
    }

    /** Waits until Chrome is foreground and the URL bar shows the expected URL. */
    public BrowserPage waitUntilOpened(String expectedUrl) {
        wait.until(d -> CHROME_PACKAGE.equals(((AndroidDriver) d).getCurrentPackage()));
        wait.until(d -> {
            String url = getDisplayedUrl();
            return url != null && url.contains(expectedUrl);
        });
        return this;
    }

    /** URL as displayed by Chrome (scheme is hidden by the browser UI). */
    public String getDisplayedUrl() {
        try {
            return driver.findElement(URL_BAR).getText();
        } catch (Exception e) {
            return null;
        }
    }

    /** True when the given page text exists AND isDisplayed(). */
    public boolean isTextDisplayed(String text) {
        WebElement el = wait.until(
                ExpectedConditions.presenceOfElementLocated(pageText(text)));
        return el.isDisplayed();
    }
}
