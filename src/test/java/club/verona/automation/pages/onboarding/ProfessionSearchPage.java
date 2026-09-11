package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Organization/Designation/Profession search-and-select modal — reached from
 * {@link ProfessionPage} tapping any of its Organization, Designation,
 * Profession, Previous organization or Previous designation selectors.
 * Same mechanics as {@link EducationSearchPage} (a DIFFERENT component, on a
 * different screen — verified live: the 'Other' row copy differs) but with
 * one real inconsistency: the Profession modal's own heading is just
 * 'Search' — not 'Search Profession' like Organization/Designation get
 * field-specific headings (verified live).
 *
 * Live dump structure:
 *   - TextView heading: 'Search Organization' (Organization AND Previous
 *     organization), 'Search Designation' (Designation AND Previous
 *     designation), or plain 'Search' (Profession only — no 'Search
 *     Profession' heading exists)
 *   - ViewGroup content-desc 'Close' (clickable)
 *   - EditText (hint varies — not asserted on, unreliable across variants)
 *   - rows: ViewGroup content-desc '<full option name>   ' (clickable,
 *     trailing padding spaces — verified live), e.g. 'Alphabet Google   ' or
 *     'Manager Operations   '
 *   - a pinned 'Other Not Listed Above   ' row (clickable, also
 *     trailing-padded) — NOT filtered out by an unrelated search query
 *     (verified live: searching a nonsense string still surfaced it), unlike
 *     {@link EducationSearchPage}'s 'Other' which needed the query itself to
 *     match. Selecting it sets the parent selector's label to 'Other (Not
 *     listed above)' (different text from the row's own label) and reveals
 *     a free-text field on the parent screen.
 */
public class ProfessionSearchPage extends BasePage {

    public static final String ORGANIZATION_HEADING = "Search Organization";
    public static final String DESIGNATION_HEADING = "Search Designation";
    public static final String PROFESSION_HEADING = "Search";
    public static final String CLOSE = "Close";
    /** Row label prefix in the modal (has trailing padding spaces — match with starts-with, not exact). */
    public static final String OTHER_OPTION_ROW_PREFIX = "Other Not Listed Above";
    /** What the parent screen's selector shows once 'Other' is chosen (different text from the row). */
    public static final String OTHER_SELECTED_LABEL = "Other (Not listed above)";

    private static final By CLOSE_LOC = AppiumBy.accessibilityId(CLOSE);
    private static final By SEARCH_BOX = AppiumBy.xpath("//android.widget.EditText");
    private static final By OTHER_OPTION_ROW_LOC = AppiumBy.xpath(
            "//*[starts-with(@content-desc,'" + OTHER_OPTION_ROW_PREFIX + "')]");
    private static final By CLICKABLE_ROWS_LOC = AppiumBy.xpath("//*[@clickable='true']");

    public ProfessionSearchPage(AppiumDriver driver) {
        super(driver);
    }

    public ProfessionSearchPage waitUntilLoaded(String heading) {
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

    public ProfessionSearchPage search(String query) {
        type(SEARCH_BOX, query);
        pause(1_500);
        return this;
    }

    /** Option names of the rows currently listed, including 'Other Not Listed Above   ' if present. */
    public List<String> getListedOptionNames() {
        return driver.findElements(CLICKABLE_ROWS_LOC).stream()
                .map(el -> el.getAttribute("content-desc"))
                .filter(desc -> desc != null && !desc.isBlank())
                .collect(Collectors.toList());
    }

    public boolean isOtherOptionVisible() {
        return isDisplayed(OTHER_OPTION_ROW_LOC);
    }

    /**
     * Selects a listed option by name — matched with starts-with since every
     * row carries trailing padding spaces in its content-desc (verified
     * live, e.g. 'Alphabet Google   '), so an exact accessibility-id match
     * would never hit.
     */
    public void selectOption(String optionName) {
        click(AppiumBy.xpath("//*[starts-with(@content-desc,'" + optionName + "')]"));
        pause(1_500);
    }

    /** Selects 'Other Not Listed Above' regardless of its trailing padding. */
    public void selectOther() {
        click(OTHER_OPTION_ROW_LOC);
        pause(1_500);
    }

    public void close() {
        click(CLOSE_LOC);
        pause(1_000);
    }
}
