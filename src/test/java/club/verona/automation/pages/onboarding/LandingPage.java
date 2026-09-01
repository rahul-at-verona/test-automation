package club.verona.automation.pages.onboarding;

import club.verona.automation.pages.BasePage;

import club.verona.automation.pages.editors.UiSnapshot;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Logged-out landing (onboarding) screen.
 *
 * Live dump structure:
 *   - 3 full-screen ImageViews  -> auto-rotating background images
 *   - 1 hero TextView           -> auto-rotates through HERO_TEXTS (~4s each)
 *   - links 'Terms of Use' / 'Privacy Policy' (content-desc, clickable),
 *     both open Chrome at verona.club
 *   - CTA 'Continue with phone number' (content-desc, clickable)
 *
 * The hero/background animate continuously, so ALL reads/taps here are
 * page-source/coordinate based (UiSnapshot) — proven not to hang.
 */
public class LandingPage extends BasePage {

    /** The three hero texts observed live (rotation order may vary). */
    public static final List<String> HERO_TEXTS = List.of(
            "A trusted community of\nhigh-achieving singles",
            "Find your life partner\nand a love that endures",
            "Handpicked profiles\ntailored to your needs");

    public static final String TERMS_OF_USE = "Terms of Use";
    public static final String PRIVACY_POLICY = "Privacy Policy";
    public static final String CONTINUE_WITH_PHONE = "Continue with phone number";
    public static final String CHROME_PACKAGE = "com.android.chrome";

    // Expected link destinations. Chrome's URL bar hides the https:// scheme,
    // so tests compare against the scheme-less form.
    public static final String TERMS_URL = "verona.club/tou.html";      // https://verona.club/tou.html
    public static final String PRIVACY_URL = "verona.club/pp.html";     // https://verona.club/pp.html
    public static final String TERMS_HEADING = "VERONA TERMS OF USE";
    public static final String PRIVACY_HEADING = "VERONA PRIVACY NOTICE";

    private static final By HERO_TEXT_LOC = AppiumBy.xpath(
            "//android.widget.HorizontalScrollView/android.view.ViewGroup/android.widget.TextView");

    public LandingPage(AppiumDriver driver) {
        super(driver);
    }

    /** Waits for the landing screen after a fresh (logged-out) app launch. */
    public LandingPage waitUntilLoaded() {
        UiSnapshot.waitFor(driver,
                s -> s.firstByDesc(CONTINUE_WITH_PHONE) != null,
                60_000, "landing page (Continue with phone number)");
        return this;
    }

    public boolean isLoaded() {
        return UiSnapshot.capture(driver).isDescDisplayed(CONTINUE_WITH_PHONE);
    }

    /** The hero text currently displayed, or null mid-transition. */
    public String getCurrentHeroText() {
        try {
            // getText() carries a trailing space after the second line (real
            // app markup, not a locator artifact) — trim so it matches HERO_TEXTS.
            return driver.findElement(HERO_TEXT_LOC).getText().trim();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /** Observes the rotating hero for the given time; returns distinct texts seen. */
    public Set<String> observeHeroTexts(long observeMs) {
        Set<String> seen = new LinkedHashSet<>();
        long deadline = System.currentTimeMillis() + observeMs;
        while (System.currentTimeMillis() < deadline
                && seen.size() < HERO_TEXTS.size()) {
            String current = getCurrentHeroText();
            if (current != null) {
                seen.add(current);
            }
            UiSnapshot.sleep(1000);
        }
        return seen;
    }

    /** Number of full-screen background ImageViews (one per carousel slide). */
    public long countBackgroundImages() {
        return UiSnapshot.capture(driver).all().stream()
                .filter(n -> n.cls.contains("ImageView"))
                .count();
    }

    public boolean isTermsOfUseVisible() {
        return UiSnapshot.capture(driver).isDescDisplayed(TERMS_OF_USE);
    }

    public boolean isPrivacyPolicyVisible() {
        return UiSnapshot.capture(driver).isDescDisplayed(PRIVACY_POLICY);
    }

    public void tapTermsOfUse()    { tapByDesc(TERMS_OF_USE); }
    public void tapPrivacyPolicy() { tapByDesc(PRIVACY_POLICY); }

    public PhoneNumberPage tapContinueWithPhone() {
        tapByDesc(CONTINUE_WITH_PHONE);
        return new PhoneNumberPage(driver).waitUntilLoaded();
    }

}
