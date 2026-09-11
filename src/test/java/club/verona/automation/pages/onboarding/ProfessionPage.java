package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * "Your current profession" screen — reached from
 * {@link PostgraduateEducationPage}'s Continue. Unlike the education
 * screens' selectors, all 5 profession options are directly visible/clickable
 * cards (no inline dropdown to open first).
 *
 * Live dump structure and per-option behaviour (ALL verified live — the 5
 * options are NOT symmetric, each was walked individually):
 *   - TextView 'Your current profession' (header)
 *   - 5 ViewGroup option cards (content-desc = exact label, clickable):
 *     'Employed', 'Business owner', 'Self-employed / Independent
 *     professional', 'Currently not working', 'Student'
 *   - Selecting one reveals a DIFFERENT field set below (previous option's
 *     fields disappear):
 *       Employed: 'Organization' selector + a visible "Example:
 *         “McKinsey & Company” or “Yash Raj Films”" helper text + 'Designation'
 *         selector + LinkedIn EditText. Continue needs ONLY Organization +
 *         Designation resolved — LinkedIn is optional (and has NO format
 *         validation at all: garbage text, a bare domain, or a full https://
 *         URL all leave Continue exactly as enabled/disabled as before,
 *         verified live across all 3).
 *       Business owner: 'Organization' selector + LinkedIn EditText + the
 *         "You can also share your company website..." note (NO example
 *         text, NO Designation field — genuinely different layout from
 *         Employed's Organization, not just a subset). Continue needs ONLY
 *         Organization resolved — LinkedIn optional.
 *       Self-employed / Independent professional: 'Profession' selector +
 *         LinkedIn EditText + the same note text. Continue needs BOTH
 *         Profession resolved AND LinkedIn non-empty (verified live:
 *         resolving Profession alone leaves Continue disabled) — unlike
 *         Business owner's single-selector case, where LinkedIn stayed
 *         optional. Not a pattern to assume from the other options.
 *       Currently not working: 'Previous organization' + 'Previous
 *         designation' selectors + LinkedIn EditText + the note text.
 *         Continue needs ALL THREE — both selectors AND LinkedIn non-empty
 *         (verified live: resolving both selectors alone still leaves
 *         Continue disabled; only typing into LinkedIn afterward enables it).
 *       Student: LinkedIn EditText + the note text only — no organization-
 *         style selector at all. Continue needs LinkedIn non-empty (verified
 *         live: disabled with it empty, enables once typed into).
 *   - Each selector (Organization/Designation/Profession/Previous
 *     organization/Previous designation) opens {@link ProfessionSearchPage}.
 *     Selecting 'Other Not Listed Above' there sets the selector's own label
 *     to 'Other (Not listed above)' and reveals a free-text EditText on THIS
 *     screen — verified live for Organization (hint 'Type organization
 *     name'); not re-verified per other selector, but the same 'Other'
 *     mechanism applies (same component, {@link ProfessionSearchPage}).
 *   - 'Continue' (content-desc): 'enabled' attribute reflects the per-option
 *     rules above (verified live).
 */
public class ProfessionPage extends BasePage {

    public static final String HEADER = "Your current profession";

    public static final String EMPLOYED = "Employed";
    public static final String BUSINESS_OWNER = "Business owner";
    public static final String SELF_EMPLOYED = "Self-employed / Independent professional";
    public static final String CURRENTLY_NOT_WORKING = "Currently not working";
    public static final String STUDENT = "Student";

    public static final String ORGANIZATION_SELECTOR = "Organization";
    public static final String DESIGNATION_SELECTOR = "Designation";
    public static final String PROFESSION_SELECTOR = "Profession";
    public static final String PREVIOUS_ORGANIZATION_SELECTOR = "Previous organization";
    public static final String PREVIOUS_DESIGNATION_SELECTOR = "Previous designation";

    public static final String LINKEDIN_HINT = "Linkedin profile or website";
    public static final String ORGANIZATION_OTHER_HINT = "Type organization name";
    /** Verified live: the Designation 'Other' free-text field's hint is literally "Designation" — NOT "Type designation name". */
    public static final String DESIGNATION_OTHER_HINT = "Designation";
    public static final String ORGANIZATION_EXAMPLE_TEXT = "Example: “McKinsey & Company” or “Yash Raj Films”";
    public static final String SHARE_PLATFORM_NOTE =
            "You can also share your company website, public Instagram, news articles, or any other online "
            + "platform that highlights your awesomeness. This helps us evaluate your application faster.";
    public static final String CONTINUE = "Continue";

    private static final By HEADER_LOC = byText(HEADER);
    private static final By ORGANIZATION_SELECTOR_LOC = AppiumBy.accessibilityId(ORGANIZATION_SELECTOR);
    private static final By DESIGNATION_SELECTOR_LOC = AppiumBy.accessibilityId(DESIGNATION_SELECTOR);
    private static final By PROFESSION_SELECTOR_LOC = AppiumBy.accessibilityId(PROFESSION_SELECTOR);
    private static final By PREVIOUS_ORGANIZATION_SELECTOR_LOC =
            AppiumBy.accessibilityId(PREVIOUS_ORGANIZATION_SELECTOR);
    private static final By PREVIOUS_DESIGNATION_SELECTOR_LOC =
            AppiumBy.accessibilityId(PREVIOUS_DESIGNATION_SELECTOR);
    private static final By LINKEDIN_LOC = AppiumBy.xpath("//android.widget.EditText");
    private static final By ORGANIZATION_EXAMPLE_TEXT_LOC = byText(ORGANIZATION_EXAMPLE_TEXT);
    private static final By ORGANIZATION_OTHER_INPUT_LOC = byText(ORGANIZATION_OTHER_HINT);
    private static final By DESIGNATION_OTHER_INPUT_LOC =
            AppiumBy.xpath("//android.widget.EditText[@text='" + DESIGNATION_OTHER_HINT + "']");
    private static final By SHARE_PLATFORM_NOTE_LOC = byText(SHARE_PLATFORM_NOTE);
    private static final By CONTINUE_LOC = AppiumBy.accessibilityId(CONTINUE);

    public ProfessionPage(AppiumDriver driver) {
        super(driver);
    }

    public ProfessionPage waitUntilLoaded() {
        newWait().until(ExpectedConditions.visibilityOfElementLocated(HEADER_LOC));
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(HEADER_LOC);
    }

    public boolean isOptionVisible(String option) {
        return isDisplayed(AppiumBy.accessibilityId(option));
    }

    /** Selects a profession option; the relevant field set appears below immediately. */
    public ProfessionPage selectProfession(String option) {
        click(AppiumBy.accessibilityId(option));
        pause(1_500);
        return this;
    }

    public boolean isOrganizationSelectorVisible() {
        return isDisplayed(ORGANIZATION_SELECTOR_LOC);
    }

    public boolean isDesignationSelectorVisible() {
        return isDisplayed(DESIGNATION_SELECTOR_LOC);
    }

    public boolean isProfessionSelectorVisible() {
        return isDisplayed(PROFESSION_SELECTOR_LOC);
    }

    public boolean isPreviousOrganizationSelectorVisible() {
        return isDisplayed(PREVIOUS_ORGANIZATION_SELECTOR_LOC);
    }

    public boolean isPreviousDesignationSelectorVisible() {
        return isDisplayed(PREVIOUS_DESIGNATION_SELECTOR_LOC);
    }

    public boolean isLinkedInFieldVisible() {
        return isDisplayed(LINKEDIN_LOC);
    }

    public boolean isOrganizationExampleTextVisible() {
        return isDisplayed(ORGANIZATION_EXAMPLE_TEXT_LOC);
    }

    /** True once 'Other' has been chosen for Organization, revealing this free-text field (verified live). */
    public boolean isOrganizationOtherFieldVisible() {
        return isDisplayed(ORGANIZATION_OTHER_INPUT_LOC);
    }

    public ProfessionPage enterOrganizationOtherText(String value) {
        type(ORGANIZATION_OTHER_INPUT_LOC, value);
        pause(500);
        return this;
    }

    /** True once 'Other' has been chosen for Designation, revealing this free-text field (verified live). */
    public boolean isDesignationOtherFieldVisible() {
        return isDisplayed(DESIGNATION_OTHER_INPUT_LOC);
    }

    public ProfessionPage enterDesignationOtherText(String value) {
        type(DESIGNATION_OTHER_INPUT_LOC, value);
        pause(500);
        return this;
    }

    public boolean isSharePlatformNoteVisible() {
        return isDisplayed(SHARE_PLATFORM_NOTE_LOC);
    }

    /** True once the given selector shows this exact resolved label (real option name or 'Other (Not listed above)'). */
    public boolean isOptionSelected(String resolvedLabel) {
        return isDisplayed(AppiumBy.accessibilityId(resolvedLabel));
    }

    public ProfessionSearchPage openOrganizationSearch() {
        click(ORGANIZATION_SELECTOR_LOC);
        return new ProfessionSearchPage(driver).waitUntilLoaded(ProfessionSearchPage.ORGANIZATION_HEADING);
    }

    public ProfessionSearchPage openDesignationSearch() {
        click(DESIGNATION_SELECTOR_LOC);
        return new ProfessionSearchPage(driver).waitUntilLoaded(ProfessionSearchPage.DESIGNATION_HEADING);
    }

    public ProfessionSearchPage openProfessionSearch() {
        click(PROFESSION_SELECTOR_LOC);
        return new ProfessionSearchPage(driver).waitUntilLoaded(ProfessionSearchPage.PROFESSION_HEADING);
    }

    public ProfessionSearchPage openPreviousOrganizationSearch() {
        click(PREVIOUS_ORGANIZATION_SELECTOR_LOC);
        return new ProfessionSearchPage(driver).waitUntilLoaded(ProfessionSearchPage.ORGANIZATION_HEADING);
    }

    public ProfessionSearchPage openPreviousDesignationSearch() {
        click(PREVIOUS_DESIGNATION_SELECTOR_LOC);
        return new ProfessionSearchPage(driver).waitUntilLoaded(ProfessionSearchPage.DESIGNATION_HEADING);
    }

    /** Fills the LinkedIn field (accepts any text — verified live, no format validation is applied). */
    public ProfessionPage enterLinkedIn(String value) {
        type(LINKEDIN_LOC, value);
        pause(500);
        return this;
    }

    /** Continue reports its real enabled state via the 'enabled' attribute (verified live). */
    public boolean isContinueEnabled() {
        return isEnabledAttr(CONTINUE_LOC);
    }
}
