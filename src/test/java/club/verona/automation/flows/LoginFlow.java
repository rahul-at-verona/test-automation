package club.verona.automation.flows;

import club.verona.automation.pages.HomePage;
import club.verona.automation.pages.onboarding.LandingPage;
import club.verona.automation.pages.onboarding.OtpPage;
import club.verona.automation.pages.onboarding.PhoneNumberPage;
import club.verona.automation.pages.editors.UiSnapshot;
import io.appium.java_client.AppiumDriver;

/**
 * End-to-end login: landing -> phone -> OTP -> post-login interstitials -> Home.
 *
 * Uses the fixed test credentials (phone 8799731416, OTP 123456) that the
 * backend accepts without sending a real SMS. Verified live: after the OTP the
 * app shows a notifications-permission dialog and two interstitials ("You're
 * in!", "Coming up next…"), each dismissed by a 'Continue'/'Allow' button —
 * handled generically below so the flow is robust to their order/count.
 */
public final class LoginFlow {

    public static final String TEST_PHONE = "8799731416";
    public static final String TEST_OTP = "123456";

    private LoginFlow() {}

    /** Assumes the app is freshly launched (logged out) on the landing screen. */
    public static HomePage login(AppiumDriver driver) {
        return login(driver, TEST_PHONE, TEST_OTP);
    }

    public static HomePage login(AppiumDriver driver, String phone, String otp) {
        LandingPage landing = new LandingPage(driver).waitUntilLoaded();

        PhoneNumberPage phonePage = landing.tapContinueWithPhone();
        phonePage.enterPhoneNumber(phone);
        phonePage.tapContinue();

        new OtpPage(driver).waitUntilLoaded().enterOtp(otp);

        // Step through the post-login interstitials / permission dialogs until
        // the Home tab's bottom navigation appears.
        dismissInterstitialsUntilHome(driver);

        return new HomePage(driver).waitUntilLoaded();
    }

    /**
     * Repeatedly taps whatever advance control is on screen ('Continue' in the
     * app, 'Allow' in a system permission dialog) until the Home nav shows.
     */
    private static void dismissInterstitialsUntilHome(AppiumDriver driver) {
        long deadline = System.currentTimeMillis() + 90_000;
        while (System.currentTimeMillis() < deadline) {
            UiSnapshot snap = UiSnapshot.capture(driver);

            if (snap.first(n -> "bottom-navigation-bar-content".equals(
                    n.element.getAttribute("resource-id"))) != null) {
                return; // reached Home
            }

            UiSnapshot.Snap advance = snap.first(n -> n.clickable
                    && ("Continue".equals(n.desc) || "Continue".equals(n.text)));
            if (advance == null) {
                advance = snap.first(n -> "Allow".equals(n.text)); // permission dialog
            }

            if (advance != null) {
                UiSnapshot.tap(driver, advance);
                UiSnapshot.sleep(2_500);
            } else {
                UiSnapshot.sleep(1_500); // nothing actionable yet — wait & re-scan
            }
        }
        throw new IllegalStateException("Did not reach Home tab after login");
    }
}
