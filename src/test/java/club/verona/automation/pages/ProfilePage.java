package club.verona.automation.pages;

import club.verona.automation.pages.editors.UiSnapshot;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Page Object for Verona "My Profile" screen (Edit tab).
 *
 * Every locator below was extracted from a live UiAutomator2 page-source dump
 * of the running app (React Native; almost no testIDs/resource-ids, so
 * locators are anchored on stable text and content-desc values).
 *
 * NOTE: React Native keeps the previous screen (RouteScreen: 3, the Settings
 * screen, which also contains a "My Profile" card) in the view tree behind
 * this screen. Locators that could collide are therefore scoped with
 * not(ancestor::*[starts-with(@resource-id,'RouteScreen')]).
 */
public class ProfilePage extends BasePage {

    private static final String NOT_IN_ROUTESCREEN =
            "not(ancestor::*[starts-with(@resource-id,'RouteScreen')])";

    // ---------- Header ----------
    public static final By HEADER_TITLE = AppiumBy.xpath(
            "//android.widget.TextView[@text='My Profile' and " + NOT_IN_ROUTESCREEN + "]");

    // Back arrow: unlabeled clickable ViewGroup left of the title
    public static final By BACK_BUTTON = AppiumBy.xpath(
            "//android.widget.TextView[@text='My Profile' and " + NOT_IN_ROUTESCREEN + "]"
            + "/preceding-sibling::android.view.ViewGroup[@clickable='true']");

    // Profile completion badge, e.g. content-desc="92%"
    public static final By COMPLETION_BADGE = AppiumBy.xpath(
            "//android.view.ViewGroup[@clickable='true' and contains(@content-desc,'%')]");

    // ---------- Tabs ----------
    public static final By EDIT_TAB = AppiumBy.accessibilityId("Edit");
    public static final By VIEW_TAB = AppiumBy.accessibilityId("View");

    // ---------- Photo grid ----------
    // Empty "+" slots (3 on current state)
    public static final By ADD_PHOTO_SLOTS = AppiumBy.xpath(
            "//android.widget.TextView[@text='Tap to edit. Drag to reorder']"
            + "/preceding-sibling::android.view.ViewGroup[@clickable='true']");

    // "X" remove buttons on uploaded photos (3 on current state)
    public static final By REMOVE_PHOTO_BUTTONS = AppiumBy.xpath(
            "//android.widget.TextView[@text='Tap to edit. Drag to reorder']"
            + "/preceding-sibling::android.view.ViewGroup[not(@clickable='true')]"
            + "//android.view.ViewGroup[@clickable='true']");

    public static final By PHOTO_HINT = AppiumBy.xpath(
            "//android.widget.TextView[@text='Tap to edit. Drag to reorder']");

    // ---------- Identity block ----------
    // "Rahul Garg, 25" — first TextView after the photo hint
    public static final By NAME_TEXT = AppiumBy.xpath(
            "//android.widget.TextView[@text='Tap to edit. Drag to reorder']"
            + "/following-sibling::android.widget.TextView[1]");

    // (i) info icon next to the name
    public static final By INFO_ICON = AppiumBy.xpath(
            "//android.widget.TextView[@text='Tap to edit. Drag to reorder']"
            + "/following-sibling::android.view.ViewGroup[@clickable='true'][1]");

    public static final By VERIFIED_BADGE = AppiumBy.accessibilityId("Verified");

    public static final By SHOW_INITIALS_LABEL = AppiumBy.xpath(
            "//android.widget.TextView[@text='Show initials only']");

    // Toggle switch right of the "Show initials only" label
    public static final By SHOW_INITIALS_TOGGLE = AppiumBy.xpath(
            "//android.widget.TextView[@text='Show initials only']"
            + "/following-sibling::android.view.ViewGroup[@clickable='true'][1]");

    // ---------- Detail rows (content-desc = "<label>, <value>") ----------
    public static final By LOCATION_ROW = AppiumBy.xpath(
            "//android.view.ViewGroup[starts-with(@content-desc,'My location')]");
    public static final By OPEN_TO_LIVING_ROW = AppiumBy.xpath(
            "//android.view.ViewGroup[starts-with(@content-desc,'Open to living in')]");
    public static final By MARRIAGE_TIMELINE_ROW = AppiumBy.xpath(
            "//android.view.ViewGroup[starts-with(@content-desc,'Looking to get married')]");

    // ---------- Credentials section ----------
    public static final By CREDENTIALS_HEADER = AppiumBy.xpath(
            "//android.widget.TextView[@text='My Credentials']");
    public static final By EDUCATION_HEADER = AppiumBy.xpath(
            "//android.widget.TextView[@text='Education']");
    public static final By EDUCATION_ROWS = AppiumBy.xpath(
            "//android.widget.TextView[@text='Education']"
            + "/following-sibling::android.view.ViewGroup[@clickable='true']");

