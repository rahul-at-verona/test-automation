package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

/**
 * "Who's this profile for?" screen — first post-signup step, reached from
 * {@link IntroductionPage}'s Continue.
 *
 * Live dump structure:
 *   - TextView 'Who’s this profile for?' (header)
 *   - helper texts 'Marriage is a big decision for two people.' and
 *     'At this time, we can only service those who sign up for themselves.'
 *   - ViewGroup content-desc 'Select member' (clickable): an INLINE dropdown,
 *     not a modal — tapping it expands 4 option rows directly below it in the
 *     same screen (content-desc 'Myself' / 'My child' / 'My sibling' /
 *     'Someone else'). Tapping an option selects it, collapses the dropdown,
 *     and the selector's own content-desc/text becomes the chosen option.
 *   - 'Continue' (content-desc): reports real state via the 'enabled'
 *     attribute (verified live) — false until a member type is chosen.
 *     Advances to {@link CreateMemberProfilePage} for 'My sibling', 'My
 *     child' and 'Someone else' (all 3 verified live, identical screen).
 *     'Myself' (verified live) instead triggers an OS notification-
 *     permission dialog ('Allow Verona to send you notifications?'), then
 *     lands on {@link JoinTheClubPage} — see {@link #tapContinueAsMyself()}.
 */
public class WhosThisProfileForPage extends BasePage {

    public static final String HEADER = "Who’s this profile for?";
    public static final String SELECT_MEMBER = "Select member";
    public static final String MYSELF = "Myself";
    public static final String MY_CHILD = "My child";
    public static final String MY_SIBLING = "My sibling";
    public static final String SOMEONE_ELSE = "Someone else";
    public static final String CONTINUE = "Continue";
    public static final String MARRIAGE_HELPER_TEXT = "Marriage is a big decision for two people.";
    public static final String SELF_SIGNUP_TEXT =
            "At this time, we can only service those who sign up for themselves.";

    private static final By HEADER_LOC = byText(HEADER);
    private static final By SELECT_MEMBER_LOC = AppiumBy.accessibilityId(SELECT_MEMBER);
    private static final By MARRIAGE_HELPER_LOC = byText(MARRIAGE_HELPER_TEXT);
    private static final By SELF_SIGNUP_LOC = byText(SELF_SIGNUP_TEXT);
    private static final By CONTINUE_LOC = AppiumBy.accessibilityId(CONTINUE);
    private static final By ALLOW_LOC = byText("Allow");
    private static final By SELECTOR_LABEL_LOC = AppiumBy.xpath(
            "//*[@content-desc='" + SELECT_MEMBER + "' or @content-desc='" + MYSELF + "'"
            + " or @content-desc='" + MY_CHILD + "' or @content-desc='" + MY_SIBLING + "'"
            + " or @content-desc='" + SOMEONE_ELSE + "']");

    public WhosThisProfileForPage(AppiumDriver driver) {
        super(driver);
    }

    public WhosThisProfileForPage waitUntilLoaded() {
        newWait().until(ExpectedConditions.visibilityOfElementLocated(HEADER_LOC));
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(HEADER_LOC);
    }

    public boolean isSelectorVisible() {
        return isDisplayed(SELECT_MEMBER_LOC);
    }

    public boolean areHelperTextsVisible() {
        return isDisplayed(MARRIAGE_HELPER_LOC) && isDisplayed(SELF_SIGNUP_LOC);
    }

    /** Opens the inline dropdown, revealing the 4 member-type options below it. */
    public WhosThisProfileForPage openSelector() {
        click(SELECT_MEMBER_LOC);
        pause(1000);
        return this;
    }

    public boolean isOptionVisible(String option) {
        return isDisplayed(AppiumBy.accessibilityId(option));
    }

    /** Selects a member type; the dropdown auto-collapses and shows the chosen label. */
    public WhosThisProfileForPage selectMemberType(String option) {
        click(AppiumBy.accessibilityId(option));
        pause(1000);
        return this;
    }

    /** The selector's current label: 'Select member' or the chosen option. */
    public String getSelectorLabel() {
        List<org.openqa.selenium.WebElement> matches = driver.findElements(SELECTOR_LABEL_LOC);
        return matches.isEmpty() ? null : matches.get(0).getAttribute("content-desc");
    }

    /** Continue reports its real enabled state via the 'enabled' attribute (verified live). */
    public boolean isContinueEnabled() {
        return isEnabledAttr(CONTINUE_LOC);
    }

    public CreateMemberProfilePage tapContinue() {
        click(CONTINUE_LOC);
        pause(1500);
        return new CreateMemberProfilePage(driver).waitUntilLoaded();
    }

    /**
     * Continue for the 'Myself' option: also dismisses the OS notification-
     * permission dialog that appears first (tapping 'Allow'), then lands on
     * {@link JoinTheClubPage}.
     */
    public JoinTheClubPage tapContinueAsMyself() {
        click(CONTINUE_LOC);
        pause(1500);
        newWait().until(ExpectedConditions.elementToBeClickable(ALLOW_LOC)).click();
        pause(1500);
        return new JoinTheClubPage(driver).waitUntilLoaded();
    }
}
