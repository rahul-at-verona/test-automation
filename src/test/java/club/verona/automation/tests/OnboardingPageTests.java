package club.verona.automation.tests;

import club.verona.automation.annotations.LandingPageTests;
import club.verona.automation.annotations.LoggedInBaseTests;
import club.verona.automation.annotations.OtpScreenTests;
import club.verona.automation.annotations.PhoneNumberScreenTests;
import club.verona.automation.core.DriverFactory;
import club.verona.automation.flows.LoginFlow;
import club.verona.automation.pages.HomePage;
import club.verona.automation.pages.onboarding.BrowserPage;
import club.verona.automation.pages.onboarding.CountrySelectorPage;
import club.verona.automation.pages.onboarding.LandingPage;
import club.verona.automation.pages.onboarding.OtpPage;
import club.verona.automation.pages.onboarding.PhoneNumberPage;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * All logged-OUT onboarding screen tests in one class:
 *   - LandingPageTests        (hero rotation, legal links, CTA)
 *   - PhoneNumberScreenTests  (texts, country selector, validation, consent)
 *   - OtpScreenTests          (OTP UI + verification behaviour)
 *   - LoggedInBaseTests       (full login flow reaches Home)
 *
 * Each test carries a marker annotation for its origin AND a matching TestNG
 * group, so a subset can be run later, e.g.:
 *   mvn test -Dplatform=android -Dgroups=OtpScreenTests
 *
 * Every test starts from a fresh, logged-out landing screen (app storage
 * cleared + relaunched in {@link #resetToLanding()}), so tests are independent
 * and order-free.
 */
public class OnboardingPageTests extends BaseTest {

    private static final String TEST_PHONE = "8799731416";
    private static final String VALID_OTP = "123456";

    private LandingPage landingPage;

    /**
     * True until the first test method runs. BaseTest.@BeforeClass already
     * cleared storage and cold-started the app, so the first reset would be a
     * redundant second clear — skip it for the first method only.
     */
    private boolean firstMethod = true;

    @BeforeMethod(alwaysRun = true)
    public void resetToLanding() {
        if (firstMethod) {
            firstMethod = false; // @BeforeClass already gave us a fresh landing
        } else {
            DriverFactory.clearAppStorage();
            ((AndroidDriver) driver).activateApp("club.verona");
        }
        landingPage = new LandingPage(driver).waitUntilLoaded();
    }

    // ---- navigation helpers ----

    private PhoneNumberPage gotoPhoneScreen() {
        return landingPage.tapContinueWithPhone();
    }

    private OtpPage gotoOtpScreen() {
        PhoneNumberPage phone = gotoPhoneScreen();
        phone.enterPhoneNumber(TEST_PHONE);
        phone.tapContinue();
        return new OtpPage(driver).waitUntilLoaded();
    }

    // =====================================================================
    // Landing page
    // =====================================================================

    @LandingPageTests
    @Test(groups = {"OnboardingPageTests", "LandingPageTests"}, priority = 1,
            description = "Landing page shows CTA, legal links and a hero text")
    public void testLandingPageLoads() {
        Assert.assertTrue(landingPage.isLoaded(), "'Continue with phone number' should be visible");
        Assert.assertTrue(landingPage.isTermsOfUseVisible(), "'Terms of Use' link should be visible");
        Assert.assertTrue(landingPage.isPrivacyPolicyVisible(), "'Privacy Policy' link should be visible");
        Assert.assertNotNull(landingPage.getCurrentHeroText(),
                "Hero text should be one of the 3 known texts");
    }

    @LandingPageTests
    @Test(groups = {"OnboardingPageTests", "LandingPageTests"}, priority = 2,
            description = "Hero text auto-rotates through all 3 known texts")
    public void testHeroTextAutoRotatesThroughAllThree() {
        Set<String> seen = landingPage.observeHeroTexts(40_000);
        Assert.assertTrue(seen.contains(LandingPage.HERO_TEXTS.get(0)),
                "Should show 'A trusted community of high-achieving singles', saw: " + seen);
        Assert.assertEquals(seen.size(), LandingPage.HERO_TEXTS.size(),
                "All 3 hero texts should appear during rotation, saw: " + seen);
    }

    @LandingPageTests
    @Test(groups = {"OnboardingPageTests", "LandingPageTests"}, priority = 3,
            description = "3 background images exist and the visual changes over time")
    public void testBackgroundImagesRotate() {
        Assert.assertEquals(landingPage.countBackgroundImages(), 3,
                "Landing carousel should have 3 background ImageViews");
        byte[] shot1 = driver.getScreenshotAs(OutputType.BYTES);
        sleep(6_000);
        byte[] shot2 = driver.getScreenshotAs(OutputType.BYTES);
        Assert.assertFalse(Arrays.equals(shot1, shot2),
                "Screen should visually change as the carousel rotates");
    }

    @LandingPageTests
    @Test(groups = {"OnboardingPageTests", "LandingPageTests"}, priority = 4,
            description = "'Terms of Use' opens https://verona.club/tou.html showing 'VERONA TERMS OF USE'")
    public void testTermsOfUseRedirectsToCorrectPage() {
        landingPage.tapTermsOfUse();
        BrowserPage browser = new BrowserPage(driver).waitUntilOpened(LandingPage.TERMS_URL);
        Assert.assertTrue(browser.getDisplayedUrl().contains(LandingPage.TERMS_URL),
                "URL should be https://" + LandingPage.TERMS_URL + " but was: " + browser.getDisplayedUrl());
        Assert.assertTrue(browser.isTextDisplayed(LandingPage.TERMS_HEADING),
                "'" + LandingPage.TERMS_HEADING + "' should be displayed on the page");
    }

    @LandingPageTests
    @Test(groups = {"OnboardingPageTests", "LandingPageTests"}, priority = 5,
            description = "'Privacy Policy' opens https://verona.club/pp.html showing 'VERONA PRIVACY NOTICE'")
    public void testPrivacyPolicyRedirectsToCorrectPage() {
        landingPage.tapPrivacyPolicy();
        BrowserPage browser = new BrowserPage(driver).waitUntilOpened(LandingPage.PRIVACY_URL);
        Assert.assertTrue(browser.getDisplayedUrl().contains(LandingPage.PRIVACY_URL),
                "URL should be https://" + LandingPage.PRIVACY_URL + " but was: " + browser.getDisplayedUrl());
        Assert.assertTrue(browser.isTextDisplayed(LandingPage.PRIVACY_HEADING),
                "'" + LandingPage.PRIVACY_HEADING + "' should be displayed on the page");
    }

    @LandingPageTests
    @Test(groups = {"OnboardingPageTests", "LandingPageTests"}, priority = 6,
            description = "Continue-with-phone opens the phone screen with a valid number enabling Continue")
    public void testContinueWithPhoneFlow() {
        PhoneNumberPage phonePage = gotoPhoneScreen();
        Assert.assertTrue(phonePage.isLoaded(), "Phone number screen should open");
        Assert.assertNotNull(phonePage.getCountryCode(), "Country code selector should be present");
        Assert.assertFalse(phonePage.isContinueEnabled(),
                "'Continue' should be disabled before entering a number");
        phonePage.enterPhoneNumber("9876543210");
        Assert.assertEquals(phonePage.getEnteredNumber(), "9876543210",
                "Typed number should appear in the field");
        Assert.assertTrue(phonePage.isContinueEnabled(),
                "'Continue' should enable after a valid 10-digit number");
    }

    // =====================================================================
    // Phone-number screen
    // =====================================================================

    @PhoneNumberScreenTests
    @Test(groups = {"OnboardingPageTests", "PhoneNumberScreenTests"}, priority = 11,
            description = "All expected texts are visible on the phone screen")
    public void testAllTextsVisible() {
        PhoneNumberPage phonePage = gotoPhoneScreen();
        Assert.assertTrue(phonePage.isLoaded(),
                "Header '" + PhoneNumberPage.HEADER + "' should be visible");
        Assert.assertNotNull(phonePage.getCountryCode(), "Country code chip should be visible");
        Assert.assertTrue(phonePage.isPhoneHintVisible(), "'Phone number' input hint should be visible");
        Assert.assertTrue(phonePage.isConsentTextVisible(),
                "'" + PhoneNumberPage.CONSENT_TEXT + "' should be visible");
        Assert.assertTrue(phonePage.isNoSpamTextVisible(),
                "'No spam ever—pinky promise…' text should be visible");
    }

    @PhoneNumberScreenTests
    @Test(groups = {"OnboardingPageTests", "PhoneNumberScreenTests"}, priority = 12,
            description = "Country selector opens with heading, search box and Close")
    public void testCountrySelectorComponents() {
        PhoneNumberPage phonePage = gotoPhoneScreen();
        CountrySelectorPage selector = phonePage.openCountrySelector();
        Assert.assertTrue(selector.isLoaded(), "'Search Country' heading should be visible");
        Assert.assertTrue(selector.isCloseButtonVisible(), "Close button should be visible");
        Assert.assertTrue(selector.isSearchBoxVisible(), "'Country name' search box should be visible");
        Assert.assertFalse(selector.getListedCountryNames().isEmpty(), "Country rows should be listed");
        selector.close();
    }

    @PhoneNumberScreenTests
    @Test(groups = {"OnboardingPageTests", "PhoneNumberScreenTests"}, priority = 13,
            description = "Close button dismisses the selector WITHOUT changing selection")
    public void testCountrySelectorCloseKeepsSelection() {
        PhoneNumberPage phonePage = gotoPhoneScreen();
        String chipBefore = phonePage.getCountryCode();
        phonePage.openCountrySelector().close();
        Assert.assertEquals(phonePage.getCountryCode(), chipBefore,
                "Closing the selector must not change the selected country");
    }

    @PhoneNumberScreenTests
    @Test(groups = {"OnboardingPageTests", "PhoneNumberScreenTests"}, priority = 14,
            description = "Search filters by partial and full country name")
    public void testCountrySearchPartialAndFull() {
        PhoneNumberPage phonePage = gotoPhoneScreen();
        CountrySelectorPage selector = phonePage.openCountrySelector();
        List<String> partial = selector.search("united").getListedCountryNames();
        Assert.assertFalse(partial.isEmpty(), "'united' should list countries");
        for (String name : partial) {
            Assert.assertTrue(name.toLowerCase(Locale.ROOT).contains("united"),
                    "Row '" + name + "' does not match partial 'united'");
        }
        List<String> full = selector.search("United States").getListedCountryNames();
        Assert.assertEquals(full, List.of("United States"),
                "Full-name search should list exactly 'United States'");
        selector.close();
    }

    @PhoneNumberScreenTests
    @Test(groups = {"OnboardingPageTests", "PhoneNumberScreenTests"}, priority = 15,
            description = "Selecting a country updates the chip on the phone screen")
    public void testSelectCountryReflectsOnPhoneScreen() {
        PhoneNumberPage phonePage = gotoPhoneScreen();
        phonePage.openCountrySelector().search("United States").selectCountry("United States");
        Assert.assertTrue(phonePage.getCountryCode().contains("+1"),
                "Chip should show +1 after selecting United States, shows: " + phonePage.getCountryCode());
    }

    @PhoneNumberScreenTests
    @Test(groups = {"OnboardingPageTests", "PhoneNumberScreenTests"}, priority = 16,
            description = "Continue enables only at the correct number length")
    public void testContinueEnablesOnlyAtCorrectLength() {
        PhoneNumberPage phonePage = gotoPhoneScreen();
        phonePage.enterPhoneNumber("12345");
        Assert.assertFalse(phonePage.isContinueEnabled(), "'Continue' must stay disabled at 5 digits");
        phonePage.enterPhoneNumber("987654321");
        Assert.assertFalse(phonePage.isContinueEnabled(), "'Continue' must stay disabled at 9 digits");
        phonePage.enterPhoneNumber("9876543210");
        Assert.assertTrue(phonePage.isContinueEnabled(), "'Continue' must enable at 10 digits for +91");
    }

    @PhoneNumberScreenTests
    @Test(groups = {"OnboardingPageTests", "PhoneNumberScreenTests"}, priority = 17,
            description = "Consent checkbox gates Continue even with a valid number")
    public void testConsentToggleGatesContinue() {
        PhoneNumberPage phonePage = gotoPhoneScreen();
        phonePage.enterPhoneNumber("9876543210");
        Assert.assertTrue(phonePage.isContinueEnabled(), "Precondition: valid number should enable 'Continue'");
        phonePage.toggleConsentCheckbox();
        Assert.assertFalse(phonePage.isContinueEnabled(), "'Continue' must disable when consent is unchecked");
        phonePage.toggleConsentCheckbox();
        Assert.assertTrue(phonePage.isContinueEnabled(), "'Continue' must re-enable when consent is checked again");
    }

    // =====================================================================
    // OTP screen
    // =====================================================================

    @OtpScreenTests
    @Test(groups = {"OnboardingPageTests", "OtpScreenTests"}, priority = 21,
            description = "Header and 'OTP sent to' label are displayed")
    public void testOtpHeaderAndLabelDisplayed() {
        OtpPage otp = gotoOtpScreen();
        Assert.assertTrue(otp.isHeaderDisplayed(), "'Verify your phone number' header should be displayed");
        Assert.assertTrue(otp.isOtpSentLabelDisplayed(), "'OTP sent to' label should be displayed");
    }

    @OtpScreenTests
    @Test(groups = {"OnboardingPageTests", "OtpScreenTests"}, priority = 22,
            description = "The entered phone number is shown with +91 prefix")
    public void testOtpPhoneNumberShown() {
        OtpPage otp = gotoOtpScreen();
        String shown = otp.getPhoneNumberText();
        Assert.assertTrue(shown.contains(TEST_PHONE), "Number should contain " + TEST_PHONE + " but was: " + shown);
        Assert.assertTrue(shown.startsWith("+91"), "Number should carry the +91 country code but was: " + shown);
    }

    @OtpScreenTests
    @Test(groups = {"OnboardingPageTests", "OtpScreenTests"}, priority = 23,
            description = "Exactly six OTP input boxes are present")
    public void testOtpSixBoxes() {
        Assert.assertEquals(gotoOtpScreen().getBoxCount(), 6, "There should be exactly 6 OTP input boxes");
    }

    @OtpScreenTests
    @Test(groups = {"OnboardingPageTests", "OtpScreenTests"}, priority = 24,
            description = "Edit affordance is displayed")
    public void testOtpEditDisplayed() {
        Assert.assertTrue(gotoOtpScreen().isEditDisplayed(), "'Edit' should be displayed");
    }

    @OtpScreenTests
    @Test(groups = {"OnboardingPageTests", "OtpScreenTests"}, priority = 25,
            description = "Resend control is displayed (countdown or active)")
    public void testOtpResendDisplayed() {
        Assert.assertTrue(gotoOtpScreen().isResendDisplayed(),
                "A Resend control ('Resend in NN s' or 'Resend OTP') should be displayed");
    }

    @OtpScreenTests
    @Test(groups = {"OnboardingPageTests", "OtpScreenTests"}, priority = 26,
            description = "Typing digits fills the boxes one per cell")
    public void testOtpDigitsFillBoxes() {
        // Only 4 digits: entering all 6 would auto-submit and leave the screen.
        OtpPage otp = gotoOtpScreen().enterOtp("1234");
        Assert.assertEquals(otp.getEnteredCode(), "1234", "Each typed digit should occupy its own box");
    }

    @OtpScreenTests
    @Test(groups = {"OnboardingPageTests", "OtpScreenTests"}, priority = 27,
            description = "An incorrect OTP shows 'Invalid or expired OTP.' and stays")
    public void testOtpInvalidShowsError() {
        OtpPage otp = gotoOtpScreen();
        otp.submitInvalidOtp("000000");
        Assert.assertTrue(otp.isInvalidOtpErrorDisplayed(), "'Invalid or expired OTP.' error should be displayed");
        Assert.assertTrue(otp.isLoaded(), "Should remain on the OTP screen after an invalid code");
    }

    @OtpScreenTests
    @Test(groups = {"OnboardingPageTests", "OtpScreenTests"}, priority = 28,
            description = "Edit returns to the phone screen with the number preserved")
    public void testOtpEditReturnsToPhoneScreen() {
        OtpPage otp = gotoOtpScreen();
        PhoneNumberPage phone = otp.tapEdit();
        Assert.assertTrue(phone.isLoaded(), "Should return to the phone-number screen");
        Assert.assertEquals(phone.getEnteredNumber(), TEST_PHONE,
                "The previously entered number should be preserved for editing");
    }

    @OtpScreenTests
    @Test(groups = {"OnboardingPageTests", "OtpScreenTests"}, priority = 29,
            description = "Correct OTP (123456) verifies and leaves the OTP screen")
    public void testOtpValidProceeds() {
        OtpPage otp = gotoOtpScreen();
        otp.submitValidOtp(VALID_OTP);
        Assert.assertTrue(otp.isDismissed(),
                "Entering the valid OTP should dismiss the OTP screen and proceed");
    }

    // =====================================================================
    // Full login flow (originates from LoggedInBaseTest)
    // =====================================================================

    @LoggedInBaseTests
    @Test(groups = {"OnboardingPageTests", "LoggedInBaseTests"}, priority = 31,
            description = "Full login (landing -> phone -> OTP -> interstitials) reaches Home")
    public void testFullLoginReachesHome() {
        HomePage home = LoginFlow.login(driver);
        Assert.assertTrue(home.isLoaded(), "Login should land on the Home tab (bottom navigation present)");
    }

    // ---------------------------------------------------------------------

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
