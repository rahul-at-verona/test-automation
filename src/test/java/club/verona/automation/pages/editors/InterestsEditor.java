package club.verona.automation.pages.editors;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * "Interests" bottom-sheet editor (opened from the Interests section on the
 * profile Edit tab). A grid of interest chips (content-desc = interest name),
 * a "Highlight between 3 to 8 interests" constraint, and a "Save changes"
 * button that ENABLES only once the selection changes.
 *
 * As with the Ideal Married Life editor, chip selection is shown by fill colour
 * only (not in the accessibility tree), so the Save enabled-state is the
 * reliable functional signal. Chips respond to standard element clicks.
 */
public class InterestsEditor extends BasePage {

    public static final String HEADER = "Interests";
    public static final String CONSTRAINT = "Highlight between 3 to 8 interests";
    public static final String SAVE = "Save changes";

    public static final By HEADER_LOC = AppiumBy.xpath(
            "//android.widget.TextView[@text='" + HEADER + "']");
    public static final By CONSTRAINT_LOC = AppiumBy.xpath(
            "//android.widget.TextView[@text='" + CONSTRAINT + "']");
    public static final By SAVE_LOC = AppiumBy.accessibilityId(SAVE);

    public InterestsEditor(AppiumDriver driver) {
        super(driver);
        // Use the constraint text (unique to the editor) as the load signal:
        // the header "Interests" also exists as the profile section label.
        newWait().until(ExpectedConditions.visibilityOfElementLocated(CONSTRAINT_LOC));
    }

    public boolean isLoaded() {
        return !driver.findElements(CONSTRAINT_LOC).isEmpty();
    }

    /** True if the "Highlight between 3 to 8 interests" constraint is shown. */
    public boolean isConstraintDisplayed() {
        return !driver.findElements(CONSTRAINT_LOC).isEmpty();
    }

    /** True once a change has been made (Save button becomes clickable). */
    public boolean isSaveEnabled() {
        return "true".equals(driver.findElement(SAVE_LOC).getAttribute("clickable"));
    }

    /** Toggles an interest chip by its label (e.g. "Pets"). */
    public InterestsEditor toggleInterest(String interest) {
        click(AppiumBy.accessibilityId(interest));
        pause(800);
        return this;
    }

    /** Leaves without persisting (back), returning to the profile Edit tab. */
    public void cancel() {
        driver.navigate().back();
        // Wait on the constraint text (unique to the editor); the "Interests"
        // header text also exists on the profile behind the sheet.
        newWait().until(ExpectedConditions.invisibilityOfElementLocated(CONSTRAINT_LOC));
    }
}