    // CHEAP anchors for screen detection. HEADER_TITLE's ancestor:: predicate
    // is O(n^2) for the XPath1 engine — fine on the small profile tree, but it
    // times out (60s+) on the huge Home-feed tree. Never use it before knowing
    // we're on the profile page; use these instead.
    public static final By PROFILE_ANCHOR = AppiumBy.xpath(
            "//android.widget.TextView[@text='Tap to edit. Drag to reorder']");
    public static final By NAV_BAR = AppiumBy.xpath(
            "//*[@resource-id='bottom-navigation-bar-content']");

    // ---------- Navigation (app opens on Home after a fresh launch) ----------
    // Bottom nav has testIDs; tabs are 4 unlabeled clickable Views.
    // Order: Home, Likes, Chats, Settings.
    public static final By NAV_SETTINGS_TAB = AppiumBy.xpath(
            "(//*[@resource-id='bottom-navigation-bar-content']"
            + "//android.view.View[@clickable='true'])[4]");

    // "My Profile" card on the Settings screen
    public static final By SETTINGS_MY_PROFILE_CARD = AppiumBy.xpath(
            "//android.view.ViewGroup[starts-with(@content-desc,'My Profile,')]");

    // =====================================================================

    private final WebDriverWait wait;

    public ProfilePage(AppiumDriver driver) {
        super(driver);
        // The app animates constantly; elements go stale mid-wait. Ignore
        // staleness so ExpectedConditions keep polling instead of aborting.
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        this.wait.ignoring(org.openqa.selenium.StaleElementReferenceException.class);
        this.wait.pollingEvery(Duration.ofSeconds(2)); // gentle on the instrumentation
    }

