package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * "Verona profiles are self-made" screen — reached from
 * {@link CreateMemberProfilePage}'s Continue. Terminal screen of the
 * "on behalf of a member" onboarding path: the placeholder profile has been
 * created and the account is now logged in, so this doubles as that
 * account's landing screen (it carries 'Log out').
 *
 * Live dump structure (My sibling path):
 *   - TextView 'Verona profiles are self-made' (header)
 *   - TextView "For now, members have to sign up to Verona themselves. It's
 *     the first sign someone's serious." (body)
 *   - TextView 'Invite them to Verona!'
 *   - quoted pre-filled invite message TextView
 *   - ViewGroup content-desc 'Share' (clickable) — opens the native Android
 *     share sheet; NOT tapped by tests (out of app scope, flaky in CI)
 *   - TextView "You've signed up on behalf of your <relation>. Made a mistake?
 *     Concierge can help." — the '<relation>' word varies by the member type
 *     chosen on {@link WhosThisProfileForPage}, verified live for all 3 paths
 *     that reach this screen: 'My sibling' -> "sibling", 'My child' ->
 *     "child", 'Someone else' -> "relative" (not the literal option label).
 *   - ViewGroup content-desc 'Text Verona Concierge' (clickable) — opens the
 *     device's messaging app; NOT tapped by tests
 *   - ViewGroup content-desc 'Log out' (clickable)
 */
public class InviteMemberPage extends BasePage {

    public static final String HEADER = "Verona profiles are self-made";
    public static final String BODY_TEXT =
            "For now, members have to sign up to Verona themselves. It's the first sign someone's serious.";
    public static final String INVITE_CTA = "Invite them to Verona!";
    public static final String SHARE = "Share";
    public static final String TEXT_CONCIERGE = "Text Verona Concierge";
    public static final String LOG_OUT = "Log out";

    /** Relation words used in {@link #isSignedUpOnBehalfTextVisible}, keyed by the picker option. */
    public static final String RELATION_SIBLING = "sibling";
    public static final String RELATION_CHILD = "child";
    public static final String RELATION_SOMEONE_ELSE = "relative";

    private static final By HEADER_LOC = byText(HEADER);
    private static final By BODY_TEXT_LOC = byText(BODY_TEXT);
    private static final By INVITE_CTA_LOC = byText(INVITE_CTA);
    private static final By SHARE_LOC = AppiumBy.accessibilityId(SHARE);
    private static final By TEXT_CONCIERGE_LOC = AppiumBy.accessibilityId(TEXT_CONCIERGE);
    private static final By LOG_OUT_LOC = AppiumBy.accessibilityId(LOG_OUT);

    public InviteMemberPage(AppiumDriver driver) {
        super(driver);
    }

    public InviteMemberPage waitUntilLoaded() {
        newWait().until(ExpectedConditions.visibilityOfElementLocated(HEADER_LOC));
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(HEADER_LOC);
    }

    public boolean isBodyTextVisible() {
        return isDisplayed(BODY_TEXT_LOC);
    }

    public boolean isInviteCtaVisible() {
        return isDisplayed(INVITE_CTA_LOC);
    }

    public boolean isShareVisible() {
        return isDisplayed(SHARE_LOC);
    }

    public boolean isTextConciergeVisible() {
        return isDisplayed(TEXT_CONCIERGE_LOC);
    }

    public boolean isLogOutVisible() {
        return isDisplayed(LOG_OUT_LOC);
    }

    /** True once the "signed up on behalf of" confirmation names the given relation, e.g. "sibling". */
    public boolean isSignedUpOnBehalfTextVisible(String relation) {
        String expected = "You've signed up on behalf of your " + relation + ". Made a mistake? Concierge can help.";
        return isDisplayed(byText(expected));
    }
}
