package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;

import club.verona.automation.pages.editors.UiSnapshot;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

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
    public static final By SEARCH_BOX = AppiumBy.xpath("//android.widget.EditText");

    /** Row content-desc pattern: dial code, name, flag separated by 3 spaces. */
    private static final Pattern ROW = Pattern.compile("^[\\d ]+ {3}.+ {3}.+");

    public CountrySelectorPage(AppiumDriver driver) {
        super(driver);
    }

    public CountrySelectorPage waitUntilLoaded() {
        UiSnapshot.waitFor(driver, s -> s.containsText(HEADING),
                15_000, "country selector modal");
        return this;
    }

    public boolean isLoaded() {
        return UiSnapshot.capture(driver).isTextDisplayed(HEADING);
    }

    public boolean isCloseButtonVisible() {
        return UiSnapshot.capture(driver).isDescDisplayed(CLOSE);
    }

    public boolean isSearchBoxVisible() {
        UiSnapshot snap = UiSnapshot.capture(driver);
        return snap.first(n -> n.cls.contains("EditText")) != null;
    }

    public CountrySelectorPage search(String query) {
        type(SEARCH_BOX, query);
        pause(2_000);
        return this;
    }

    /** Country names of the rows currently listed. */
    public List<String> getListedCountryNames() {
        return UiSnapshot.capture(driver).all().stream()
                .filter(n -> n.clickable && n.desc != null && ROW.matcher(n.desc).matches())
                .map(n -> n.desc.split(" {3}")[1].trim())
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
        clickByDesc(CLOSE);
        UiSnapshot.waitFor(driver, s -> !s.containsText(HEADING),
                10_000, "country selector to close");
        return new PhoneNumberPage(driver).waitUntilLoaded();
    }
}
