package club.verona.automation.pages.editors;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Set;

/**
 * "My location" editor: single-select city list with a live partial search.
 * Opened by tapping the "My location" row on the profile Edit tab.
 *
 * All reads and clicks are page-source/coordinate based (see UiSnapshot) —
 * on-device element queries hang this app's instrumentation. The only
 * element interaction kept is sendKeys into the search box.
 */
public class LocationEditor extends BasePage {

    public static final By SEARCH_BOX = AppiumBy.xpath("//android.widget.EditText");
    private static final String SAVE_LABEL = "Save changes";

    public LocationEditor(AppiumDriver driver) {
        super(driver);
        UiSnapshot.waitFor(driver,
                s -> s.first(n -> n.cls.contains("EditText")) != null,
                15_000, "location editor (search box)");
    }

    public LocationEditor search(String partial) {
        type(SEARCH_BOX, partial);
        settle();
        return this;
    }

    /** City names currently suggested. */
    public List<String> getSuggestions() {
        return SourceList.suggestions(driver, Set.of(SAVE_LABEL));
    }

    public LocationEditor selectCity(String city) {
        clickByDesc(city);
        UiSnapshot.sleep(800);
        return this;
    }

    /** Saves and waits for the editor to close. */
    public void save() {
        clickByDesc(SAVE_LABEL);
        UiSnapshot.waitFor(driver,
                s -> s.first(n -> n.cls.contains("EditText")) == null,
                15_000, "location editor to close");
    }

    /** Wait until two consecutive suggestion reads are identical (debounce). */
    private void settle() {
        List<String> prev = null;
        for (int i = 0; i < 8; i++) {
            UiSnapshot.sleep(1000);
            List<String> now = getSuggestions();
            if (now.equals(prev)) {
                return;
            }
            prev = now;
        }
    }
}
