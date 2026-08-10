package club.verona.automation.pages;

import club.verona.automation.pages.editors.UiSnapshot;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * "My Photos" section at the top of the profile Edit tab.
 *
 * Layout (from live dump): a 2x3 grid of photo slots. Filled slots show the
 * image with a small circular "X" (delete cross) at the top-right; empty slots
 * show a "+". A "+ N%" badge shows the profile-completion percentage still
 * available from photos, and the header shows the overall completion %.
 *
 * Per-photo completion weights (photo 1 is mandatory, not counted here):
 *   photo 2 = 4%, photo 3 = 3%, photo 4 = 3%, photo 5 = 2%, photo 6 = 2%.
 * So the "+ N%" badge equals the sum of the weights of the EMPTY slots.
 *
 * Delete behaviour (from live dump): tapping the X opens a bottom sheet.
 *   - Photos in the FIRST 3 slots -> only a "Replace" action (no Delete).
 *   - Photos in the LAST 3 slots  -> "Replace" AND "Delete".
 * "Replace" launches the OS media-permission / photo-picker flow.
 */
public class PhotosSection extends BasePage {

    public static final String HEADER = "My Photos";

    /** Position (1-based) -> completion weight. Position 1 is mandatory (0). */
    public static final Map<Integer, Integer> PHOTO_WEIGHT = Map.of(
            1, 0, 2, 4, 3, 3, 4, 3, 5, 2, 6, 2);

    public static final int TOTAL_SLOTS = 6;

    public static final By HEADER_LOC = AppiumBy.xpath(
            "//android.widget.TextView[@text='" + HEADER + "']");
    public static final By REPLACE_LOC = AppiumBy.accessibilityId("Replace");
    public static final By DELETE_LOC = AppiumBy.accessibilityId("Delete");

