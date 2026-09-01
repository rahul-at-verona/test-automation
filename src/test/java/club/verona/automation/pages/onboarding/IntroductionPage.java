package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;
import club.verona.automation.pages.editors.UiSnapshot;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Post-OTP "quick introduction" screen — name and email are now collected
 * together on one screen (previously two separate screens).
 *
 * Live dump structure (new-signup phone number, first time through OTP):
 *   - TextView 'Let do a quick introduction!' (header; app copy as shipped)
 *   - 'Your name' section label + helper text ('...Avoid nicknames and
 *     initials at this stage')
 *   - 3 EditTexts in order: hint 'First', hint 'Middle (optional)', hint 'Last'
 *   - 'Your email' section label
 *   - 1 EditText: hint 'youremail@example.com'
 *   - 'Continue' (content-desc): enabled=false until fields are filled;
 *     verified enabled=true after first name + last name + email are filled.
 *     Advances to the next onboarding step ("Who's this profile for?").
 */
public class IntroductionPage extends BasePage {

    public static final String HEADER = "Let do a quick introduction!";
    public static final String NAME_SECTION_LABEL = "Your name";
    public static final String NAME_HELPER_TEXT =
            "We’re a secured private club. Avoid nicknames and initials at this stage";
    public static final String FIRST_NAME_HINT = "First";
    public static final String MIDDLE_NAME_HINT = "Middle (optional)";
    public static final String LAST_NAME_HINT = "Last";
    public static final String EMAIL_SECTION_LABEL = "Your email";
    public static final String EMAIL_HINT = "youremail@example.com";
    public static final String CONTINUE = "Continue";

    private static final By EDIT_TEXTS = AppiumBy.className("android.widget.EditText");
    private static final int FIRST_NAME_INDEX = 0;
    private static final int MIDDLE_NAME_INDEX = 1;
    private static final int LAST_NAME_INDEX = 2;
    private static final int EMAIL_INDEX = 3;

    public IntroductionPage(AppiumDriver driver) {
        super(driver);
    }

    public IntroductionPage waitUntilLoaded() {
        UiSnapshot.waitFor(driver, s -> s.containsText(HEADER), 15_000, "introduction (name+email) screen");
        return this;
    }

    public boolean isLoaded() {
        return UiSnapshot.capture(driver).isTextDisplayed(HEADER);
    }

    public boolean isNameSectionVisible()   { return UiSnapshot.capture(driver).isTextDisplayed(NAME_SECTION_LABEL); }
    public boolean isNameHelperTextVisible(){ return UiSnapshot.capture(driver).isTextDisplayed(NAME_HELPER_TEXT); }
    public boolean isEmailSectionVisible()  { return UiSnapshot.capture(driver).isTextDisplayed(EMAIL_SECTION_LABEL); }

    public IntroductionPage enterFirstName(String value)  { fillField(FIRST_NAME_INDEX, value); return this; }
    public IntroductionPage enterMiddleName(String value) { fillField(MIDDLE_NAME_INDEX, value); return this; }
    public IntroductionPage enterLastName(String value)   { fillField(LAST_NAME_INDEX, value); return this; }
    public IntroductionPage enterEmail(String value)      { fillField(EMAIL_INDEX, value); return this; }

    /** Continue reports its real enabled state via the 'enabled' attribute (verified live). */
    public boolean isContinueEnabled() {
        UiSnapshot.Snap btn = UiSnapshot.capture(driver).first(n -> CONTINUE.equals(n.desc));
        return btn != null && "true".equals(btn.element.getAttribute("enabled"));
    }

    public void tapContinue() {
        UiSnapshot.Snap btn = UiSnapshot.capture(driver).first(n -> CONTINUE.equals(n.desc));
        if (btn == null) {
            throw new IllegalStateException("'Continue' not found on the introduction screen");
        }
        UiSnapshot.tap(driver, btn);
        pause(2_000);
    }

    /** Fills first/last name + email and submits. Middle name is optional and left blank. */
    public void completeWith(String firstName, String lastName, String email) {
        enterFirstName(firstName);
        enterLastName(lastName);
        enterEmail(email);
        tapContinue();
    }

    private void fillField(int index, String value) {
        List<WebElement> fields = driver.findElements(EDIT_TEXTS);
        WebElement field = fields.get(index);
        field.clear();
        field.sendKeys(value);
        pause(500);
    }
}
