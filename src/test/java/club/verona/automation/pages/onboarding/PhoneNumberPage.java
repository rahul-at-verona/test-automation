package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.regex.Pattern;

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

    private static final By HEADER_LOC = byText(HEADER);
    private static final By CONTINUE_LOC = AppiumBy.accessibilityId(CONTINUE);
    private static final By PHONE_INPUT = AppiumBy.xpath("//android.widget.EditText");
    private static final By PHONE_HINT_LOC = byText(PHONE_HINT);
    private static final By CONSENT_TEXT_LOC = byText(CONSENT_TEXT);
    private static final By NO_SPAM_TEXT_LOC = AppiumBy.xpath(
            "//*[starts-with(@text,'" + NO_SPAM_TEXT_PREFIX + "')]");
    private static final By COUNTRY_CHIP_LOC = AppiumBy.xpath(
            "//*[@clickable='true' and contains(@content-desc,'+')"
            + " and string-length(@content-desc) < 20]");
    private static final By PLUS_DESC_CANDIDATES_LOC = AppiumBy.xpath(
            "//*[@clickable='true' and contains(@content-desc,'+')]");
    private static final Pattern PLUS_CODE = Pattern.compile(".*\\+\\d{1,4}\\s*$");

    public PhoneNumberPage(AppiumDriver driver) {
        super(driver);
    }

    public PhoneNumberPage waitUntilLoaded() {
        newWait().until(ExpectedConditions.visibilityOfElementLocated(HEADER_LOC));
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(HEADER_LOC);
    }

    /**
     * Country selector label, e.g. "🇮🇳  +91".
     * Matched strictly — clickable node whose desc ENDS with "+<1-4 digits>" —
     * so unrelated text that merely contains a '+' can never be picked up.
     */
    public String getCountryCode() {
        return driver.findElements(PLUS_DESC_CANDIDATES_LOC).stream()
                .map(el -> el.getAttribute("content-desc"))
                .filter(desc -> desc != null && PLUS_CODE.matcher(desc).matches())
                .findFirst()
                .orElse(null);
    }

    public PhoneNumberPage enterPhoneNumber(String digits) {
        type(PHONE_INPUT, digits);
        pause(1500);
        return this;
    }

    public String getEnteredNumber() {
        try {
            return driver.findElement(PHONE_INPUT).getText();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /** The Continue button reports clickable=true only once input is valid. */
    public boolean isContinueEnabled() {
        try {
            return "true".equals(driver.findElement(CONTINUE_LOC).getAttribute("clickable"));
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /** True if the phone input still shows its hint (i.e. it is empty). */
    public boolean isPhoneHintVisible() {
        return isDisplayed(PHONE_HINT_LOC);
    }

    public boolean isConsentTextVisible() {
        return isDisplayed(CONSENT_TEXT_LOC);
    }

    public boolean isNoSpamTextVisible() {
        return isDisplayed(NO_SPAM_TEXT_LOC);
    }

    /** Taps Continue (caller must have entered a valid number + kept consent). */
    public void tapContinue() {
        click(CONTINUE_LOC);
    }

    /** Opens the country-code selector modal by tapping the +XX chip. */
    public CountrySelectorPage openCountrySelector() {
        click(COUNTRY_CHIP_LOC);
        return new CountrySelectorPage(driver).waitUntilLoaded();
    }

    /**
     * Toggles the WhatsApp/SMS consent checkbox: the unlabeled clickable
     * element immediately LEFT of the consent text (found by geometry — its
     * center y matches the text row and it sits before the text). Verified
     * live: it is not a sibling of the consent text in the tree, and no
     * label/id exists, so no direct locator can target it — position among
     * the clickable elements is the only reliable way to find it.
     */
    public PhoneNumberPage toggleConsentCheckbox() {
        Rectangle textRect = driver.findElement(CONSENT_TEXT_LOC).getRect();
        int textCenterY = textRect.getY() + textRect.getHeight() / 2;
        int textCenterX = textRect.getX() + textRect.getWidth() / 2;

        WebElement checkbox = driver.findElements(AppiumBy.xpath("//*[@clickable='true']")).stream()
                .filter(el -> isBlank(el.getAttribute("content-desc")) && isBlank(el.getText()))
                .filter(el -> {
                    Rectangle r = el.getRect();
                    int cy = r.getY() + r.getHeight() / 2;
                    int cx = r.getX() + r.getWidth() / 2;
                    return Math.abs(cy - textCenterY) < 50 && cx < textCenterX;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Consent checkbox not found"));
        checkbox.click();
        pause(1_500);
        return this;
    }

    /** Backs out (keyboard first, then the screen) to the landing page. */
    public LandingPage backToLanding() {
        for (int i = 0; i < 3; i++) {
            driver.navigate().back();
            pause(1500);
            if (!isDisplayed(HEADER_LOC) && isDisplayed(AppiumBy.accessibilityId(LandingPage.CONTINUE_WITH_PHONE))) {
                break;
            }
        }
        return new LandingPage(driver);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }
}
