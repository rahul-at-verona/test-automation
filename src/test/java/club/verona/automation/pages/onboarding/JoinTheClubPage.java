package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * "Join the Club" screen — reached from {@link WhosThisProfileForPage}'s
 * Continue for the 'Myself' option (via {@link WhosThisProfileForPage#tapContinueAsMyself()}),
 * after the OS notification-permission dialog.
 *
 * Live dump structure (Myself path):
 *   - TextView 'Join the Club' (header)
 *   - TextView 'Our members are successful and intentional.'
 *   - TextView 'Let’s begin the journey of getting\nto know you.'
 *   - TextView 'Next few steps are important. Be accurate as you’ll have to verify it later'
 *   - ViewGroup content-desc 'Start application' (clickable) — begins the
 *     member application; advances to {@link YouIdentifyAsPage} (verified live).
 */
public class JoinTheClubPage extends BasePage {

    public static final String HEADER = "Join the Club";
    public static final String SUBTITLE = "Our members are successful and intentional.";
    public static final String JOURNEY_TEXT = "Let’s begin the journey of getting\nto know you.";
    public static final String ACCURACY_TEXT =
            "Next few steps are important. Be accurate as you’ll have to verify it later";
    public static final String START_APPLICATION = "Start application";

    private static final By HEADER_LOC = byText(HEADER);
    private static final By SUBTITLE_LOC = byText(SUBTITLE);
    private static final By JOURNEY_TEXT_LOC = byText(JOURNEY_TEXT);
    private static final By ACCURACY_TEXT_LOC = byText(ACCURACY_TEXT);
    private static final By START_APPLICATION_LOC = AppiumBy.accessibilityId(START_APPLICATION);

    public JoinTheClubPage(AppiumDriver driver) {
        super(driver);
    }

    public JoinTheClubPage waitUntilLoaded() {
        newWait().until(ExpectedConditions.visibilityOfElementLocated(HEADER_LOC));
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(HEADER_LOC);
    }

    public boolean areTextsVisible() {
        return isDisplayed(SUBTITLE_LOC) && isDisplayed(JOURNEY_TEXT_LOC) && isDisplayed(ACCURACY_TEXT_LOC);
    }

    public boolean isStartApplicationVisible() {
        return isDisplayed(START_APPLICATION_LOC);
    }

    /** Start application reports its real enabled state via the 'enabled' attribute (verified live). */
    public boolean isStartApplicationEnabled() {
        return isEnabledAttr(START_APPLICATION_LOC);
    }

    /** Begins the member application; lands on {@link YouIdentifyAsPage}. */
    public YouIdentifyAsPage tapStartApplication() {
        click(START_APPLICATION_LOC);
        pause(2_000);
        return new YouIdentifyAsPage(driver).waitUntilLoaded();
    }
}