    private static final Pattern REMAINING = Pattern.compile("\\+\\s*(\\d+)%");
    private static final Pattern BOUNDS = Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]");

    // The photo grid lives between the "My Photos" header and the
    // "Tap to edit. Drag to reorder" caption (~y 530..1520 on this device).
    private static final int GRID_TOP = 500;
    private static final int GRID_BOTTOM = 1500;
    private static final int CROSS_MAX_WIDTH = 130; // the X is small; tiles are ~400 wide

    public PhotosSection(AppiumDriver driver) {
        super(driver);
    }

    public boolean isHeaderDisplayed() {
        return !driver.findElements(HEADER_LOC).isEmpty();
    }

    /** Value of the "+ N%" photo-section remaining badge. */
    public int getRemainingPercent() {
        for (UiSnapshot.Snap n : snapshot().all()) {
            if (n.text != null) {
                Matcher m = REMAINING.matcher(n.text);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        }
        throw new IllegalStateException("'+ N%' photo remaining badge not found");
    }

    /** Overall profile-completion percent shown in the header (e.g. 90). */
    public int getOverallPercent() {
        String p = new ProfilePage(driver).getCompletionPercent(); // e.g. "90%"
        return Integer.parseInt(p.replace("%", "").trim());
    }

    /** Number of filled photos = number of delete crosses in the grid. */
    public int getFilledPhotoCount() {
        return deleteCrosses().size();
    }

    /** Expected remaining % = sum of weights of the empty slots. */
    public int getExpectedRemainingPercent() {
        int filled = getFilledPhotoCount();
        int sum = 0;
        for (int pos = filled + 1; pos <= TOTAL_SLOTS; pos++) {
            sum += PHOTO_WEIGHT.getOrDefault(pos, 0);
        }
        return sum;
    }

    /** Opens the delete/replace sheet for the photo at the given 1-based slot. */
    public PhotosSection openPhotoActions(int slot) {
        List<UiSnapshot.Snap> crosses = deleteCrosses();
        if (slot < 1 || slot > crosses.size()) {
            throw new IllegalArgumentException(
                    "No photo (delete cross) at slot " + slot + "; filled=" + crosses.size());
        }
        tapByCoordinates(crosses.get(slot - 1));
        pause(1_200);
        return this;
    }

    public boolean isReplaceOptionShown() {
        return !driver.findElements(REPLACE_LOC).isEmpty();
    }

    public boolean isDeleteOptionShown() {
        return !driver.findElements(DELETE_LOC).isEmpty();
    }

    /** Taps Replace (starts the OS media-permission / photo-picker flow). */
    public void tapReplace() {
        click(REPLACE_LOC);
    }

    /** Taps Delete (only present for last-3 photos); waits for the sheet to close. */
    public PhotosSection tapDelete() {
        click(DELETE_LOC);
        newWait().until(d -> driver.findElements(DELETE_LOC).isEmpty());
        pause(1_500); // allow the % badges to refresh
        return this;
    }

    /** Dismisses the delete/replace bottom sheet without acting. */
    public void dismissActions() {
        driver.navigate().back();
        newWait().until(d -> driver.findElements(REPLACE_LOC).isEmpty());
    }

    // ------------------------------------------------------------------
    // Adding photos via the OS photo picker
    // ------------------------------------------------------------------

    private static final By PICKER_IMAGE = AppiumBy.xpath(
            "//android.view.View[contains(@content-desc,'Photo taken on')]");
    private static final By ALLOW_ALL = AppiumBy.xpath("//*[@text='Allow all']");

    /** Adds photos until the profile has at least {@code target} of them. */
    public PhotosSection ensureAtLeastPhotos(int target) {
        int guard = 0;
        while (getFilledPhotoCount() < target && guard++ <= TOTAL_SLOTS) {
            addPhoto();
        }
        return this;
    }

    /**
     * Adds one photo through the OS flow: tap the next empty "+" slot ->
     * (grant media permission if prompted) -> pick the first image -> Done ->
     * crop "Save". Blocks until the app shows one more photo.
     */
    public PhotosSection addPhoto() {
        int before = getFilledPhotoCount();
        tapFirstEmptySlot();
        pause(2_500);
        grantMediaPermissionIfPrompted();
        click(PICKER_IMAGE);                 // select the first available image
        pause(1_000);
        tapProportional(0.85, 0.925);        // picker "Done"
        pause(2_500);
        tapProportional(0.84, 0.931);        // crop "Save"
        newWait().until(d -> "club.verona".equals(((AndroidDriver) d).getCurrentPackage()));
        waitForFilledCount(before + 1);
        pause(1_000);                        // let the % badges settle
        return this;
    }

    private void tapFirstEmptySlot() {
        List<UiSnapshot.Snap> slots = snapshot().all().stream()
                .filter(n -> n.clickable)
                .filter(this::isEmptySlotSized)
                .sorted(Comparator.comparingInt((UiSnapshot.Snap n) -> n.cy / 100)
                        .thenComparingInt(n -> n.cx))
                .collect(Collectors.toList());
        if (slots.isEmpty()) {
            throw new IllegalStateException("No empty photo slot available to add to");
        }
        tapByCoordinates(slots.get(0));
    }

    /** Empty "+" slots are ~400x400 clickable tiles (filled tiles are not). */
    private boolean isEmptySlotSized(UiSnapshot.Snap n) {
        Matcher m = BOUNDS.matcher(n.element.getAttribute("bounds"));
        if (!m.matches()) {
            return false;
        }
        int x1 = Integer.parseInt(m.group(1));
        int y1 = Integer.parseInt(m.group(2));
        int x2 = Integer.parseInt(m.group(3));
        int y2 = Integer.parseInt(m.group(4));
        int w = x2 - x1;
        int h = y2 - y1;
        return w >= 300 && w <= 520 && h >= 300 && h <= 520 && y1 >= GRID_TOP && y1 <= GRID_BOTTOM;
    }

    private void grantMediaPermissionIfPrompted() {
        for (int i = 0; i < 3; i++) {
            List<WebElement> allow = driver.findElements(ALLOW_ALL);
            if (!allow.isEmpty()) {
                allow.get(0).click();
                pause(2_000);
                return;
            }
            if (!driver.findElements(PICKER_IMAGE).isEmpty()) {
                return; // already in the picker
            }
            pause(1_000);
        }
    }

    private void tapProportional(double fx, double fy) {
        Dimension s = driver.manage().window().getSize();
        tapByCoordinates((int) (s.width * fx), (int) (s.height * fy));
    }

    private void waitForFilledCount(int expected) {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            try {
                if (getFilledPhotoCount() == expected) {
                    return;
                }
            } catch (Exception ignored) {
                // transient during upload/render
            }
            pause(1_000);
        }
        throw new IllegalStateException("Photo count did not reach " + expected);
    }

    // ------------------------------------------------------------------

    /** Small clickable nodes in the photo grid = the delete crosses, row-major. */
    private List<UiSnapshot.Snap> deleteCrosses() {
        return snapshot().all().stream()
                .filter(n -> n.clickable)
                .filter(this::isCrossSized)
                .sorted(Comparator.comparingInt((UiSnapshot.Snap n) -> n.cy / 100)
                        .thenComparingInt(n -> n.cx))
                .collect(Collectors.toList());
    }

    private boolean isCrossSized(UiSnapshot.Snap n) {
        Matcher m = BOUNDS.matcher(n.element.getAttribute("bounds"));
        if (!m.matches()) {
            return false;
        }
        int x1 = Integer.parseInt(m.group(1));
        int y1 = Integer.parseInt(m.group(2));
        int x2 = Integer.parseInt(m.group(3));
        int width = x2 - x1;
        return width > 0 && width <= CROSS_MAX_WIDTH && y1 >= GRID_TOP && y1 <= GRID_BOTTOM;
    }
}
