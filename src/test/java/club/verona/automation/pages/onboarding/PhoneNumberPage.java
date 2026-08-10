package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;

import club.verona.automation.pages.editors.UiSnapshot;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * "What's your phone number?" screen (opened from the landing CTA).
 *
 * Live dump structure:
 *   - TextView 'What's your phone number?'
 *   - country selector ViewGroup content-desc '🇮🇳  +91' (clickable)
 *   - EditText hint 'Phone number'
 *   - consent text 'Connect with me on WhatsApp, SMS, or email.' + checkbox
 *   - 'Continue' button: clickable=false until a valid number is entered
 */
public class PhoneNumberPage extends BasePage {

    public static final String HEADER = "What’s your phone number?";
    public static final String CONTINUE = "Continue";
    public static final String PHONE_HINT = "Phone number";
    public static final String CONSENT_TEXT = "Connect with me on WhatsApp, SMS, or email.";
    public static final String NO_SPAM_TEXT_PREFIX = "No spam ever—pinky promise.";
    public static final By PHONE_INPUT = AppiumBy.xpath("//android.widget.EditText");

    public PhoneNumberPage(AppiumDriver driver) {
        super(driver);
    }

    public PhoneNumberPage waitUntilLoaded() {
        UiSnapshot.waitFor(driver, s -> s.containsText(HEADER),
                15_000, "phone number screen");
        return this;
    }

    public boolean isLoaded() {
        return UiSnapshot.capture(driver).isTextDisplayed(HEADER);
    }

    /**
     * Country selector label, e.g. "🇮🇳  +91".
     * Matched strictly — clickable node whose desc ENDS with "+<1-4 digits>" —
     * so unrelated text that merely contains a '+' can never be picked up.
     */
    public String getCountryCode() {
        UiSnapshot.Snap s = UiSnapshot.capture(driver)
                .first(n -> n.clickable && n.desc != null
                        && n.desc.matches(".*\\+\\d{1,4}\\s*$"));
        return s != null ? s.desc : null;
    }

    public PhoneNumberPage enterPhoneNumber(String digits) {
        type(PHONE_INPUT, digits);
        pause(1500);
        return this;
    }

    public String getEnteredNumber() {
        UiSnapshot.Snap s = UiSnapshot.capture(driver)
                .first(n -> n.cls.contains("EditText"));
        return s != null ? s.text : null;
    }

    /** The Continue button reports clickable=true only once input is valid. */
    public boolean isContinueEnabled() {
        UiSnapshot.Snap s = UiSnapshot.capture(driver).firstByDesc(CONTINUE);
        return s != null && s.clickable;
    }

    /** True if the phone input still shows its hint (i.e. it is empty). */
    public boolean isPhoneHintVisible() {
        return UiSnapshot.capture(driver).isTextDisplayed(PHONE_HINT);
    }

    public boolean isConsentTextVisible() {
        return UiSnapshot.capture(driver).isTextDisplayed(CONSENT_TEXT);
    }

    public boolean isNoSpamTextVisible() {
        return UiSnapshot.capture(driver)
                .first(n -> n.text != null && n.text.startsWith(NO_SPAM_TEXT_PREFIX)
                        && n.displayed) != null;
    }

    /** Taps Continue (caller must have entered a valid number + kept consent). */
    public void tapContinue() {
        clickByDesc(CONTINUE);
    }

    /** Opens the country-code selector modal by tapping the +XX chip. */
    public CountrySelectorPage openCountrySelector() {
        click(AppiumBy.xpath(
                "//*[@clickable='true' and contains(@content-desc,'+')"
                + " and string-length(@content-desc) < 20]"));
        return new CountrySelectorPage(driver).waitUntilLoaded();
    }

    /**
     * Toggles the WhatsApp/SMS consent checkbox: the unlabeled clickable
     * ViewGroup immediately LEFT of the consent text (found by geometry —
     * its center y matches the text row and it sits before the text).
     */
    public PhoneNumberPage toggleConsentCheckbox() {
        // REVERTED to geometry + coordinate tap: the checkbox is an unlabeled
        // ViewGroup that is NOT a sibling of the consent text in the tree, so
        // no stable element locator exists (verified: the preceding-sibling
        // XPath finds nothing). Discovery by position is the reliable way.
        UiSnapshot snap = UiSnapshot.capture(driver);
        UiSnapshot.Snap textNode = snap.first(n -> CONSENT_TEXT.equals(n.text));
        if (textNode == null) {
            throw new IllegalStateException("Consent text not found");
        }
        UiSnapshot.Snap checkbox = snap.first(n -> n.clickable
                && (n.desc == null || n.desc.isEmpty())
                && (n.text == null || n.text.isEmpty())
                && Math.abs(n.cy - textNode.cy) < 50
                && n.cx < textNode.cx);
        if (checkbox == null) {
            throw new IllegalStateException("Consent checkbox not found");
        }
        tapByCoordinates(checkbox);
        pause(1_500);
        return this;
    }

    /** Backs out (keyboard first, then the screen) to the landing page. */
    public LandingPage backToLanding() {
        for (int i = 0; i < 3; i++) {
            driver.navigate().back();
            UiSnapshot.sleep(1500);
            UiSnapshot snap = UiSnapshot.capture(driver);
            if (!snap.containsText(HEADER)
                    && snap.firstByDesc(LandingPage.CONTINUE_WITH_PHONE) != null) {
                break;
            }
        }
        return new LandingPage(driver);
    }
}