    /**
     * Navigation from a fresh app launch (or straight after login):
     * Home tab -> Settings tab -> My Profile card -> Edit tab.
     *
     * The Settings tab is opened with a BLIND coordinate tap, on purpose: the
     * Home feed animates continuously, and issuing any accessibility query
     * against it (as element click() does, to find + wait for the tab) hangs
     * the UiAutomator2 instrumentation. Tapping by coordinates never queries
     * Home. Once Settings is foregrounded every screen is static, so standard
     * click() is used for the card and the Edit tab.
     */
    public ProfilePage navigateFromColdStart() {
        pause(12_000); // let the RN bundle / Home settle after launch or login
        openSettingsTabBlind();
        wait.until(ExpectedConditions.presenceOfElementLocated(SETTINGS_MY_PROFILE_CARD));
        click(SETTINGS_MY_PROFILE_CARD);
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(PROFILE_ANCHOR));
        } catch (org.openqa.selenium.TimeoutException e) {
            openEditTab(); // opened on the View tab
            wait.until(ExpectedConditions.presenceOfElementLocated(PROFILE_ANCHOR));
        }
        return this;
    }

    /** Blind-taps the Settings tab (4th of 4) by proportional coordinates. */
    private void openSettingsTabBlind() {
        org.openqa.selenium.Dimension size = driver.manage().window().getSize();
        int x = (int) (size.width * 0.8616);   // centre of the 4th nav tab
        int y = (int) (size.height * 0.9464);
        for (int attempt = 0; attempt < 3; attempt++) {
            tapByCoordinates(x, y);
            pause(3_000);
            if (!driver.findElements(SETTINGS_MY_PROFILE_CARD).isEmpty()) {
                return; // Settings screen is up
            }
        }
    }


    /**
     * Recovery: if a previous test died inside an editor/modal, back out
     * until the profile page (or a main-tab screen) is visible, then navigate.
     */
    public ProfilePage ensureOnProfile() {
        // Back out of editors/modals until a known anchor appears
        for (int i = 0; i < 4 && onUnknownScreen(); i++) {
            driver.navigate().back();
            pause(1_500);
        }
        if (!driver.findElements(PROFILE_ANCHOR).isEmpty()) {
            return this; // already on the profile Edit tab
        }
        if (!driver.findElements(SETTINGS_MY_PROFILE_CARD).isEmpty()) {
            click(SETTINGS_MY_PROFILE_CARD); // on the Settings screen
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(PROFILE_ANCHOR));
            } catch (org.openqa.selenium.TimeoutException e) {
                openEditTab();
                wait.until(ExpectedConditions.presenceOfElementLocated(PROFILE_ANCHOR));
            }
            return this;
        }
        // Unknown state (possibly the poisonous Home tab) — go the blind route
        return navigateFromColdStart();
    }

    private boolean onUnknownScreen() {
        return driver.findElements(PROFILE_ANCHOR).isEmpty()
                && driver.findElements(SETTINGS_MY_PROFILE_CARD).isEmpty();
    }



    /**
     * Navigates Home -> Settings tab -> My Profile card -> Edit tab.
     * Safe to call from any main-tab screen; no-op if already on the profile page.
     */
    public ProfilePage navigateTo() {
        if (!driver.findElements(PROFILE_ANCHOR).isEmpty()) {
            return this; // already on the profile Edit tab
        }
        click(NAV_SETTINGS_TAB);
        click(SETTINGS_MY_PROFILE_CARD);
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(PROFILE_ANCHOR));
        } catch (org.openqa.selenium.TimeoutException e) {
            openEditTab(); // profile opened on the View tab
            wait.until(ExpectedConditions.presenceOfElementLocated(PROFILE_ANCHOR));
        }
        return this;
    }

    public boolean isLoaded() {
        // PROFILE_ANCHOR is cheap; HEADER_TITLE's ancestor:: predicate can be
        // slow on large transitional trees right after an editor closes.
        return wait.until(ExpectedConditions.presenceOfElementLocated(PROFILE_ANCHOR)) != null;
    }

    public String getName() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(NAME_TEXT)).getText();
    }

    public String getCompletionPercent() {
        // content-desc like "92%"
        return driver.findElement(COMPLETION_BADGE).getAttribute("content-desc");
    }

    public boolean isVerified() {
        return !driver.findElements(VERIFIED_BADGE).isEmpty();
    }

    /**
     * True if the identity block shows any verification status. A fresh account
     * is "In review"; an approved one is "Verified".
     */
    public boolean hasVerificationStatus() {
        UiSnapshot snap = UiSnapshot.capture(driver);
        return snap.isDescDisplayed("Verified") || snap.isTextDisplayed("Verified")
                || snap.isDescDisplayed("In review") || snap.isTextDisplayed("In review");
    }

    public String getLocation() {
        // "My location, Farrukhabad, India" -> "Farrukhabad, India"
        String desc = driver.findElement(LOCATION_ROW).getAttribute("content-desc");
        return desc.replaceFirst("^My location, ", "");
    }

    public String getMarriageTimeline() {
        String desc = driver.findElement(MARRIAGE_TIMELINE_ROW).getAttribute("content-desc");
        return desc.replaceFirst("^Looking to get married, ", "");
    }

    public int getEmptyPhotoSlotCount() {
        return driver.findElements(ADD_PHOTO_SLOTS).size();
    }

    public int getUploadedPhotoCount() {
        return driver.findElements(REMOVE_PHOTO_BUTTONS).size();
    }

    public List<WebElement> getEducationEntries() {
        return driver.findElements(EDUCATION_ROWS);
    }

    public String getOpenToLivingSummary() {
        // full content-desc, e.g. "Open to living in, Chennai, Pune +4 more"
        return driver.findElement(OPEN_TO_LIVING_ROW).getAttribute("content-desc");
    }

    public String getMarriageTimelineRaw() {
        return driver.findElement(MARRIAGE_TIMELINE_ROW).getAttribute("content-desc")
                .replaceFirst("^Looking to get married, ", "");
    }

    // ---------- Actions ----------
    public void openViewTab()          { click(VIEW_TAB); }
    public void openEditTab()          { click(EDIT_TAB); }
    public void goBack()               { click(BACK_BUTTON); }
    public void openLocationEditor()   { click(LOCATION_ROW); }
    public void openLivingRow()        { click(OPEN_TO_LIVING_ROW); }
    public void openMarriageRow()      { click(MARRIAGE_TIMELINE_ROW); }

    // These two rows sit low on the Edit tab and their content-desc is the
    // (variable) selected value, so open them by scrolling their section label
    // into view and tapping the clickable row that follows it in document
    // order. (The XPath following:: axis is unusable under this app's engine.)

    /** Opens the "Ideal married life" editor (Post-marriage living + children). */
    public void openIdealMarriedLifeEditor() {
        scrollIntoViewByText("Post marriage living setup");
        tapClickableAfterLabel("Post marriage living setup");
    }

    /** Opens the "Interests" editor. */
    public void openInterestsEditor() {
        scrollIntoViewByText("Interests");
        tapClickableAfterLabel("Interests");
    }

    /** Coordinate-taps the first clickable node after the given label text. */
    private void tapClickableAfterLabel(String label) {
        java.util.List<UiSnapshot.Snap> all = snapshot().all();
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (label.equals(all.get(i).text)) { idx = i; break; }
        }
        if (idx < 0) {
            throw new IllegalStateException("Label not found on screen: " + label);
        }
        for (int i = idx + 1; i < all.size(); i++) {
            if (all.get(i).clickable) {
                tapByCoordinates(all.get(i));
                return;
            }
        }
        throw new IllegalStateException("No clickable row after label: " + label);
    }
    public void toggleShowInitials()   { click(SHOW_INITIALS_TOGGLE); }
    public void tapAddPhotoSlot(int i) { driver.findElements(ADD_PHOTO_SLOTS).get(i).click(); }

    /** True if a TextView with exactly this text is currently in the tree. */
    public boolean isTextVisible(String text) {
        return !driver.findElements(AppiumBy.xpath(
                "//android.widget.TextView[@text='" + text + "']")).isEmpty();
    }

    /** Waits for a TextView with exactly this text to appear. */
    public void waitForText(String text) {
        wait.until(ExpectedConditions.presenceOfElementLocated(AppiumBy.xpath(
                "//android.widget.TextView[@text='" + text + "']")));
    }


}
