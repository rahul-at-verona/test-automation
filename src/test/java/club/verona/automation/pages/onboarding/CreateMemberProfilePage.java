package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.regex.Pattern;

/**
 * "Create profile" screen — collects a name and phone number for the member
 * type chosen on {@link WhosThisProfileForPage}. Verified live: identical
 * screen (same fields, same copy) for 'My sibling', 'My child' and 'Someone
 * else' — only the relation word on the following {@link InviteMemberPage}
 * differs per option.
 *
 * Live dump structure:
 *   - TextView 'Create profile' (header)
 *   - TextView 'Who’s seeking a life partner?' (sub-header)
 *   - 2 EditTexts in order: hint 'First name', hint 'Last name'
 *   - country selector ViewGroup content-desc '🇮🇳  +91' (clickable, defaults
 *     to India — same component as {@link PhoneNumberPage})
 *   - 1 EditText: hint 'Phone number' (3rd EditText overall)
 *   - 'Continue' (content-desc): 'enabled' attribute false until first name,
 *     last name and phone number are all filled. Advances to
 *     {@link InviteMemberPage} — no OTP step for the member being profiled.
 */
public class CreateMemberProfilePage extends BasePage {

    public static final String HEADER = "Create profile";
    public static final String SUB_HEADER = "Who’s seeking a life partner?";
    public static final String FIRST_NAME_HINT = "First name";
    public static final String LAST_NAME_HINT = "Last name";
    public static final String PHONE_HINT = "Phone number";
    public static final String CONTINUE = "Continue";

    private static final By HEADER_LOC = byText(HEADER);
    private static final By SUB_HEADER_LOC = byText(SUB_HEADER);
    private static final By CONTINUE_LOC = AppiumBy.accessibilityId(CONTINUE);
    private static final By EDIT_TEXTS = AppiumBy.className("android.widget.EditText");
    private static final By PLUS_DESC_CANDIDATES_LOC = AppiumBy.xpath(
            "//*[@clickable='true' and contains(@content-desc,'+')]");
    private static final Pattern PLUS_CODE = Pattern.compile(".*\\+\\d{1,4}\\s*$");
    private static final int FIRST_NAME_INDEX = 0;
    private static final int LAST_NAME_INDEX = 1;
    private static final int PHONE_INDEX = 2;

    public CreateMemberProfilePage(AppiumDriver driver) {
        super(driver);
    }

    public CreateMemberProfilePage waitUntilLoaded() {
        newWait().until(ExpectedConditions.visibilityOfElementLocated(HEADER_LOC));
        return this;
    }

    public boolean isLoaded() {
        return isDisplayed(HEADER_LOC);
    }

    public boolean isSubHeaderVisible() {
        return isDisplayed(SUB_HEADER_LOC);
    }

    /**
     * Country selector label, e.g. "🇮🇳  +91". Matched strictly — clickable
     * node whose desc ENDS with "+<1-4 digits>" (same rule as {@link PhoneNumberPage}).
     */
    public String getCountryCode() {
        return driver.findElements(PLUS_DESC_CANDIDATES_LOC).stream()
                .map(el -> el.getAttribute("content-desc"))
                .filter(desc -> desc != null && PLUS_CODE.matcher(desc).matches())
                .findFirst()
                .orElse(null);
    }

    public CreateMemberProfilePage enterFirstName(String value) { fillField(FIRST_NAME_INDEX, value); return this; }
    public CreateMemberProfilePage enterLastName(String value)  { fillField(LAST_NAME_INDEX, value); return this; }
    public CreateMemberProfilePage enterPhoneNumber(String value) { fillField(PHONE_INDEX, value); return this; }

    /** Continue reports its real enabled state via the 'enabled' attribute (verified live). */
    public boolean isContinueEnabled() {
        return isEnabledAttr(CONTINUE_LOC);
    }

    public InviteMemberPage tapContinue() {
        click(CONTINUE_LOC);
        pause(2_000);
        return new InviteMemberPage(driver).waitUntilLoaded();
    }

    /** Fills first name, last name and phone, then submits. */
    public InviteMemberPage completeWith(String firstName, String lastName, String phone) {
        enterFirstName(firstName);
        enterLastName(lastName);
        enterPhoneNumber(phone);
        return tapContinue();
    }

    private void fillField(int index, String value) {
        List<WebElement> fields = driver.findElements(EDIT_TEXTS);
        WebElement field = fields.get(index);
        field.clear();
        field.sendKeys(value);
        pause(500);
    }
}
