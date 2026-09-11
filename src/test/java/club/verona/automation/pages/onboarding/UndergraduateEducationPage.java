package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * "Undergraduate education" screen — first step of the member application's
 * education section, reached from {@link YouIdentifyAsPage#tapContinue()}.
 *
 * Live dump structure:
 *   - TextView 'Undergraduate education' (header)
 *   - TextView '*Full-time, on-campus program only' (subtitle)
 *   - ViewGroup content-desc 'Type full name of institute' (clickable):
 *     opens {@link EducationSearchPage} (heading 'Search University').
 *     Picking a real result makes the selector's own content-desc become
 *     that option's exact name. Picking 'Other (Not listed above)' instead
 *     reveals an extra EditText (hint 'Full institute name & location') on
 *     THIS screen for free-text entry (verified live).
 *   - ViewGroup content-desc 'Type degree or major' (clickable): same
 *     pattern via {@link EducationSearchPage} (heading 'Search Degree');
 *     'Other (Not listed above)' reveals an EditText hint 'Please specify
 *     field of study'.
 *   - 'Continue' (content-desc): 'enabled' attribute false until BOTH the
 *     institute and degree fields are resolved — either a real selection,
 *     or 'Other' with its free-text field filled in (verified live: 'Other'
 *     alone, with the text field still empty, leaves Continue disabled).
 *     Advances to {@link PostgraduateEducationPage}.
 */
public class UndergraduateEducationPage extends BasePage {

    public static final String HEADER = "Undergraduate education";
    public static final String SUBTITLE = "*Full-time, on-campus program only";
    public static final String INSTITUTE_SELECTOR = "Type full name of institute";
    public static final String DEGREE_SELECTOR = "Type degree or major";
    public static final String INSTITUTE_OTHER_HINT = "Full institute name & location";
    public static final String DEGREE_OTHER_HINT = "Please specify field of study";
    public static final String CONTINUE = "Continue";
    public static final String UG_INSTITUTE_EXAMPLE_TEXT = "Eg: “Indian School of Business Mohali” (not just ISB); “Hindu College Delhi University” (not just DU)";
    public static final String UG_DEGREE_EXAMPLE_TEXT = "Eg: “Bachelor of Arts in Chinese” (not just BA). Pick the closest match for variants like BS = BSc, BA LLB Hons = BA LLB";

    private static final By HEADER_LOC = byText(HEADER);
    private static final By SUBTITLE_LOC = byText(SUBTITLE);
    private static final By INSTITUTE_SELECTOR_LOC = AppiumBy.accessibilityId(INSTITUTE_SELECTOR);
    private static final By DEGREE_SELECTOR_LOC = AppiumBy.accessibilityId(DEGREE_SELECTOR);
    private static final By INSTITUTE_OTHER_INPUT_LOC = byText(INSTITUTE_OTHER_HINT);
    private static final By DEGREE_OTHER_INPUT_LOC = byText(DEGREE_OTHER_HINT);
    private static final By CONTINUE_LOC = AppiumBy.accessibilityId(CONTINUE);
    public static final By UG_INSTITUTE_EXAMPLE_TEXT_LOC = byText(UG_INSTITUTE_EXAMPLE_TEXT);
    public static final By UG_DEGREE_EXAMPLE_TEXT_LOC = byText(UG_DEGREE_EXAMPLE_TEXT);


    public UndergraduateEducationPage(AppiumDriver driver) {
        super(driver);
    }

    public UndergraduateEducationPage waitUntilLoaded() {
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

    public boolean isUGExampleTextsVisible() {
        return (isDisplayed(UG_INSTITUTE_EXAMPLE_TEXT_LOC) && isDisplayed(UG_DEGREE_EXAMPLE_TEXT_LOC));
    }

    public boolean isDegreeSelectorVisible() {
        return isDisplayed(DEGREE_SELECTOR_LOC);
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
        return new EducationSearchPage(driver).waitUntilLoaded(EducationSearchPage.DEGREE_HEADING);
    }

    public boolean isInstituteOtherFieldVisible() {
        return isDisplayed(INSTITUTE_OTHER_INPUT_LOC);
    }

    public boolean isDegreeOtherFieldVisible() {
        return isDisplayed(DEGREE_OTHER_INPUT_LOC);
    }

    public UndergraduateEducationPage enterInstituteOtherText(String value) {
        type(INSTITUTE_OTHER_INPUT_LOC, value);
        pause(500);
        return this;
    }

    public UndergraduateEducationPage enterDegreeOtherText(String value) {
        type(DEGREE_OTHER_INPUT_LOC, value);
        pause(500);
        return this;
    }

    /** Continue reports its real enabled state via the 'enabled' attribute (verified live). */
    public boolean isContinueEnabled() {
        return isEnabledAttr(CONTINUE_LOC);
    }

    public PostgraduateEducationPage tapContinue() {
        click(CONTINUE_LOC);
        pause(2_000);
        return new PostgraduateEducationPage(driver).waitUntilLoaded();
    }
}
