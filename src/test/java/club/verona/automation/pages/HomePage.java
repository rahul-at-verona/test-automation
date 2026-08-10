package club.verona.automation.pages;

import club.verona.automation.pages.editors.UiSnapshot;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

/**
 * The Verona Home tab (the recommendations feed shown after login).
 * Its bottom navigation bar (resource-id 'bottom-navigation-bar-content')
 * is the reliable "logged in" anchor.
 */
public class HomePage extends BasePage {

    public static final By BOTTOM_NAV = AppiumBy.xpath(
            "//*[@resource-id='bottom-navigation-bar-content']");

    public HomePage(AppiumDriver driver) {
        super(driver);
    }

    /** Waits until the Home feed with its bottom nav is present. */
    public HomePage waitUntilLoaded() {
        UiSnapshot.waitFor(driver, s -> s.first(
                        n -> "bottom-navigation-bar-content".equals(
                                n.element.getAttribute("resource-id"))) != null,
                60_000, "Home tab (bottom navigation)");
        return this;
    }

    public boolean isLoaded() {
        return !driver.findElements(BOTTOM_NAV).isEmpty();
    }
}
