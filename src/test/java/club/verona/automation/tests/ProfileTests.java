package club.verona.automation.tests;

import club.verona.automation.annotations.ProfileFieldEditTests;
import club.verona.automation.annotations.ProfilePageTests;
import club.verona.automation.pages.PhotosSection;
import club.verona.automation.pages.ProfilePage;
import club.verona.automation.pages.editors.IdealMarriedLifeEditor;
import club.verona.automation.pages.editors.InterestsEditor;
import club.verona.automation.pages.editors.LivingPreferenceEditor;
import club.verona.automation.pages.editors.LocationEditor;
import club.verona.automation.pages.editors.MarriageTimelineEditor;
import io.appium.java_client.android.AndroidDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Locale;

/**
 * All "My Profile" tests in one class. Each test carries its origin marker
 * annotation and a matching TestNG group so a subset can be run, e.g.:
 *   mvn test -Dplatform=android -Dgroups=ProfileFieldEditTests
 *
 *   - ProfilePageTests      : read-only smoke checks of the profile screen
 *   - ProfileFieldEditTests : editing fields (search, select, toggle, restore)
 *
 * Extends {@link LoggedInBaseTest}: before the class, storage is cleared and a
 * fresh login (8799731416 / 123456) reaches Home; the profile is then opened.
 */
public class ProfileTests extends LoggedInBaseTest {

    private ProfilePage profilePage;

    @BeforeClass(alwaysRun = true)
    public void initPage() {
        try {
            profilePage = new ProfilePage(driver).navigateFromColdStart();
        } catch (Exception e) {
            profilePage = new ProfilePage(resetSession()).navigateFromColdStart();
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void recoverToProfile() {
        // A failed test may leave the app inside an editor — always restart
        // each test from the profile Edit tab; rebuild the session if wedged.
        try {
            profilePage.ensureOnProfile();
        } catch (org.openqa.selenium.WebDriverException e) {
            profilePage = new ProfilePage(resetSession()).navigateFromColdStart();
        }
    }

    // =====================================================================
    // ProfilePageTests — read-only smoke checks
    // =====================================================================

    @ProfilePageTests
    @Test(groups = {"ProfileTests", "ProfilePageTests"}, priority = 1,
            description = "Profile page loads with header title")
    public void testProfilePageIsLoaded() {
        Assert.assertTrue(profilePage.isLoaded(), "'My Profile' header should be visible");
    }

    @ProfilePageTests
    @Test(groups = {"ProfileTests", "ProfilePageTests"}, priority = 2,
            description = "Profile completion badge shows a percentage")
    public void testCompletionBadge() {
        String percent = profilePage.getCompletionPercent();
        Assert.assertTrue(percent.matches("\\d{1,3}%"),
                "Completion badge should show N%, but was: " + percent);
    }

    @ProfilePageTests
    @Test(groups = {"ProfileTests", "ProfilePageTests"}, priority = 3,
            description = "User name and age are displayed")
    public void testNameIsDisplayed() {
        String name = profilePage.getName();
        Assert.assertTrue(name.matches(".+, \\d{2}"),
                "Name should be '<Name>, <Age>', but was: " + name);
    }

    @ProfilePageTests
    @Test(groups = {"ProfileTests", "ProfilePageTests"}, priority = 4,
            description = "Profile shows a verification status (Verified or In review)")
    public void testVerificationStatus() {
        Assert.assertTrue(profilePage.hasVerificationStatus(),
                "Profile should show a verification status (Verified or In review)");
    }

    @ProfilePageTests
    @Test(groups = {"ProfileTests", "ProfilePageTests"}, priority = 5,
            description = "Location row shows a value")
    public void testLocationRow() {
        Assert.assertFalse(profilePage.getLocation().isBlank(), "Location value should not be empty");
    }

    @ProfilePageTests
    @Test(groups = {"ProfileTests", "ProfilePageTests"}, priority = 6,
            description = "Marriage timeline row shows a value")
    public void testMarriageTimelineRow() {
        Assert.assertFalse(profilePage.getMarriageTimeline().isBlank(),
                "Marriage timeline value should not be empty");
    }

    @ProfilePageTests
    @Test(groups = {"ProfileTests", "ProfilePageTests"}, priority = 7,
            description = "Photo grid: 6 slots total (uploaded + empty)")
    public void testPhotoGrid() {
        int uploaded = profilePage.getUploadedPhotoCount();
        int empty = profilePage.getEmptyPhotoSlotCount();
        Assert.assertEquals(uploaded + empty, 6,
                "Photo grid should have 6 slots (uploaded=" + uploaded + ", empty=" + empty + ")");
    }

    @ProfilePageTests
    @Test(groups = {"ProfileTests", "ProfilePageTests"}, priority = 8,
            description = "At least one education credential is listed")
    public void testEducationCredentials() {
        Assert.assertFalse(profilePage.getEducationEntries().isEmpty(),
                "Education section should list at least one entry");
    }

    @ProfilePageTests
    @Test(groups = {"ProfileTests", "ProfilePageTests"}, priority = 9,
            description = "Switch to View tab and back to Edit")
    public void testTabSwitch() {
        profilePage.openViewTab();
        profilePage.openEditTab();
        Assert.assertTrue(profilePage.isLoaded(), "Should return to Edit tab");
    }

    // =====================================================================
    // ProfileFieldEditTests — editing fields
    // =====================================================================

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 11,
            description = "Location search filters suggestions by partial text")
    public void testLocationPartialSearch() {
        profilePage.openLocationEditor();
        LocationEditor editor = new LocationEditor(driver);

        int unfiltered = editor.getSuggestions().size();
        List<String> results = editor.search("chen").getSuggestions();

        Assert.assertFalse(results.isEmpty(), "'chen' should return suggestions");
        Assert.assertTrue(results.size() < unfiltered,
                "Filtered list should be smaller than unfiltered (" + unfiltered + ")");
        for (String city : results) {
            Assert.assertTrue(city.toLowerCase(Locale.ROOT).contains("chen"),
                    "Suggestion '" + city + "' does not match partial 'chen'");
        }
        driver.navigate().back();
        Assert.assertTrue(profilePage.isLoaded(), "Should be back on profile page");
    }

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 12,
            description = "Edit location, verify on Edit + View tabs, then restore")
    public void testEditLocationVerifyOnBothTabsAndRestore() {
        String originalCity = profilePage.getLocation().split(",")[0].trim();
        String targetCity = originalCity.equals("Chennai") ? "Mumbai" : "Chennai";

        changeLocationTo(targetCity);
        Assert.assertTrue(profilePage.getLocation().startsWith(targetCity),
                "Edit tab row should show " + targetCity + " but shows: " + profilePage.getLocation());

        profilePage.openViewTab();
        profilePage.waitForText(targetCity + ", India");
        Assert.assertTrue(profilePage.isTextVisible(targetCity + ", India"),
                "View tab should show '" + targetCity + ", India'");
        profilePage.openEditTab();

        changeLocationTo(originalCity);
        Assert.assertTrue(profilePage.getLocation().startsWith(originalCity),
                "Location should be restored to " + originalCity);
    }

