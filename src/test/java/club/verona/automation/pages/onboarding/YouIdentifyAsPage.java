package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * "You identify as" screen — the first step of the member application,
 * reached from {@link JoinTheClubPage#tapStartApplication()}.
 *
 * Live dump structure (Myself path):
 *   - TextView 'You identify as' (header)
 *   - ViewGroup content-desc 'Gender' (clickable): an INLINE dropdown, not a
 *     modal — tapping it expands 2 option rows directly below it in the same
 *     screen (content-desc 'Female' / 'Male'). Tapping an option selects it,
 *     collapses the dropdown, and the selector's own content-desc becomes the
 *     chosen option — same interaction pattern as {@link WhosThisProfileForPage}'s
 *     "Select member" dropdown.
 *   - TextView "At the moment, we cater to cisgender individuals. As our
 *     community grows, we'd love to include more diverse identities."
 *   - 'Continue' (content-desc): 'enabled' attribute false until a gender is
 *     chosen. Advances to {@link UndergraduateEducationPage}.
 */
public class YouIdentifyAsPage extends BasePage {

    public static final String HEADER = "You identify as";
    public static final String GENDER_SELECTOR = "Gender";
    public static final String FEMALE = "Female";
    public static final String MALE = "Male";
    public static final String CONTINUE = "Continue";
    public static final String CISGENDER_NOTE =
            "At the moment, we cater to cisgender individuals. As our community grows, we’d love to include more diverse identities.";

    private static final By HEADER_LOC = byText(HEADER);
    private static final By GENDER_SELECTOR_LOC = AppiumBy.accessibilityId(GENDER_SELECTOR);
    private static final By CISGENDER_NOTE_LOC = byText(CISGENDER_NOTE);
    private static final By CONTINUE_LOC = AppiumBy.accessibilityId(CONTINUE);
    private static final By SELECTOR_LABEL_LOC = AppiumBy.xpath(
            "//*[@content-desc='" + GENDER_SELECTOR + "' or @content-desc='" + FEMALE + "'"
            + " or @content-desc='" + MALE + "']");

    public YouIdentifyAsPage(AppiumDriver driver) {
        super(driver);
    }

    public YouIdentifyAsPage waitUntilLoaded() {
        newWait().until(ExpectedConditions.visibilityOfElementLocated(HEADER_LOC));
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(HEADER_LOC);
    }

    public boolean isSelectorVisible() {
        return isDisplayed(GENDER_SELECTOR_LOC);
    }

    public boolean isCisgenderNoteVisible() {
        return isDisplayed(CISGENDER_NOTE_LOC);
    }

    /** Opens the inline dropdown, revealing the 'Female' / 'Male' options below it. */
    public YouIdentifyAsPage openSelector() {
        click(GENDER_SELECTOR_LOC);
        pause(1000);
        return this;
    }

    public boolean isOptionVisible(String option) {
        return isDisplayed(AppiumBy.accessibilityId(option));
    }

    /** Selects a gender; the dropdown auto-collapses and shows the chosen label. */
    public YouIdentifyAsPage selectGender(String option) {
        click(AppiumBy.accessibilityId(option));
        pause(1000);
        return this;
    }

    /** The selector's current label: 'Gender' or the chosen option. */
    public String getSelectorLabel() {
        List<WebElement> matches = driver.findElements(SELECTOR_LABEL_LOC);
        return matches.isEmpty() ? null : matches.get(0).getAttribute("content-desc");
    }

    /** Continue reports its real enabled state via the 'enabled' attribute (verified live). */
    public boolean isContinueEnabled() {
        return isEnabledAttr(CONTINUE_LOC);
    }

    public UndergraduateEducationPage tapContinue() {
        click(CONTINUE_LOC);
        pause(2_000);
        return new UndergraduateEducationPage(driver).waitUntilLoaded();
    }
}
