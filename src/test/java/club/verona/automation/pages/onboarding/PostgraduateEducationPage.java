package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * "Postgraduate college & university, if applicable" screen — reached from
 * {@link UndergraduateEducationPage#tapContinue()}. Same institute/degree
 * selector + {@link EducationSearchPage} + 'Other' free-text pattern as the
 * undergraduate screen, plus an opt-out for members with no PG degree.
 *
 * Live dump structure:
 *   - TextView 'Postgraduate college & university, if applicable' (header)
 *   - TextView 'Full-time on-campus program only' (subtitle — no leading '*',
 *     unlike the undergraduate screen)
 *   - ViewGroup content-desc 'Type full name of institute' (clickable) —
 *     opens {@link EducationSearchPage} with heading 'Search University',
 *     identical to {@link UndergraduateEducationPage}
 *   - ViewGroup content-desc 'Type degree or major' (clickable) — opens
 *     {@link EducationSearchPage} too, but its heading is 'Field of Study'
 *     here, NOT 'Search Degree' like the undergraduate screen (verified
 *     live — the postgraduate degree modal uses different copy)
 *   - TextView "I don't have a postgraduate degree" + an adjacent UNLABELED
 *     clickable checkbox (no content-desc/text) immediately to its LEFT —
 *     same pattern as {@link PhoneNumberPage}'s consent checkbox: tapping
 *     the text itself does nothing (verified live); the checkbox must be
 *     found by geometry. Checking it enables Continue directly, without
 *     resolving the institute/degree fields (verified live).
 *   - 'Continue' (content-desc): 'enabled' attribute false until EITHER both
 *     institute+degree are resolved (same rule as the UG screen) OR the
 *     'no postgraduate degree' checkbox is checked.
 */
public class PostgraduateEducationPage extends BasePage {

    public static final String HEADER = "Postgraduate college & university, if applicable";
    public static final String SUBTITLE = "Full-time on-campus program only";
    public static final String INSTITUTE_SELECTOR = "Type full name of institute";
    public static final String DEGREE_SELECTOR = "Type degree or major";
    public static final String INSTITUTE_OTHER_HINT = "Full institute name & location";
    public static final String DEGREE_OTHER_HINT = "Please specify field of study";
    public static final String NO_PG_DEGREE_TEXT = "I don’t have a postgraduate degree";
    public static final String CONTINUE = "Continue";

    private static final By HEADER_LOC = byText(HEADER);
    private static final By SUBTITLE_LOC = byText(SUBTITLE);
    private static final By INSTITUTE_SELECTOR_LOC = AppiumBy.accessibilityId(INSTITUTE_SELECTOR);
    private static final By DEGREE_SELECTOR_LOC = AppiumBy.accessibilityId(DEGREE_SELECTOR);
    private static final By INSTITUTE_OTHER_INPUT_LOC = byText(INSTITUTE_OTHER_HINT);
    private static final By DEGREE_OTHER_INPUT_LOC = byText(DEGREE_OTHER_HINT);
    private static final By NO_PG_DEGREE_TEXT_LOC = byText(NO_PG_DEGREE_TEXT);
    private static final By CONTINUE_LOC = AppiumBy.accessibilityId(CONTINUE);

    public PostgraduateEducationPage(AppiumDriver driver) {
        super(driver);
    }

    public PostgraduateEducationPage waitUntilLoaded() {
        newWait().until(ExpectedConditions.visibilityOfElementLocated(HEADER_LOC));
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(HEADER_LOC);
    }

    public boolean isSubtitleVisible() {
        return isDisplayed(SUBTITLE_LOC);
    }

    public boolean isInstituteSelectorVisible() {
        return isDisplayed(INSTITUTE_SELECTOR_LOC);
    }

    public boolean isDegreeSelectorVisible() {
        return isDisplayed(DEGREE_SELECTOR_LOC);
    }

    public boolean isNoDegreeOptOutVisible() {
        return isDisplayed(NO_PG_DEGREE_TEXT_LOC);
    }

    /** True once the given option's name is showing as the selector's chosen label (institute or degree). */
    public boolean isOptionSelected(String optionNameOrOther) {
        return isDisplayed(AppiumBy.accessibilityId(optionNameOrOther));
    }

    public EducationSearchPage openInstituteSearch() {
        click(INSTITUTE_SELECTOR_LOC);
        return new EducationSearchPage(driver).waitUntilLoaded(EducationSearchPage.UNIVERSITY_HEADING);
    }

    public EducationSearchPage openDegreeSearch() {
        click(DEGREE_SELECTOR_LOC);
        return new EducationSearchPage(driver).waitUntilLoaded(EducationSearchPage.PG_DEGREE_HEADING);
    }

    public boolean isInstituteOtherFieldVisible() {
        return isDisplayed(INSTITUTE_OTHER_INPUT_LOC);
    }

    public boolean isDegreeOtherFieldVisible() {
        return isDisplayed(DEGREE_OTHER_INPUT_LOC);
    }

    public PostgraduateEducationPage enterInstituteOtherText(String value) {
        type(INSTITUTE_OTHER_INPUT_LOC, value);
        pause(500);
        return this;
    }

    public PostgraduateEducationPage enterDegreeOtherText(String value) {
        type(DEGREE_OTHER_INPUT_LOC, value);
        pause(500);
        return this;
    }

    /**
     * Checks the 'no postgraduate degree' checkbox: the unlabeled clickable
     * element immediately LEFT of its text (found by geometry — verified
     * live that tapping the text itself is a no-op; see class doc).
     */
    public PostgraduateEducationPage checkNoPostgraduateDegree() {
        Rectangle textRect = driver.findElement(NO_PG_DEGREE_TEXT_LOC).getRect();
        int textCenterY = textRect.getY() + textRect.getHeight() / 2;
        int textCenterX = textRect.getX() + textRect.getWidth() / 2;

        WebElement checkbox = driver.findElements(AppiumBy.xpath("//*[@clickable='true']")).stream()
                .filter(el -> isBlank(el.getAttribute("content-desc")) && isBlank(el.getText()))
                .filter(el -> {
                    Rectangle r = el.getRect();
                    int cy = r.getY() + r.getHeight() / 2;
                    int cx = r.getX() + r.getWidth() / 2;
                    return Math.abs(cy - textCenterY) < 60 && cx < textCenterX;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("'No postgraduate degree' checkbox not found"));
        checkbox.click();
        pause(1_500);
        return this;
    }

    /** Continue reports its real enabled state via the 'enabled' attribute (verified live). */
    public boolean isContinueEnabled() {
        return isEnabledAttr(CONTINUE_LOC);
    }

    public ProfessionPage tapContinue() {
        click(CONTINUE_LOC);
        pause(2_000);
        return new ProfessionPage(driver).waitUntilLoaded();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }
}
