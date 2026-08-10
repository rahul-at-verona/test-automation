package club.verona.automation.pages.editors;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "Open to living in" editor: two-level multi-select.
 *   Level 1 (countries): header 'Select location', search 'Search Countries',
 *       rows content-desc '<Country>' / '<Country>, 1+ cities selected' /
 *       '<Country>, Open to all'; Save button content-desc 'Save'.
 *   Level 2 (cities): header '<Country>', search 'Search cities', rows
 *       content-desc '<City>' whose CHECKBOX (first clickable child) is the
 *       only tappable part; Save button content-desc 'Save'.
 *
 * All reads/taps are page-source/coordinate based (UiSnapshot) — on-device
 * queries hang this app. sendKeys into the search box is the only element
 * interaction kept.
 */
public class LivingPreferenceEditor extends BasePage {

    public static final By SEARCH_BOX = AppiumBy.xpath("//android.widget.EditText");
    private static final String COUNTRY_HEADER = "Select location";
    private static final String SAVE_LABEL = "Save";

    public LivingPreferenceEditor(AppiumDriver driver) {
        super(driver);
        UiSnapshot.waitFor(driver, s -> s.containsText(COUNTRY_HEADER),
                15_000, "'Select location' country list");
    }

    public LivingPreferenceEditor search(String partial) {
        type(SEARCH_BOX, partial);
        settle();
        return this;
    }

    /** Row names on the current level ("India, 1+ cities selected" -> "India"). */
    public List<String> getSuggestionNames() {
        return SourceList.suggestions(driver, Set.of("Save", "Save changes")).stream()
                .map(d -> d.split(",")[0].trim())
                .collect(Collectors.toList());
    }

    /**
     * Opens a country's city list. Filters the list first, taps the row by
     * fresh coordinates, then VERIFIES the right city screen opened (its
     * unique 'Open to all cities in X' row) — retries if a re-render shifted
     * the rows under the tap.
     */
    public LivingPreferenceEditor openCountry(String country) {
        String marker = "Open to all cities in " + country;
        for (int attempt = 1; attempt <= 3; attempt++) {
            search(country.toLowerCase(Locale.ROOT));
            try {
                click(AppiumBy.xpath("//*[@content-desc='" + country
                        + "' or starts-with(@content-desc,'" + country + ",')]"));
            } catch (org.openqa.selenium.TimeoutException e) {
                continue; // row not clickable yet — search again
            }
            UiSnapshot.sleep(2000);
            if (UiSnapshot.capture(driver).containsDescPrefix(marker)) {
                return this;
            }
            driver.navigate().back(); // wrong screen — retry
            UiSnapshot.sleep(1500);
        }
        throw new IllegalStateException("Could not open city list of " + country);
    }

    /**
     * Toggles a city's checkbox. REVERTED to coordinate tap: verified live
     * that element.click() on the checkbox does NOT flip it, while a
     * coordinate tap on the same node does (this is the documented
     * "fall back to coordinates" case).
     */
    public LivingPreferenceEditor toggleCity(String city) {
        UiSnapshot snap = waitForSnapshot(s -> s.firstByDesc(city) != null,
                10_000, "city row '" + city + "'");
        UiSnapshot.Snap row = snap.firstByDesc(city);
        UiSnapshot.Snap checkbox = snap.firstClickableDescendant(row);
        tapByCoordinates(checkbox != null ? checkbox : row);
        UiSnapshot.sleep(800);
        return this;
    }

    /**
     * True if the city's checkbox shows selected. The RN checkbox does NOT
     * expose the `checked` attribute; instead a checkmark glyph node
     * (com.horcrux.svg.G) is rendered inside the row only when selected —
     * verified live (checked row has this node, unchecked does not).
     */
    public boolean isCitySelected(String city) {
        UiSnapshot snap = UiSnapshot.capture(driver);
        UiSnapshot.Snap row = snap.firstByDesc(city);
        if (row == null) {
            return false;
        }
        org.w3c.dom.NodeList descendants =
                row.element.getElementsByTagName("com.horcrux.svg.G");
        return descendants.getLength() > 0;
    }

    /** Backs out of the city level and the country level without saving. */
    public void cancel() {
        driver.navigate().back();  // city -> country list
        UiSnapshot.waitFor(driver, s -> s.containsText(COUNTRY_HEADER),
                10_000, "return to country list");
        driver.navigate().back();  // country list -> profile
        UiSnapshot.waitFor(driver, s -> !s.containsText(COUNTRY_HEADER),
                10_000, "living editor to close");
    }

    /** Saves the city level (returns to countries), then the country level. */
    public void saveAll() {
        clickByDesc(SAVE_LABEL); // city level
        UiSnapshot.waitFor(driver, s -> s.containsText(COUNTRY_HEADER),
                15_000, "return to country list");
        clickByDesc(SAVE_LABEL); // country level
        UiSnapshot.waitFor(driver, s -> !s.containsText(COUNTRY_HEADER),
                15_000, "living editor to close");
    }

    private void settle() {
        List<String> prev = null;
        for (int i = 0; i < 8; i++) {
            UiSnapshot.sleep(1000);
            List<String> now = getSuggestionNames();
            if (now.equals(prev)) {
                return;
            }
            prev = now;
        }
    }
}
