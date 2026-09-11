package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Institute/degree search-and-select modal — reached from
 * {@link UndergraduateEducationPage} or {@link PostgraduateEducationPage}
 * tapping their institute or degree selector. Identical structure across all
 * 4 call sites (only the heading text differs — verified live), so one page
 * object serves all of them.
 *
 * Live dump structure (verified for University, UG Degree and PG Degree):
 *   - TextView heading: 'Search University' (both UG and PG institute),
 *     'Search Degree' (UG degree) or 'Field of Study' (PG degree — NOT
 *     'Search Degree', verified live; the postgraduate degree modal uses
 *     different copy from the undergraduate one)
 *   - ViewGroup content-desc 'Close' (clickable)
 *   - EditText hint 'Search' (live filter as you type)
 *   - rows: ViewGroup content-desc '<full option name>' (clickable), e.g.
 *     'AIIMS Rishikesh' or 'Bachelor of Architecture (BArch)'
 *   - a pinned 'Other (Not listed above)' row (clickable) — always present;
 *     visible without scrolling once a search narrows the list enough (e.g.
 *     searching "other" itself surfaces it directly, verified live)
 */
public class EducationSearchPage extends BasePage {

    public static final String UNIVERSITY_HEADING = "Search University";
    public static final String DEGREE_HEADING = "Search Degree";
    public static final String PG_DEGREE_HEADING = "Field of Study";
    public static final String CLOSE = "Close";
    public static final String SEARCH_HINT = "Search";
    public static final String OTHER_OPTION = "Other (Not listed above)";

    private static final By CLOSE_LOC = AppiumBy.accessibilityId(CLOSE);
    private static final By SEARCH_BOX = AppiumBy.xpath("//android.widget.EditText");
    private static final By OTHER_OPTION_LOC = AppiumBy.accessibilityId(OTHER_OPTION);
    private static final By CLICKABLE_ROWS_LOC = AppiumBy.xpath("//*[@clickable='true']");

    public EducationSearchPage(AppiumDriver driver) {
        super(driver);
    }

    public EducationSearchPage waitUntilLoaded(String heading) {
        newWait().until(ExpectedConditions.visibilityOfElementLocated(byText(heading)));
        return this;
    }

    public boolean isLoaded(String heading) {
        return isDisplayed(byText(heading));
    }

    public boolean isCloseButtonVisible() {
        return isDisplayed(CLOSE_LOC);
    }

    public boolean isSearchBoxVisible() {
        return isDisplayed(SEARCH_BOX);
    }

    public EducationSearchPage search(String query) {
        type(SEARCH_BOX, query);
        pause(1_500);
        return this;
    }

    /** Option names of the rows currently listed, including 'Other (Not listed above)' if present. */
    public List<String> getListedOptionNames() {
        return driver.findElements(CLICKABLE_ROWS_LOC).stream()
                .map(el -> el.getAttribute("content-desc"))
                .filter(desc -> desc != null && !desc.isBlank())
                .collect(Collectors.toList());
    }

    public boolean isOtherOptionVisible() {
        return isDisplayed(OTHER_OPTION_LOC);
    }

    /** Selects a listed option (or 'Other (Not listed above)'); the modal closes back to the calling screen. */
    public void selectOption(String optionName) {
        click(AppiumBy.accessibilityId(optionName));
        pause(1_500);
    }

    /**
     * Selects 'Other (Not listed above)'. It is always present but, in the
     * default unfiltered list, sits below the visible/scrolled area
     * (verified live) — searching "other" reliably narrows the list down to
     * just that row (its own label matches the query) before tapping it.
     */
    public void selectOther() {
        search("other");
        selectOption(OTHER_OPTION);
    }

    /** Closes the modal via the Close button, leaving the selector unset. */
    public void close() {
        click(CLOSE_LOC);
        pause(1_000);
    }
}
