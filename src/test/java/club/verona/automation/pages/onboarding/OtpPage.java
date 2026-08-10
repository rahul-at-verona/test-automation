package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * "Verify your phone number" OTP bottom sheet — fully element/locator based
 * (no page-source snapshots). The sheet's native views query reliably, unlike
 * the animated Home feed.
 *
 * Verified live:
 *   - header TextView 'Verify your phone number'
 *   - TextView 'OTP sent to' + '+91-<number>' + tappable 'Edit'
 *   - SIX single-digit EditText boxes (each holds one char) scoped under the
 *     sheet ViewGroup — the underlying phone field is excluded by that scope
 *   - Resend control: TextView 'Resend in' + 'NN s' countdown; becomes
 *     'Resend OTP' (clickable) after it expires
 *   - correct OTP 123456 (universal test code) verifies and advances
 *   - any other code -> TextView 'Invalid or expired OTP.' and stays put
 *   - Edit -> returns to the phone-number screen with the number preserved
 */
public class OtpPage extends BasePage {

    public static final String HEADER = "Verify your phone number";
    public static final String OTP_SENT_LABEL = "OTP sent to";
    public static final String INVALID_OTP_ERROR = "Invalid or expired OTP.";

    public static final By HEADER_LOC = AppiumBy.xpath(
            "//android.widget.TextView[@text='" + HEADER + "']");
    public static final By OTP_SENT_LOC = AppiumBy.xpath(
            "//android.widget.TextView[@text='" + OTP_SENT_LABEL + "']");
    public static final By PHONE_TEXT_LOC = AppiumBy.xpath(
            "//android.widget.TextView[starts-with(@text,'+')]");
    public static final By EDIT_LOC = AppiumBy.xpath(
            "//android.widget.TextView[@text='Edit']");
    public static final By RESEND_LABEL_LOC = AppiumBy.xpath(
            "//android.widget.TextView[@text='Resend in' or @text='Resend OTP']");
    public static final By RESEND_ACTIVE_LOC = AppiumBy.accessibilityId("Resend OTP");
    public static final By INVALID_ERROR_LOC = AppiumBy.xpath(
            "//android.widget.TextView[@text='" + INVALID_OTP_ERROR + "']");

    // The 6 boxes are scoped under the sheet so the phone field behind is excluded.
    public static final By OTP_BOXES = AppiumBy.xpath(
            "//android.view.ViewGroup[starts-with(@content-desc,'" + HEADER + "')]"
            + "//android.widget.EditText");

    public OtpPage(AppiumDriver driver) {
        super(driver);
    }

    public OtpPage waitUntilLoaded() {
        newWait().until(ExpectedConditions.visibilityOfElementLocated(HEADER_LOC));
        return this;
    }

    // ---------------- UI queries ----------------

    public boolean isLoaded() {
        return isDisplayed(HEADER_LOC);
    }

    public boolean isHeaderDisplayed()      { return isDisplayed(HEADER_LOC); }
    public boolean isOtpSentLabelDisplayed(){ return isDisplayed(OTP_SENT_LOC); }
    public boolean isEditDisplayed()        { return isDisplayed(EDIT_LOC); }
    public boolean isResendDisplayed()      { return isDisplayed(RESEND_LABEL_LOC); }

    /** The masked number shown, e.g. "+91-8799731416". */
    public String getPhoneNumberText() {
        return driver.findElement(PHONE_TEXT_LOC).getText();
    }

    /** Number of OTP input boxes (should be 6). */
    public int getBoxCount() {
        return driver.findElements(OTP_BOXES).size();
    }

    /** True while the invalid/expired OTP error is shown. */
    public boolean isInvalidOtpErrorDisplayed() {
        return isDisplayed(INVALID_ERROR_LOC);
    }

    /** True once the Resend control is active (countdown finished). */
    public boolean isResendActive() {
        return !driver.findElements(RESEND_ACTIVE_LOC).isEmpty();
    }

    // ---------------- actions ----------------

    /** Types one digit into each box (one char per box). */
    public OtpPage enterOtp(String code) {
        List<WebElement> boxes = newWait().until(
                ExpectedConditions.numberOfElementsToBeMoreThan(OTP_BOXES, 0));
        for (int i = 0; i < code.length() && i < boxes.size(); i++) {
            WebElement box = boxes.get(i);
            box.click();
            box.sendKeys(String.valueOf(code.charAt(i)));
        }
        return this;
    }

    /** Reads the current per-box contents, e.g. "123456". */
    public String getEnteredCode() {
        StringBuilder sb = new StringBuilder();
        for (WebElement box : driver.findElements(OTP_BOXES)) {
            sb.append(box.getText() == null ? "" : box.getText());
        }
        return sb.toString();
    }

    /** Submits a code expected to be rejected; waits for the error to appear. */
    public OtpPage submitInvalidOtp(String code) {
        enterOtp(code);
        newWait().until(ExpectedConditions.visibilityOfElementLocated(INVALID_ERROR_LOC));
        return this;
    }

    /** Submits the valid test OTP and waits for the sheet to disappear. */
    public void submitValidOtp(String code) {
        enterOtp(code);
        newWait().until(ExpectedConditions.invisibilityOfElementLocated(HEADER_LOC));
    }

    /** Taps Edit; returns to the phone-number screen. */
    public PhoneNumberPage tapEdit() {
        click(EDIT_LOC);
        newWait().until(ExpectedConditions.invisibilityOfElementLocated(HEADER_LOC));
        return new PhoneNumberPage(driver).waitUntilLoaded();
    }

    /** Taps Resend OTP (only valid once active). */
    public OtpPage tapResend() {
        click(RESEND_ACTIVE_LOC);
        return this;
    }

    /** True if the app is no longer on the OTP screen. */
    public boolean isDismissed() {
        return driver.findElements(HEADER_LOC).isEmpty();
    }

    private boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }
}
