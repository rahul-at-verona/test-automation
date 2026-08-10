package club.verona.automation.pages.editors;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * "Ideal married life" bottom-sheet editor (opened from the Ideal Married Life
 * section on the profile Edit tab). Two multi-select groups:
 *   - Post-marriage living setup: "Just me and my partner (nuclear family)",
 *     "With parents/in-laws", "Joint family"
 *   - Open to Children: "I'm open to having children someday",
 *     "I'm not open to having children"
 * Plus a "Save changes" button that ENABLES only once a selection changes.
 *
 * Selection state is not exposed in the accessibility tree (only conveyed by
 * fill colour), so the reliable functional signal is the Save button's enabled
 * state — verified live: toggling an option flips Save clickable, toggling back
 * disables it again. Options respond to standard element clicks.
 */
public class IdealMarriedLifeEditor extends BasePage {

    public static final String HEADER = "Ideal married life";
    public static final String SAVE = "Save changes";

    public static final By HEADER_LOC = AppiumBy.xpath(
            "//android.widget.TextView[@text='" + HEADER + "']");
    public static final By SAVE_LOC = AppiumBy.accessibilityId(SAVE);

    // Convenience constants for the options.
    public static final String NUCLEAR = "Just me and my partner (nuclear family)";
    public static final String WITH_PARENTS = "With parents/in-laws";
    public static final String JOINT_FAMILY = "Joint family";
    public static final String OPEN_TO_CHILDREN = "I'm open to having children someday";
    public static final String NOT_OPEN_TO_CHILDREN = "I'm not open to having children";

    public IdealMarriedLifeEditor(AppiumDriver driver) {
        super(driver);
        newWait().until(ExpectedConditions.visibilityOfElementLocated(HEADER_LOC));
    }

    public boolean isLoaded() {
        return !driver.findElements(HEADER_LOC).isEmpty();
    }

    /** True once a change has been made (Save button becomes clickable). */
    public boolean isSaveEnabled() {
        return "true".equals(driver.findElement(SAVE_LOC).getAttribute("clickable"));
    }

    /** Toggles a living-setup / children option by its label. */
    public IdealMarriedLifeEditor toggleOption(String optionLabel) {
        click(AppiumBy.accessibilityId(optionLabel));
        pause(800);
        return this;
    }

    /** Leaves without persisting (back), returning to the profile Edit tab. */
    public void cancel() {
        driver.navigate().back();
        newWait().until(ExpectedConditions.invisibilityOfElementLocated(HEADER_LOC));
    }
}