    private void changeLocationTo(String city) {
        profilePage.openLocationEditor();
        new LocationEditor(driver)
                .search(city.substring(0, 4).toLowerCase(Locale.ROOT))
                .selectCity(city)
                .save();
        Assert.assertTrue(profilePage.isLoaded(), "Should return to profile after save");
    }

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 13,
            description = "Country search filters by partial text")
    public void testCountryPartialSearch() {
        profilePage.openLivingRow();
        LivingPreferenceEditor editor = new LivingPreferenceEditor(driver);

        List<String> results = editor.search("ind").getSuggestionNames();
        Assert.assertFalse(results.isEmpty(), "'ind' should return countries");
        for (String country : results) {
            Assert.assertTrue(country.toLowerCase(Locale.ROOT).contains("ind"),
                    "Country '" + country + "' does not match partial 'ind'");
        }
        Assert.assertTrue(results.contains("India"), "India should match 'ind'");

        driver.navigate().back();
        Assert.assertTrue(profilePage.isLoaded(), "Should be back on profile page");
    }

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 14,
            description = "City search inside a country filters by partial text")
    public void testCityPartialSearchInsideCountry() {
        profilePage.openLivingRow();
        LivingPreferenceEditor editor = new LivingPreferenceEditor(driver);

        List<String> cities = editor.openCountry("India").search("pun").getSuggestionNames();
        Assert.assertFalse(cities.isEmpty(), "'pun' should return cities in India");
        for (String city : cities) {
            Assert.assertTrue(city.toLowerCase(Locale.ROOT).contains("pun"),
                    "City '" + city + "' does not match partial 'pun'");
        }

        driver.navigate().back();
        driver.navigate().back();
        Assert.assertTrue(profilePage.isLoaded(), "Should be back on profile page");
    }

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 15,
            description = "Toggle a city's checkbox on/off inside a country and revert")
    public void testToggleLivingCityAndRevert() {
        profilePage.openLivingRow();
        LivingPreferenceEditor editor = new LivingPreferenceEditor(driver)
                .openCountry("India")
                .search("pun");

        boolean initial = editor.isCitySelected("Pune");

        editor.toggleCity("Pune");
        Assert.assertNotEquals(editor.isCitySelected("Pune"), initial,
                "Pune's checkbox state should flip after one tap");

        editor.toggleCity("Pune");
        Assert.assertEquals(editor.isCitySelected("Pune"), initial,
                "Pune's checkbox should return to its original state");

        editor.cancel();
        Assert.assertTrue(profilePage.isLoaded(), "Should be back on profile page");
    }

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 16,
            description = "Change marriage timeline, verify row, restore original")
    public void testEditMarriageTimelineAndRestore() {
        String original = profilePage.getMarriageTimelineRaw();
        String target = MarriageTimelineEditor.differentOptionThan(original);

        profilePage.openMarriageRow();
        new MarriageTimelineEditor(driver).selectOption(target).save();

        String updated = profilePage.getMarriageTimelineRaw();
        String expected = target.replaceFirst("(?i)^In ", "").toLowerCase(Locale.ROOT);
        Assert.assertTrue(updated.toLowerCase(Locale.ROOT).contains(expected),
                "Row should reflect '" + target + "' but shows: " + updated);

        profilePage.openMarriageRow();
        new MarriageTimelineEditor(driver)
                .selectOption(MarriageTimelineEditor.optionForRowValue(original))
                .save();
        Assert.assertEquals(profilePage.getMarriageTimelineRaw(), original,
                "Marriage timeline should be restored");
    }

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 17,
            description = "Ideal Married Life: toggling an option enables Save; reverting disables it")
    public void testIdealMarriedLifeToggleAndRevert() {
        profilePage.openIdealMarriedLifeEditor();
        IdealMarriedLifeEditor editor = new IdealMarriedLifeEditor(driver);

        Assert.assertTrue(editor.isLoaded(), "'Ideal married life' editor should open");
        Assert.assertFalse(editor.isSaveEnabled(),
                "'Save changes' should be disabled before any change");

        editor.toggleOption(IdealMarriedLifeEditor.JOINT_FAMILY);
        Assert.assertTrue(editor.isSaveEnabled(),
                "'Save changes' should enable after toggling a living-setup option");

        editor.toggleOption(IdealMarriedLifeEditor.JOINT_FAMILY); // revert
        Assert.assertFalse(editor.isSaveEnabled(),
                "'Save changes' should disable again once the change is reverted");

        editor.cancel(); // leave without persisting
        // The profile is scrolled to this section now, so assert the editor is
        // dismissed (scroll-independent) rather than the top-of-page anchor.
        Assert.assertFalse(editor.isLoaded(), "Editor should be dismissed after cancel");
    }

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 18,
            description = "Interests: shows the 3–8 constraint; toggling a chip enables Save; reverting disables it")
    public void testInterestsToggleAndRevert() {
        profilePage.openInterestsEditor();
        InterestsEditor editor = new InterestsEditor(driver);

        Assert.assertTrue(editor.isLoaded(), "'Interests' editor should open");
        Assert.assertTrue(editor.isConstraintDisplayed(),
                "'" + InterestsEditor.CONSTRAINT + "' hint should be displayed");
        Assert.assertFalse(editor.isSaveEnabled(),
                "'Save changes' should be disabled before any change");

        editor.toggleInterest("Pets");
        Assert.assertTrue(editor.isSaveEnabled(),
                "'Save changes' should enable after toggling an interest chip");

        editor.toggleInterest("Pets"); // revert
        Assert.assertFalse(editor.isSaveEnabled(),
                "'Save changes' should disable again once the change is reverted");

        editor.cancel(); // leave without persisting
        // Profile is scrolled to this section; assert the editor is dismissed
        // (its unique constraint text is gone) rather than the top-page anchor.
        Assert.assertFalse(editor.isLoaded(), "Editor should be dismissed after cancel");
    }

    // =====================================================================
    // My Photos section
    // =====================================================================

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 19,
            description = "Photo section shows the '+N%' remaining that matches the per-photo weights")
    public void testPhotoSectionRemainingPercentMatchesWeights() {
        PhotosSection photos = new PhotosSection(driver);

        Assert.assertTrue(photos.isHeaderDisplayed(), "'My Photos' header should be visible");

        int remaining = photos.getRemainingPercent();
        int expected = photos.getExpectedRemainingPercent();
        Assert.assertEquals(remaining, expected,
                "'+" + remaining + "%' badge should equal the sum of empty-slot weights ("
                        + expected + "); filled photos = " + photos.getFilledPhotoCount());

        int overall = photos.getOverallPercent();
        Assert.assertTrue(overall > 0 && overall <= 100,
                "Overall completion % should be a sensible value, was: " + overall);
    }

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 20,
            description = "Deleting a first-3 photo offers Replace only (no Delete)")
    public void testFirstThreePhotoShowsReplaceOnly() {
        PhotosSection photos = new PhotosSection(driver);
        Assert.assertTrue(photos.getFilledPhotoCount() >= 1,
                "Test needs at least one photo in a first-3 slot");

        photos.openPhotoActions(1); // slot 1 is within the first 3
        Assert.assertTrue(photos.isReplaceOptionShown(),
                "'Replace' should be shown for a first-3 photo");
        Assert.assertFalse(photos.isDeleteOptionShown(),
                "'Delete' must NOT be shown for a first-3 photo");

        photos.dismissActions(); // no mutation
        Assert.assertTrue(photos.isHeaderDisplayed(), "Should be back on the photo section");
    }

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 21,
            description = "Replace CTA launches the OS photo-picker / media-permission flow")
    public void testReplaceCtaLaunchesPhotoPicker() {
        PhotosSection photos = new PhotosSection(driver);
        Assert.assertTrue(photos.getFilledPhotoCount() >= 1, "Test needs at least one photo");

        photos.openPhotoActions(1);
        photos.tapReplace();

        // Replace hands off to the OS: a media-permission dialog / photo picker
        // (a different package) comes to the foreground.
        boolean leftApp = false;
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (!"club.verona".equals(((AndroidDriver) driver).getCurrentPackage())) {
                leftApp = true;
                break;
            }
            try { Thread.sleep(500); } catch (InterruptedException ignored) { }
        }
        Assert.assertTrue(leftApp,
                "Tapping 'Replace' should launch the OS photo-picker / permission flow");

        // Return to the app without granting/selecting (non-destructive).
        driver.navigate().back();
    }

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 22,
            description = "Last-3 photo delete offers both Replace and Delete")
    public void testLastThreePhotoShowsReplaceAndDelete() {
        PhotosSection photos = new PhotosSection(driver);
        photos.ensureAtLeastPhotos(4); // guarantees a photo in a last-3 slot (4)

        photos.openPhotoActions(4); // slot 4 is within the last 3
        Assert.assertTrue(photos.isReplaceOptionShown(),
                "'Replace' should be shown for a last-3 photo");
        Assert.assertTrue(photos.isDeleteOptionShown(),
                "'Delete' should be shown for a last-3 photo");

        photos.dismissActions(); // don't delete here
    }

    @ProfileFieldEditTests
    @Test(groups = {"ProfileTests", "ProfileFieldEditTests"}, priority = 23,
            description = "Adding then deleting a photo updates overall and photo-section % by the photo's weight")
    public void testAddAndDeletePhotoUpdatePercentagesInRealTime() {
        PhotosSection photos = new PhotosSection(driver);
        photos.ensureAtLeastPhotos(4); // stable base so the added photo lands in a last-3 slot

        int filledBefore = photos.getFilledPhotoCount();
        Assert.assertTrue(filledBefore < PhotosSection.TOTAL_SLOTS,
                "Need at least one empty slot to add a photo");
        int overallBefore = photos.getOverallPercent();
        int remainingBefore = photos.getRemainingPercent();

        int newPos = filledBefore + 1;                  // slot the photo will occupy
        int weight = PhotosSection.PHOTO_WEIGHT.get(newPos);

        // --- add -> percentages update in real time ---
        photos.addPhoto();
        Assert.assertEquals(photos.getFilledPhotoCount(), filledBefore + 1,
                "Photo count should increase by 1 after adding");
        Assert.assertEquals(photos.getOverallPercent(), overallBefore + weight,
                "Overall % should rise by photo " + newPos + "'s weight (" + weight + ")");
        Assert.assertEquals(photos.getRemainingPercent(), remainingBefore - weight,
                "Photo-section remaining % should drop by the same weight");

        // --- delete the just-added (last-3) photo -> percentages revert ---
        photos.openPhotoActions(newPos);
        Assert.assertTrue(photos.isDeleteOptionShown(),
                "The newly added photo is in a last-3 slot and must be deletable");
        photos.tapDelete();

        Assert.assertEquals(photos.getFilledPhotoCount(), filledBefore,
                "Photo count should return to the original after delete");
        Assert.assertEquals(photos.getOverallPercent(), overallBefore,
                "Overall % should return to the original after delete");
        Assert.assertEquals(photos.getRemainingPercent(), remainingBefore,
                "Photo-section remaining % should return to the original after delete");
    }
}
