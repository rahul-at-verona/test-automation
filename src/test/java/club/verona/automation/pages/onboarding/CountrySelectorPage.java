package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Country-code selector modal (opened from the phone-number screen chip).
 *
 * Live dump structure:
 *   - TextView 'Search Country' (heading)
 *   - ViewGroup content-desc 'Close' (clickable) — closes WITHOUT changing
 *     the current selection
 *   - EditText hint 'Country name' (live partial/full search)
 *   - rows: ViewGroup content-desc '<code>   <Name>   <flag>   '
 *     e.g. '91   India   🇮🇳   ' — current selection is pinned first
 */
public class CountrySelectorPage extends BasePage {

    public static final String HEADING = "Search Country";
    public static final String CLOSE = "Close";
    public static final String SEARCH_HINT = "Country name";

    private static final By HEADING_LOC = byText(HEADING);
    private static final By CLOSE_LOC = AppiumBy.accessibilityId(CLOSE);
    public static final By SEARCH_BOX = AppiumBy.xpath("//android.widget.EditText");
    private static final By CLICKABLE_ROWS_LOC = AppiumBy.xpath("//*[@clickable='true']");

    /** Row content-desc pattern: dial code, name, flag separated by 3 spaces. */
    private static final Pattern ROW = Pattern.compile("^[\\d ]+ {3}.+ {3}.+");

    public CountrySelectorPage(AppiumDriver driver) {
        super(driver);
    }

    public CountrySelectorPage waitUntilLoaded() {
        newWait().until(ExpectedConditions.visibilityOfElementLocated(HEADING_LOC));
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(HEADING_LOC);
    }

    public boolean isCloseButtonVisible() {
        return isDisplayed(CLOSE_LOC);
    }

    public boolean isSearchBoxVisible() {
        return isDisplayed(SEARCH_BOX);
    }

    public CountrySelectorPage search(String query) {
        type(SEARCH_BOX, query);
        pause(2_000);
        return this;
    }

    /** Country names of the rows currently listed. */
    public List<String> getListedCountryNames() {
        return driver.findElements(CLICKABLE_ROWS_LOC).stream()
                .map(el -> el.getAttribute("content-desc"))
                .filter(desc -> desc != null && ROW.matcher(desc).matches())
                .map(desc -> desc.split(" {3}")[1].trim())
                .collect(Collectors.toList());
    }

    /** Selects a listed country; returns once the phone screen is back. */
    public PhoneNumberPage selectCountry(String countryName) {
        click(AppiumBy.xpath(
                "//*[@clickable='true' and contains(@content-desc,'   "
                + countryName + "   ')]"));
        return new PhoneNumberPage(driver).waitUntilLoaded();
    }

    /** Closes the modal via the Close button (selection must stay unchanged). */
    public PhoneNumberPage close() {
        click(CLOSE_LOC);
        newWait().until(ExpectedConditions.invisibilityOfElementLocated(HEADING_LOC));
        return new PhoneNumberPage(driver).waitUntilLoaded();
    }
}
