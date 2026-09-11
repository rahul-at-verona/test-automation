package club.verona.automation.tests;

import club.verona.automation.annotations.EducationScreenTests;
import club.verona.automation.annotations.GenderScreenTests;
import club.verona.automation.annotations.IntroductionScreenTests;
import club.verona.automation.annotations.LandingPageTests;
import club.verona.automation.annotations.LoggedInBaseTests;
import club.verona.automation.annotations.OtpScreenTests;
import club.verona.automation.annotations.PhoneNumberScreenTests;
import club.verona.automation.annotations.ProfessionScreenTests;
import club.verona.automation.annotations.ProfileForScreenTests;
import club.verona.automation.core.DriverFactory;
import club.verona.automation.flows.LoginFlow;
import club.verona.automation.pages.HomePage;
import club.verona.automation.pages.onboarding.BrowserPage;
import club.verona.automation.pages.onboarding.CountrySelectorPage;
import club.verona.automation.pages.onboarding.CreateMemberProfilePage;
import club.verona.automation.pages.onboarding.EducationSearchPage;
import club.verona.automation.pages.onboarding.IntroductionPage;
import club.verona.automation.pages.onboarding.InviteMemberPage;
import club.verona.automation.pages.onboarding.JoinTheClubPage;
import club.verona.automation.pages.onboarding.LandingPage;
import club.verona.automation.pages.onboarding.OtpPage;
import club.verona.automation.pages.onboarding.PhoneNumberPage;
import club.verona.automation.pages.onboarding.PostgraduateEducationPage;
import club.verona.automation.pages.onboarding.ProfessionPage;
import club.verona.automation.pages.onboarding.ProfessionSearchPage;
import club.verona.automation.pages.onboarding.UndergraduateEducationPage;
import club.verona.automation.pages.onboarding.WhosThisProfileForPage;
import club.verona.automation.pages.onboarding.YouIdentifyAsPage;
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
 *   - IntroductionScreenTests (combined name + email screen, new signups only)
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
        try {
            if (firstMethod) {
                firstMethod = false; // @BeforeClass already gave us a fresh landing
            } else {
                DriverFactory.clearAppStorage(driver);
                ((AndroidDriver) driver).activateApp("club.verona");
            }
            landingPage = new LandingPage(driver).waitUntilLoaded();
        } catch (org.openqa.selenium.WebDriverException e) {
            // A dropped remote-grid session (observed live: UnreachableBrowserException,
            // NoSuchSessionException, connection resets) otherwise cascades into every
            // remaining test in the class failing/skipping the same way, since the driver
            // session is shared across the whole class (@BeforeClass). Rebuild once and
            // retry with a full clear+relaunch, mirroring ProfileTests.recoverToProfile().
            resetSession();
            DriverFactory.clearAppStorage(driver);
            ((AndroidDriver) driver).activateApp("club.verona");
            landingPage = new LandingPage(driver).waitUntilLoaded();
        }
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

    /**
     * Verifies OTP with a never-before-used phone number and lands on the
     * introduction (name+email) screen. Unlike {@link #gotoOtpScreen()},
     * this can't reuse {@code TEST_PHONE}: that number already has a
     * profile, so verifying it skips straight past this screen to Home
     * (see {@link LoginFlow}). A fresh number is generated per call so
     * re-running these tests never hits an already-onboarded number.
     */
    private IntroductionPage gotoIntroductionScreen() {
        PhoneNumberPage phone = gotoPhoneScreen();
        phone.enterPhoneNumber(freshUnregisteredPhone());
        phone.tapContinue();
        new OtpPage(driver).waitUntilLoaded().submitValidOtp(VALID_OTP);
        return new IntroductionPage(driver).waitUntilLoaded();
    }

    /**
     * Completes the introduction screen with disposable test data and lands
     * on {@link WhosThisProfileForPage}. Uses a fresh unregistered phone per
     * call (see {@link #gotoIntroductionScreen()}).
     */
    private WhosThisProfileForPage gotoWhosThisProfileForScreen() {
        IntroductionPage intro = gotoIntroductionScreen();
        intro.enterFirstName("Test");
        intro.enterLastName("User");
        intro.enterEmail("qa.test." + System.currentTimeMillis() + "@example.com");
        intro.tapContinue();
        return new WhosThisProfileForPage(driver).waitUntilLoaded();
    }

    private static String freshUnregisteredPhone() {
        long suffix = Math.abs((System.nanoTime() ^ System.currentTimeMillis()) % 1_000_000_000L);
        return String.format("9%09d", suffix);
    }

    // =====================================================================
    // Landing page
    // =====================================================================
    @LandingPageTests
    @Test(groups = {"OnboardingPageTests", "LandingPageTests","Regression"}, priority = 1,
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
    @Test(groups = {"OnboardingPageTests", "OtpScreenTests","Regression"}, priority = 24,
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
    // Introduction screen (combined name + email, new signups only)
    // =====================================================================

    @IntroductionScreenTests
    @Test(groups = {"OnboardingPageTests", "IntroductionScreenTests"}, priority = 31,
            description = "Combined name+email screen shows both sections and disables Continue until filled")
    public void testIntroductionScreenLoadsWithNameAndEmailSections() {
        IntroductionPage intro = gotoIntroductionScreen();
        Assert.assertTrue(intro.isLoaded(), "'" + IntroductionPage.HEADER + "' header should be visible");
        Assert.assertTrue(intro.isNameSectionVisible(),
                "'" + IntroductionPage.NAME_SECTION_LABEL + "' section should be visible");
        Assert.assertTrue(intro.isNameHelperTextVisible(), "Name helper text should be visible");
        Assert.assertTrue(intro.isEmailSectionVisible(),
                "'" + IntroductionPage.EMAIL_SECTION_LABEL + "' section should be visible");
        Assert.assertFalse(intro.isContinueEnabled(),
                "'Continue' should be disabled before name and email are filled");
    }

    @IntroductionScreenTests
    @Test(groups = {"OnboardingPageTests", "IntroductionScreenTests"}, priority = 32,
            description = "Filling first name, last name and email enables Continue and advances past the screen")
    public void testIntroductionCompletingNameAndEmailAdvances() {
        IntroductionPage intro = gotoIntroductionScreen();
        intro.enterFirstName("Test");
        intro.enterLastName("User");
        intro.enterEmail("qa.test." + System.currentTimeMillis() + "@example.com");
        Assert.assertTrue(intro.isContinueEnabled(),
                "'Continue' should enable once first name, last name and email are filled");
        intro.tapContinue();
        Assert.assertFalse(intro.isLoaded(), "Should leave the introduction screen after Continue");
    }

    // =====================================================================
    // "Who's this profile for?" screen (member-type picker) — my-sibling flow
    // =====================================================================

    @ProfileForScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfileForScreenTests"}, priority = 33,
            description = "'Who's this profile for?' loads with selector, helper texts and disabled Continue")
    public void testWhosThisProfileForScreenLoads() {
        WhosThisProfileForPage page = gotoWhosThisProfileForScreen();
        Assert.assertTrue(page.isLoaded(), "'" + WhosThisProfileForPage.HEADER + "' header should be visible");
        Assert.assertTrue(page.isSelectorVisible(), "'Select member' selector should be visible");
        Assert.assertTrue(page.areHelperTextsVisible(), "Both helper texts should be visible");
        Assert.assertFalse(page.isContinueEnabled(),
                "'Continue' should be disabled before a member type is chosen");
    }

    @ProfileForScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfileForScreenTests"}, priority = 34,
            description = "Opening the selector reveals all 4 member-type options")
    public void testSelectorRevealsAllFourMemberOptions() {
        WhosThisProfileForPage page = gotoWhosThisProfileForScreen().openSelector();
        Assert.assertTrue(page.isOptionVisible(WhosThisProfileForPage.MYSELF), "'Myself' option should be visible");
        Assert.assertTrue(page.isOptionVisible(WhosThisProfileForPage.MY_CHILD), "'My child' option should be visible");
        Assert.assertTrue(page.isOptionVisible(WhosThisProfileForPage.MY_SIBLING), "'My sibling' option should be visible");
        Assert.assertTrue(page.isOptionVisible(WhosThisProfileForPage.SOMEONE_ELSE), "'Someone else' option should be visible");
    }

    @ProfileForScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfileForScreenTests"}, priority = 35,
            description = "Selecting 'My sibling' updates the selector label and enables Continue")
    public void testSelectingMySiblingEnablesContinue() {
        WhosThisProfileForPage page = gotoWhosThisProfileForScreen()
                .openSelector()
                .selectMemberType(WhosThisProfileForPage.MY_SIBLING);
        Assert.assertEquals(page.getSelectorLabel(), WhosThisProfileForPage.MY_SIBLING,
                "Selector should show the chosen option");
        Assert.assertTrue(page.isContinueEnabled(), "'Continue' should enable once a member type is chosen");
    }

    @ProfileForScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfileForScreenTests"}, priority = 36,
            description = "My sibling: Continue opens 'Create profile' with name + phone fields")
    public void testMySiblingOpensCreateProfileScreen() {
        CreateMemberProfilePage createPage = gotoWhosThisProfileForScreen()
                .openSelector()
                .selectMemberType(WhosThisProfileForPage.MY_SIBLING)
                .tapContinue();

        Assert.assertTrue(createPage.isLoaded(), "'" + CreateMemberProfilePage.HEADER + "' header should be visible");
        Assert.assertTrue(createPage.isSubHeaderVisible(),
                "'" + CreateMemberProfilePage.SUB_HEADER + "' sub-header should be visible");
        String countryCode = createPage.getCountryCode();
        Assert.assertNotNull(countryCode, "Country code chip should be visible");
        Assert.assertTrue(countryCode.contains("+91"), "Country code should default to +91 but was: " + countryCode);
        Assert.assertFalse(createPage.isContinueEnabled(),
                "'Continue' should be disabled before name and phone are filled");
    }

    @ProfileForScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfileForScreenTests"}, priority = 37,
            description = "My sibling: completing name + phone advances to the invite screen")
    public void testMySiblingCreateProfileAdvancesToInviteScreen() {
        CreateMemberProfilePage createPage = gotoWhosThisProfileForScreen()
                .openSelector()
                .selectMemberType(WhosThisProfileForPage.MY_SIBLING)
                .tapContinue();

        InviteMemberPage invite = createPage.completeWith("Sib", "Ling", freshUnregisteredPhone());

        Assert.assertTrue(invite.isLoaded(), "'" + InviteMemberPage.HEADER + "' header should be visible");
        Assert.assertTrue(invite.isBodyTextVisible(), "Body text should be visible");
        Assert.assertTrue(invite.isInviteCtaVisible(), "'" + InviteMemberPage.INVITE_CTA + "' should be visible");
        Assert.assertTrue(invite.isShareVisible(), "'Share' control should be visible");
        Assert.assertTrue(invite.isTextConciergeVisible(), "'Text Verona Concierge' control should be visible");
        Assert.assertTrue(invite.isSignedUpOnBehalfTextVisible(InviteMemberPage.RELATION_SIBLING),
                "Confirmation text should name 'sibling' as the relation");
        Assert.assertTrue(invite.isLogOutVisible(), "'Log out' should be visible");
    }

    @ProfileForScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfileForScreenTests"}, priority = 38,
            description = "My child: Continue opens 'Create profile' with name + phone fields")
    public void testMyChildOpensCreateProfileScreen() {
        CreateMemberProfilePage createPage = gotoWhosThisProfileForScreen()
                .openSelector()
                .selectMemberType(WhosThisProfileForPage.MY_CHILD)
                .tapContinue();

        Assert.assertTrue(createPage.isLoaded(), "'" + CreateMemberProfilePage.HEADER + "' header should be visible");
        Assert.assertTrue(createPage.isSubHeaderVisible(),
                "'" + CreateMemberProfilePage.SUB_HEADER + "' sub-header should be visible");
        Assert.assertFalse(createPage.isContinueEnabled(),
                "'Continue' should be disabled before name and phone are filled");
    }

    @ProfileForScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfileForScreenTests"}, priority = 39,
            description = "My child: completing name + phone advances to the invite screen")
    public void testMyChildCreateProfileAdvancesToInviteScreen() {
        CreateMemberProfilePage createPage = gotoWhosThisProfileForScreen()
                .openSelector()
                .selectMemberType(WhosThisProfileForPage.MY_CHILD)
                .tapContinue();

        InviteMemberPage invite = createPage.completeWith("Kid", "Doe", freshUnregisteredPhone());

        Assert.assertTrue(invite.isLoaded(), "'" + InviteMemberPage.HEADER + "' header should be visible");
        Assert.assertTrue(invite.isSignedUpOnBehalfTextVisible(InviteMemberPage.RELATION_CHILD),
                "Confirmation text should name 'child' as the relation");
    }

    @ProfileForScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfileForScreenTests"}, priority = 40,
            description = "Someone else: Continue opens 'Create profile' with name + phone fields")
    public void testSomeoneElseOpensCreateProfileScreen() {
        CreateMemberProfilePage createPage = gotoWhosThisProfileForScreen()
                .openSelector()
                .selectMemberType(WhosThisProfileForPage.SOMEONE_ELSE)
                .tapContinue();

        Assert.assertTrue(createPage.isLoaded(), "'" + CreateMemberProfilePage.HEADER + "' header should be visible");
        Assert.assertTrue(createPage.isSubHeaderVisible(),
                "'" + CreateMemberProfilePage.SUB_HEADER + "' sub-header should be visible");
        Assert.assertFalse(createPage.isContinueEnabled(),
                "'Continue' should be disabled before name and phone are filled");
    }

    @ProfileForScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfileForScreenTests"}, priority = 41,
            description = "Someone else: completing name + phone advances to the invite screen")
    public void testSomeoneElseCreateProfileAdvancesToInviteScreen() {
        CreateMemberProfilePage createPage = gotoWhosThisProfileForScreen()
                .openSelector()
                .selectMemberType(WhosThisProfileForPage.SOMEONE_ELSE)
                .tapContinue();

        InviteMemberPage invite = createPage.completeWith("Some", "One", freshUnregisteredPhone());

        Assert.assertTrue(invite.isLoaded(), "'" + InviteMemberPage.HEADER + "' header should be visible");
        Assert.assertTrue(invite.isSignedUpOnBehalfTextVisible(InviteMemberPage.RELATION_SOMEONE_ELSE),
                "Confirmation text should name 'relative' as the relation");
    }

    @ProfileForScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfileForScreenTests"}, priority = 42,
            description = "Selecting 'Myself' updates the selector label and enables Continue")
    public void testSelectingMyselfEnablesContinue() {
        WhosThisProfileForPage page = gotoWhosThisProfileForScreen()
                .openSelector()
                .selectMemberType(WhosThisProfileForPage.MYSELF);
        Assert.assertEquals(page.getSelectorLabel(), WhosThisProfileForPage.MYSELF,
                "Selector should show the chosen option");
        Assert.assertTrue(page.isContinueEnabled(), "'Continue' should enable once a member type is chosen");
    }

    @ProfileForScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfileForScreenTests"}, priority = 43,
            description = "Myself: Continue dismisses the notification-permission dialog and opens 'Join the Club'")
    public void testMyselfOpensJoinTheClubScreen() {
        JoinTheClubPage joinPage = gotoWhosThisProfileForScreen()
                .openSelector()
                .selectMemberType(WhosThisProfileForPage.MYSELF)
                .tapContinueAsMyself();

        Assert.assertTrue(joinPage.isLoaded(), "'" + JoinTheClubPage.HEADER + "' header should be visible");
        Assert.assertTrue(joinPage.areTextsVisible(), "All 'Join the Club' body texts should be visible");
        Assert.assertTrue(joinPage.isStartApplicationVisible(),
                "'" + JoinTheClubPage.START_APPLICATION + "' should be visible");
        Assert.assertTrue(joinPage.isStartApplicationEnabled(),
                "'" + JoinTheClubPage.START_APPLICATION + "' should be enabled");
    }

    // =====================================================================
    // "You identify as" screen (gender picker) — Join the Club -> Start
    // application, Myself path only
    // =====================================================================

    /**
     * Drives 'Myself' all the way to {@link YouIdentifyAsPage}: Who's this
     * profile for -> Myself -> notification dialog -> Join the Club ->
     * Start application.
     */
    private YouIdentifyAsPage gotoYouIdentifyAsScreen() {
        return gotoWhosThisProfileForScreen()
                .openSelector()
                .selectMemberType(WhosThisProfileForPage.MYSELF)
                .tapContinueAsMyself()
                .tapStartApplication();
    }

    @GenderScreenTests
    @Test(groups = {"OnboardingPageTests", "GenderScreenTests"}, priority = 44,
            description = "Start application opens 'You identify as' with selector, note and disabled Continue")
    public void testStartApplicationOpensYouIdentifyAsScreen() {
        YouIdentifyAsPage page = gotoYouIdentifyAsScreen();
        Assert.assertTrue(page.isLoaded(), "'" + YouIdentifyAsPage.HEADER + "' header should be visible");
        Assert.assertTrue(page.isSelectorVisible(), "'Gender' selector should be visible");
        Assert.assertTrue(page.isCisgenderNoteVisible(), "Cisgender note should be visible");
        Assert.assertFalse(page.isContinueEnabled(),
                "'Continue' should be disabled before a gender is chosen");
    }

    @GenderScreenTests
    @Test(groups = {"OnboardingPageTests", "GenderScreenTests"}, priority = 45,
            description = "Opening the selector reveals both 'Female' and 'Male' options")
    public void testGenderSelectorRevealsBothOptions() {
        YouIdentifyAsPage page = gotoYouIdentifyAsScreen().openSelector();
        Assert.assertTrue(page.isOptionVisible(YouIdentifyAsPage.FEMALE), "'Female' option should be visible");
        Assert.assertTrue(page.isOptionVisible(YouIdentifyAsPage.MALE), "'Male' option should be visible");
    }

    @GenderScreenTests
    @Test(groups = {"OnboardingPageTests", "GenderScreenTests"}, priority = 46,
            description = "Selecting 'Female' updates the selector label and enables Continue")
    public void testSelectingFemaleEnablesContinue() {
        YouIdentifyAsPage page = gotoYouIdentifyAsScreen()
                .openSelector()
                .selectGender(YouIdentifyAsPage.FEMALE);
        Assert.assertEquals(page.getSelectorLabel(), YouIdentifyAsPage.FEMALE,
                "Selector should show the chosen option");
        Assert.assertTrue(page.isContinueEnabled(), "'Continue' should enable once a gender is chosen");
    }

    @GenderScreenTests
    @Test(groups = {"OnboardingPageTests", "GenderScreenTests"}, priority = 47,
            description = "Selecting 'Male' updates the selector label and enables Continue")
    public void testSelectingMaleEnablesContinue() {
        YouIdentifyAsPage page = gotoYouIdentifyAsScreen()
                .openSelector()
                .selectGender(YouIdentifyAsPage.MALE);
        Assert.assertEquals(page.getSelectorLabel(), YouIdentifyAsPage.MALE,
                "Selector should show the chosen option");
        Assert.assertTrue(page.isContinueEnabled(), "'Continue' should enable once a gender is chosen");
    }

    // =====================================================================
    // Undergraduate / Postgraduate education screens — institute + degree
    // search-and-select, 'Other' free-text fallback, Continue gating
    // =====================================================================

    /** Drives 'Myself' all the way to {@link UndergraduateEducationPage} (gender: Female). */
    private UndergraduateEducationPage gotoUndergraduateEducationScreen() {
        return gotoYouIdentifyAsScreen()
                .openSelector()
                .selectGender(YouIdentifyAsPage.FEMALE)
                .tapContinue();
    }

    private static final String SAMPLE_UNIVERSITY = "AIIMS Rishikesh";
    private static final String SAMPLE_DEGREE = "Bachelor of Architecture (BArch)";
    // The postgraduate 'Field of Study' catalog lists master's/professional
    // qualifications, not undergraduate degrees like SAMPLE_DEGREE (verified
    // live — searching "Architecture" there returns nothing).
    private static final String SAMPLE_PG_DEGREE = "Chartered Accountant (CA)";

    @EducationScreenTests
    @Test(groups = {"OnboardingPageTests", "EducationScreenTests"}, priority = 48,
            description = "Undergraduate education screen loads with both selectors and disabled Continue")
    public void testUndergraduateScreenLoads() {
        UndergraduateEducationPage page = gotoUndergraduateEducationScreen();
        Assert.assertTrue(page.isLoaded(), "'" + UndergraduateEducationPage.HEADER + "' header should be visible");
        Assert.assertTrue(page.isSubtitleVisible(), "Subtitle should be visible");
        Assert.assertTrue(page.isUGExampleTextsVisible(), "Example Text should be visible");
        Assert.assertTrue(page.isInstituteSelectorVisible(), "Institute selector should be visible");
        Assert.assertTrue(page.isDegreeSelectorVisible(), "Degree selector should be visible");
        Assert.assertFalse(page.isContinueEnabled(),
                "'Continue' should be disabled before institute and degree are resolved");
    }

    @EducationScreenTests
    @Test(groups = {"OnboardingPageTests", "EducationScreenTests"}, priority = 49,
            description = "Institute search modal loads, lists results and narrows by search text")
    public void testInstituteSearchShowsResultsAndNarrowsBySearch() {
        EducationSearchPage search = gotoUndergraduateEducationScreen().openInstituteSearch();
        Assert.assertTrue(search.isLoaded(EducationSearchPage.UNIVERSITY_HEADING),
                "'" + EducationSearchPage.UNIVERSITY_HEADING + "' heading should be visible");
        Assert.assertTrue(search.isCloseButtonVisible(), "Close button should be visible");
        Assert.assertTrue(search.isSearchBoxVisible(), "Search box should be visible");
        Assert.assertFalse(search.getListedOptionNames().isEmpty(), "Universities should be listed by default");

        List<String> results = search.search(SAMPLE_UNIVERSITY).getListedOptionNames();
        Assert.assertTrue(results.contains(SAMPLE_UNIVERSITY),
                "Searching '" + SAMPLE_UNIVERSITY + "' should list it, saw: " + results);
    }

    @EducationScreenTests
    @Test(groups = {"OnboardingPageTests", "EducationScreenTests"}, priority = 50,
            description = "Selecting a real institute alone leaves Continue disabled (degree still unresolved)")
    public void testSelectingInstituteOnlyKeepsContinueDisabled() {
        UndergraduateEducationPage page = gotoUndergraduateEducationScreen();
        page.openInstituteSearch().search(SAMPLE_UNIVERSITY).selectOption(SAMPLE_UNIVERSITY);
        Assert.assertTrue(page.isOptionSelected(SAMPLE_UNIVERSITY),
                "Institute selector should now show '" + SAMPLE_UNIVERSITY + "'");
        Assert.assertFalse(page.isContinueEnabled(),
                "'Continue' should stay disabled with only the institute resolved");
    }

    @EducationScreenTests
    @Test(groups = {"OnboardingPageTests", "EducationScreenTests"}, priority = 51,
            description = "Selecting 'Other' for institute reveals the free-text field, still disabling Continue until filled")
    public void testSelectingOtherForInstituteRevealsExtraField() {
        UndergraduateEducationPage page = gotoUndergraduateEducationScreen();
        EducationSearchPage search = page.openInstituteSearch().search("other");
        Assert.assertTrue(search.isOtherOptionVisible(), "'Other (Not listed above)' should be listed");
        search.selectOther();

        Assert.assertTrue(page.isOptionSelected(EducationSearchPage.OTHER_OPTION),
                "Institute selector should now show 'Other (Not listed above)'");
        Assert.assertTrue(page.isInstituteOtherFieldVisible(),
                "Selecting 'Other' should reveal the '" + UndergraduateEducationPage.INSTITUTE_OTHER_HINT + "' field");
        Assert.assertFalse(page.isContinueEnabled(),
                "'Continue' should stay disabled while the 'Other' institute field is still empty");
    }

    @EducationScreenTests
    @Test(groups = {"OnboardingPageTests", "EducationScreenTests"}, priority = 52,
            description = "Degree search modal loads, lists results and narrows by search text")
    public void testDegreeSearchShowsResultsAndNarrowsBySearch() {
        EducationSearchPage search = gotoUndergraduateEducationScreen().openDegreeSearch();
        Assert.assertTrue(search.isLoaded(EducationSearchPage.DEGREE_HEADING),
                "'" + EducationSearchPage.DEGREE_HEADING + "' heading should be visible");
        Assert.assertTrue(search.isCloseButtonVisible(), "Close button should be visible");
        Assert.assertTrue(search.isSearchBoxVisible(), "Search box should be visible");
        Assert.assertFalse(search.getListedOptionNames().isEmpty(), "Degrees should be listed by default");

        List<String> results = search.search("Architecture").getListedOptionNames();
        Assert.assertTrue(results.contains(SAMPLE_DEGREE),
                "Searching 'Architecture' should list '" + SAMPLE_DEGREE + "', saw: " + results);
    }

    @EducationScreenTests
    @Test(groups = {"OnboardingPageTests", "EducationScreenTests"}, priority = 53,
            description = "Selecting 'Other' for degree reveals the free-text field, still disabling Continue until filled")
    public void testSelectingOtherForDegreeRevealsExtraField() {
        UndergraduateEducationPage page = gotoUndergraduateEducationScreen();
        EducationSearchPage search = page.openDegreeSearch().search("other");
        Assert.assertTrue(search.isOtherOptionVisible(), "'Other (Not listed above)' should be listed");
        search.selectOther();

        Assert.assertTrue(page.isOptionSelected(EducationSearchPage.OTHER_OPTION),
                "Degree selector should now show 'Other (Not listed above)'");
        Assert.assertTrue(page.isDegreeOtherFieldVisible(),
                "Selecting 'Other' should reveal the '" + UndergraduateEducationPage.DEGREE_OTHER_HINT + "' field");
        Assert.assertFalse(page.isContinueEnabled(),
                "'Continue' should stay disabled while the 'Other' degree field is still empty");
    }

    @EducationScreenTests
    @Test(groups = {"OnboardingPageTests", "EducationScreenTests"}, priority = 54,
            description = "Continue enables once both institute and degree are resolved with real selections")
    public void testContinueEnablesWhenBothRealFieldsSelected() {
        UndergraduateEducationPage page = gotoUndergraduateEducationScreen();
        page.openInstituteSearch().search(SAMPLE_UNIVERSITY).selectOption(SAMPLE_UNIVERSITY);
        Assert.assertFalse(page.isContinueEnabled(), "Precondition: degree still unresolved");
        page.openDegreeSearch().search("Architecture").selectOption(SAMPLE_DEGREE);
        Assert.assertTrue(page.isContinueEnabled(),
                "'Continue' should enable once both institute and degree are selected");
    }

    @EducationScreenTests
    @Test(groups = {"OnboardingPageTests", "EducationScreenTests"}, priority = 55,
            description = "Continue enables once both 'Other' free-text fields are filled")
    public void testContinueEnablesWhenBothOtherFieldsFilled() {
        UndergraduateEducationPage page = gotoUndergraduateEducationScreen();
        page.openInstituteSearch().selectOther();
        page.openDegreeSearch().selectOther();
        Assert.assertFalse(page.isContinueEnabled(), "Precondition: both 'Other' fields still empty");

        page.enterInstituteOtherText("My Custom Institute, Testville");
        Assert.assertFalse(page.isContinueEnabled(), "Precondition: degree 'Other' field still empty");
        page.enterDegreeOtherText("My Custom Degree");
        Assert.assertTrue(page.isContinueEnabled(),
                "'Continue' should enable once both 'Other' free-text fields are filled");
    }

    @EducationScreenTests
    @Test(groups = {"OnboardingPageTests", "EducationScreenTests"}, priority = 56,
            description = "Completing undergraduate education advances to the postgraduate screen")
    public void testUndergraduateContinueAdvancesToPostgraduateScreen() {
        UndergraduateEducationPage ug = gotoUndergraduateEducationScreen();
        ug.openInstituteSearch().search(SAMPLE_UNIVERSITY).selectOption(SAMPLE_UNIVERSITY);
        ug.openDegreeSearch().search("Architecture").selectOption(SAMPLE_DEGREE);
        PostgraduateEducationPage pg = ug.tapContinue();

        Assert.assertTrue(pg.isLoaded(), "'" + PostgraduateEducationPage.HEADER + "' header should be visible");
        Assert.assertTrue(pg.isSubtitleVisible(), "Subtitle should be visible");
        Assert.assertTrue(pg.isInstituteSelectorVisible(), "Institute selector should be visible");
        Assert.assertTrue(pg.isDegreeSelectorVisible(), "Degree selector should be visible");
        Assert.assertTrue(pg.isNoDegreeOptOutVisible(), "'No postgraduate degree' opt-out should be visible");
        Assert.assertFalse(pg.isContinueEnabled(),
                "'Continue' should be disabled before institute/degree are resolved or the opt-out is checked");
    }

    @EducationScreenTests
    @Test(groups = {"OnboardingPageTests", "EducationScreenTests"}, priority = 57,
            description = "Postgraduate: checking 'no postgraduate degree' enables Continue without filling fields")
    public void testNoPostgraduateDegreeEnablesContinueWithoutFillingFields() {
        UndergraduateEducationPage ug = gotoUndergraduateEducationScreen();
        ug.openInstituteSearch().selectOther();
        ug.openDegreeSearch().selectOther();
        ug.enterInstituteOtherText("My Custom Institute, Testville");
        ug.enterDegreeOtherText("My Custom Degree");
        PostgraduateEducationPage pg = ug.tapContinue();

        pg.checkNoPostgraduateDegree();
        Assert.assertTrue(pg.isContinueEnabled(),
                "'Continue' should enable once 'no postgraduate degree' is checked, with no fields filled");
    }

    @EducationScreenTests
    @Test(groups = {"OnboardingPageTests", "EducationScreenTests"}, priority = 58,
            description = "Postgraduate: Continue enables once both institute and degree are resolved with real selections")
    public void testPostgraduateContinueEnablesWhenBothRealFieldsSelected() {
        UndergraduateEducationPage ug = gotoUndergraduateEducationScreen();
        ug.openInstituteSearch().selectOther();
        ug.openDegreeSearch().selectOther();
        ug.enterInstituteOtherText("My Custom Institute, Testville");
        ug.enterDegreeOtherText("My Custom Degree");
        PostgraduateEducationPage pg = ug.tapContinue();

        pg.openInstituteSearch().search(SAMPLE_UNIVERSITY).selectOption(SAMPLE_UNIVERSITY);
        Assert.assertFalse(pg.isContinueEnabled(), "Precondition: PG degree still unresolved");
        pg.openDegreeSearch().search("Chartered").selectOption(SAMPLE_PG_DEGREE);
        Assert.assertTrue(pg.isContinueEnabled(),
                "'Continue' should enable once both PG institute and degree are selected");
    }

    // =====================================================================
    // "Your current profession" screen — 5 profession-type options, each
    // with a different field set and Continue-gating rule (all verified
    // live individually)
    // =====================================================================

    /** Drives 'Myself' all the way to {@link ProfessionPage} (UG/PG both via 'Other' + free text). */
    private ProfessionPage gotoProfessionScreen() {
        UndergraduateEducationPage ug = gotoUndergraduateEducationScreen();
        ug.openInstituteSearch().selectOther();
        ug.openDegreeSearch().selectOther();
        ug.enterInstituteOtherText("My Custom Institute, Testville");
        ug.enterDegreeOtherText("My Custom Degree");
        PostgraduateEducationPage pg = ug.tapContinue();
        pg.checkNoPostgraduateDegree();
        return pg.tapContinue();
    }

    private static final String SAMPLE_ORGANIZATION_QUERY = "Google";
    private static final String SAMPLE_ORGANIZATION = "Alphabet Google";
    private static final String SAMPLE_ORGANIZATION_RESOLVED_LABEL = "Alphabet (Google)";
    private static final String SAMPLE_DESIGNATION_QUERY = "Manager";
    // Disambiguates the exact 'Manager' row from 'Manager Operations' etc. (verified live: trailing padding).
    private static final String SAMPLE_DESIGNATION_EXACT_ROW = "Manager   ";
    private static final String SAMPLE_DESIGNATION_RESOLVED_LABEL = "Manager";
    private static final String SAMPLE_PROFESSION_QUERY = "Doctor";
    private static final String SAMPLE_PROFESSION_ROW = "Ayurvedic Doctor";

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 59,
            description = "Profession screen loads with all 5 options visible and disabled Continue")
    public void testProfessionScreenLoadsWithAllFiveOptions() {
        ProfessionPage page = gotoProfessionScreen();
        Assert.assertTrue(page.isLoaded(), "'" + ProfessionPage.HEADER + "' header should be visible");
        Assert.assertTrue(page.isOptionVisible(ProfessionPage.EMPLOYED), "'Employed' option should be visible");
        Assert.assertTrue(page.isOptionVisible(ProfessionPage.BUSINESS_OWNER), "'Business owner' option should be visible");
        Assert.assertTrue(page.isOptionVisible(ProfessionPage.SELF_EMPLOYED),
                "'Self-employed / Independent professional' option should be visible");
        Assert.assertTrue(page.isOptionVisible(ProfessionPage.CURRENTLY_NOT_WORKING),
                "'Currently not working' option should be visible");
        Assert.assertTrue(page.isOptionVisible(ProfessionPage.STUDENT), "'Student' option should be visible");
        Assert.assertFalse(page.isContinueEnabled(), "'Continue' should be disabled before a profession is chosen");
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 60,
            description = "Employed shows Organization + example text + Designation + LinkedIn")
    public void testSelectingEmployedShowsOrganizationDesignationAndLinkedIn() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.EMPLOYED);
        Assert.assertTrue(page.isOrganizationSelectorVisible(), "Organization selector should be visible");
        Assert.assertTrue(page.isOrganizationExampleTextVisible(), "Organization example text should be visible");
        Assert.assertTrue(page.isDesignationSelectorVisible(), "Designation selector should be visible");
        Assert.assertTrue(page.isLinkedInFieldVisible(), "LinkedIn field should be visible");
        Assert.assertFalse(page.isSharePlatformNoteVisible(), "Share-platform note should NOT be shown for Employed");
        Assert.assertFalse(page.isContinueEnabled(), "'Continue' should be disabled before fields are resolved");
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 61,
            description = "Employed: Continue enables with Organization + Designation only, LinkedIn left empty")
    public void testEmployedContinueEnablesWithOrganizationAndDesignationOnly() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.EMPLOYED);
        page.openOrganizationSearch().search(SAMPLE_ORGANIZATION_QUERY).selectOption(SAMPLE_ORGANIZATION);
        Assert.assertTrue(page.isOptionSelected(SAMPLE_ORGANIZATION_RESOLVED_LABEL),
                "Organization selector should show '" + SAMPLE_ORGANIZATION_RESOLVED_LABEL + "'");
        Assert.assertFalse(page.isContinueEnabled(), "Precondition: Designation still unresolved");

        page.openDesignationSearch().search(SAMPLE_DESIGNATION_QUERY).selectOption(SAMPLE_DESIGNATION_EXACT_ROW);
        Assert.assertTrue(page.isOptionSelected(SAMPLE_DESIGNATION_RESOLVED_LABEL),
                "Designation selector should show '" + SAMPLE_DESIGNATION_RESOLVED_LABEL + "'");
        Assert.assertTrue(page.isContinueEnabled(),
                "'Continue' should enable once Organization and Designation are resolved (LinkedIn left empty)");
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 62,
            description = "Business owner shows only Organization + LinkedIn + share-platform note (no Designation, no example text)")
    public void testSelectingBusinessOwnerShowsOrganizationAndLinkedInOnly() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.BUSINESS_OWNER);
        Assert.assertTrue(page.isOrganizationSelectorVisible(), "Organization selector should be visible");
        Assert.assertFalse(page.isDesignationSelectorVisible(),
                "Designation selector should NOT be shown for Business owner");
        Assert.assertFalse(page.isOrganizationExampleTextVisible(),
                "Organization example text should NOT be shown for Business owner");
        Assert.assertTrue(page.isLinkedInFieldVisible(), "LinkedIn field should be visible");
        Assert.assertTrue(page.isSharePlatformNoteVisible(), "Share-platform note should be visible for Business owner");
        Assert.assertFalse(page.isContinueEnabled(), "'Continue' should be disabled before Organization is resolved");
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 63,
            description = "Business owner: Continue enables with Organization alone, LinkedIn left empty")
    public void testBusinessOwnerContinueEnablesWithOrganizationOnly() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.BUSINESS_OWNER);
        page.openOrganizationSearch().search(SAMPLE_ORGANIZATION_QUERY).selectOption(SAMPLE_ORGANIZATION);
        Assert.assertTrue(page.isContinueEnabled(),
                "'Continue' should enable once Organization is resolved — LinkedIn is optional for Business owner");
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 64,
            description = "Self-employed shows only Profession + LinkedIn + share-platform note")
    public void testSelectingSelfEmployedShowsProfessionAndLinkedIn() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.SELF_EMPLOYED);
        Assert.assertTrue(page.isProfessionSelectorVisible(), "Profession selector should be visible");
        Assert.assertFalse(page.isOrganizationSelectorVisible(),
                "Organization selector should NOT be shown for Self-employed");
        Assert.assertTrue(page.isLinkedInFieldVisible(), "LinkedIn field should be visible");
        Assert.assertTrue(page.isSharePlatformNoteVisible(), "Share-platform note should be visible");
        Assert.assertFalse(page.isContinueEnabled(), "'Continue' should be disabled before Profession is resolved");
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 65,
            description = "Self-employed: Continue requires BOTH Profession resolved AND LinkedIn filled")
    public void testSelfEmployedContinueRequiresBothProfessionAndLinkedIn() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.SELF_EMPLOYED);
        page.openProfessionSearch().search(SAMPLE_PROFESSION_QUERY).selectOption(SAMPLE_PROFESSION_ROW);
        Assert.assertTrue(page.isOptionSelected(SAMPLE_PROFESSION_ROW),
                "Profession selector should show '" + SAMPLE_PROFESSION_ROW + "'");
        Assert.assertFalse(page.isContinueEnabled(),
                "'Continue' should stay disabled with only Profession resolved — unlike Business owner's single "
                + "selector, LinkedIn is required here too");

        page.enterLinkedIn("https://www.linkedin.com/in/testuser");
        Assert.assertTrue(page.isContinueEnabled(), "'Continue' should enable once LinkedIn is also filled");
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 66,
            description = "Currently not working shows Previous organization + Previous designation + LinkedIn")
    public void testSelectingCurrentlyNotWorkingShowsPreviousFieldsAndLinkedIn() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.CURRENTLY_NOT_WORKING);
        Assert.assertTrue(page.isPreviousOrganizationSelectorVisible(),
                "'Previous organization' selector should be visible");
        Assert.assertTrue(page.isPreviousDesignationSelectorVisible(),
                "'Previous designation' selector should be visible");
        Assert.assertTrue(page.isLinkedInFieldVisible(), "LinkedIn field should be visible");
        Assert.assertTrue(page.isSharePlatformNoteVisible(), "Share-platform note should be visible");
        Assert.assertFalse(page.isContinueEnabled(), "'Continue' should be disabled before fields are resolved");
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 67,
            description = "Currently not working: Continue requires BOTH previous fields AND LinkedIn filled")
    public void testCurrentlyNotWorkingContinueRequiresBothPreviousFieldsAndLinkedIn() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.CURRENTLY_NOT_WORKING);
        page.openPreviousOrganizationSearch().search(SAMPLE_ORGANIZATION_QUERY).selectOption(SAMPLE_ORGANIZATION);
        page.openPreviousDesignationSearch().search(SAMPLE_DESIGNATION_QUERY).selectOption(SAMPLE_DESIGNATION_EXACT_ROW);
        Assert.assertFalse(page.isContinueEnabled(),
                "'Continue' should stay disabled with both previous fields resolved but LinkedIn still empty");

        page.enterLinkedIn("https://www.linkedin.com/in/testuser");
        Assert.assertTrue(page.isContinueEnabled(), "'Continue' should enable once LinkedIn is also filled");
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 68,
            description = "Student shows only LinkedIn + share-platform note, no organization-style selector")
    public void testSelectingStudentShowsOnlyLinkedIn() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.STUDENT);
        Assert.assertTrue(page.isLinkedInFieldVisible(), "LinkedIn field should be visible");
        Assert.assertTrue(page.isSharePlatformNoteVisible(), "Share-platform note should be visible");
        Assert.assertFalse(page.isOrganizationSelectorVisible(), "No Organization selector should exist for Student");
        Assert.assertFalse(page.isProfessionSelectorVisible(), "No Profession selector should exist for Student");
        Assert.assertFalse(page.isPreviousOrganizationSelectorVisible(),
                "No Previous organization selector should exist for Student");
        Assert.assertFalse(page.isContinueEnabled(), "'Continue' should be disabled before LinkedIn is filled");
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 69,
            description = "Student: Continue requires LinkedIn — the only field for this option")
    public void testStudentContinueRequiresLinkedIn() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.STUDENT);
        Assert.assertFalse(page.isContinueEnabled(), "Precondition: LinkedIn empty");
        page.enterLinkedIn("https://www.linkedin.com/in/testuser");
        Assert.assertTrue(page.isContinueEnabled(),
                "'Continue' should enable once LinkedIn is filled (the only field for Student)");
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 70,
            description = "Organization search modal loads, lists results and narrows by search text")
    public void testOrganizationSearchNarrowsResults() {
        ProfessionSearchPage search = gotoProfessionScreen()
                .selectProfession(ProfessionPage.EMPLOYED)
                .openOrganizationSearch();
        Assert.assertTrue(search.isLoaded(ProfessionSearchPage.ORGANIZATION_HEADING),
                "'" + ProfessionSearchPage.ORGANIZATION_HEADING + "' heading should be visible");
        Assert.assertTrue(search.isCloseButtonVisible(), "Close button should be visible");
        Assert.assertTrue(search.isSearchBoxVisible(), "Search box should be visible");
        Assert.assertFalse(search.getListedOptionNames().isEmpty(), "Organizations should be listed by default");

        List<String> results = search.search(SAMPLE_ORGANIZATION_QUERY).getListedOptionNames();
        Assert.assertTrue(results.stream().anyMatch(r -> r.startsWith(SAMPLE_ORGANIZATION)),
                "Searching '" + SAMPLE_ORGANIZATION_QUERY + "' should list '" + SAMPLE_ORGANIZATION
                        + "', saw: " + results);
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 71,
            description = "Designation search modal loads, lists results and narrows by search text")
    public void testDesignationSearchNarrowsResults() {
        ProfessionSearchPage search = gotoProfessionScreen()
                .selectProfession(ProfessionPage.EMPLOYED)
                .openDesignationSearch();
        Assert.assertTrue(search.isLoaded(ProfessionSearchPage.DESIGNATION_HEADING),
                "'" + ProfessionSearchPage.DESIGNATION_HEADING + "' heading should be visible");
        Assert.assertFalse(search.getListedOptionNames().isEmpty(), "Designations should be listed by default");

        List<String> results = search.search(SAMPLE_DESIGNATION_QUERY).getListedOptionNames();
        Assert.assertTrue(results.stream().anyMatch(r -> r.startsWith(SAMPLE_DESIGNATION_QUERY)),
                "Searching '" + SAMPLE_DESIGNATION_QUERY + "' should list matching designations, saw: " + results);
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 72,
            description = "Profession search modal's heading is plain 'Search', unlike Organization/Designation's field-specific headings")
    public void testProfessionSearchModalHasGenericHeading() {
        ProfessionSearchPage search = gotoProfessionScreen()
                .selectProfession(ProfessionPage.SELF_EMPLOYED)
                .openProfessionSearch();
        Assert.assertTrue(search.isLoaded(ProfessionSearchPage.PROFESSION_HEADING),
                "Profession search modal heading should be plain '" + ProfessionSearchPage.PROFESSION_HEADING + "'");

        List<String> results = search.search(SAMPLE_PROFESSION_QUERY).getListedOptionNames();
        Assert.assertTrue(results.stream().anyMatch(r -> r.startsWith(SAMPLE_PROFESSION_ROW)),
                "Searching '" + SAMPLE_PROFESSION_QUERY + "' should list '" + SAMPLE_PROFESSION_ROW
                        + "', saw: " + results);
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 73,
            description = "LinkedIn field applies NO format validation — Continue's state is unaffected by its content")
    public void testLinkedInAcceptsAnyTextNoFormatValidation() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.EMPLOYED);
        page.openOrganizationSearch().search(SAMPLE_ORGANIZATION_QUERY).selectOption(SAMPLE_ORGANIZATION);
        page.openDesignationSearch().search(SAMPLE_DESIGNATION_QUERY).selectOption(SAMPLE_DESIGNATION_EXACT_ROW);
        Assert.assertTrue(page.isContinueEnabled(),
                "Precondition: Continue already enabled with Organization+Designation, LinkedIn empty");

        for (String value : new String[]{
                "not a link at all",
                "just some random words here",
                "linkedin.com/in/testuser",
                "https://www.linkedin.com/in/testuser"}) {
            page.enterLinkedIn(value);
            Assert.assertTrue(page.isContinueEnabled(),
                    "'Continue' should stay enabled regardless of LinkedIn format — no validation is applied "
                    + "(value: '" + value + "')");
        }
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 74,
            description = "Selecting 'Other' for Organization reveals a free-text field, distinct from the row's own label")
    public void testSelectingOtherForOrganizationRevealsFreeTextField() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.EMPLOYED);
        ProfessionSearchPage search = page.openOrganizationSearch().search("zzzznotarealorganization");
        Assert.assertTrue(search.isOtherOptionVisible(),
                "'Other Not Listed Above' should be listed even with no real matches");
        search.selectOther();

        Assert.assertTrue(page.isOptionSelected(ProfessionSearchPage.OTHER_SELECTED_LABEL),
                "Organization selector should now show '" + ProfessionSearchPage.OTHER_SELECTED_LABEL + "'");
        Assert.assertTrue(page.isOrganizationOtherFieldVisible(),
                "Selecting 'Other' should reveal the '" + ProfessionPage.ORGANIZATION_OTHER_HINT + "' field");
        Assert.assertFalse(page.isContinueEnabled(),
                "'Continue' should stay disabled while the 'Other' organization field is still empty");

        page.enterOrganizationOtherText("My Custom Organization Pvt Ltd");
        page.openDesignationSearch().search(SAMPLE_DESIGNATION_QUERY).selectOption(SAMPLE_DESIGNATION_EXACT_ROW);
        Assert.assertTrue(page.isContinueEnabled(),
                "'Continue' should enable once the 'Other' organization text and Designation are both resolved");
    }

    @ProfessionScreenTests
    @Test(groups = {"OnboardingPageTests", "ProfessionScreenTests"}, priority = 75,
            description = "Selecting 'Other' for Designation reveals a free-text field, distinct from the row's own label")
    public void testSelectingOtherForDesignationRevealsFreeTextField() {
        ProfessionPage page = gotoProfessionScreen().selectProfession(ProfessionPage.EMPLOYED);
        ProfessionSearchPage search = page.openDesignationSearch().search("zzzznotarealdesignation");
        Assert.assertTrue(search.isOtherOptionVisible(),
                "'Other Not Listed Above' should be listed even with no real matches");
        search.selectOther();

        Assert.assertTrue(page.isOptionSelected(ProfessionSearchPage.OTHER_SELECTED_LABEL),
                "Designation selector should now show '" + ProfessionSearchPage.OTHER_SELECTED_LABEL + "'");
        Assert.assertTrue(page.isDesignationOtherFieldVisible(),
                "Selecting 'Other' should reveal the '" + ProfessionPage.DESIGNATION_OTHER_HINT + "' field");
        Assert.assertFalse(page.isContinueEnabled(),
                "'Continue' should stay disabled while the 'Other' designation field is still empty");

        page.openOrganizationSearch().search(SAMPLE_ORGANIZATION_QUERY).selectOption(SAMPLE_ORGANIZATION);
        Assert.assertFalse(page.isContinueEnabled(),
                "Precondition: 'Other' designation field still empty even with Organization resolved");
        page.enterDesignationOtherText("Chief Something Officer");
        Assert.assertTrue(page.isContinueEnabled(),
                "'Continue' should enable once the 'Other' designation text and Organization are both resolved");
    }

    // =====================================================================
    // Full login flow (originates from LoggedInBaseTest)
    // =====================================================================

    @LoggedInBaseTests
    @Test(groups = {"OnboardingPageTests", "LoggedInBaseTests"}, priority = 90,
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
