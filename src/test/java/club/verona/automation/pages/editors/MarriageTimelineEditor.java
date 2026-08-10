package club.verona.automation.pages.editors;

import club.verona.automation.pages.BasePage;
import io.appium.java_client.AppiumDriver;

import java.util.List;

/**
 * "Looking to get married" modal: fixed option list + 'Save changes'.
 * Options observed live: "Less than a year", "1 to 2 years", "In 3 years".
 * The profile row afterwards shows e.g. "In 1 to 2 years" for option
 * "1 to 2 years" — assert with contains(), not equals.
 *
 * All interactions are page-source/coordinate based (UiSnapshot).
 */
public class MarriageTimelineEditor extends BasePage {

    private static final String HEADER = "Looking to get married in";
    private static final String SAVE_LABEL = "Save changes";

    public static final List<String> OPTIONS =
            List.of("Less than a year", "1 to 2 years", "In 3 years");

    public MarriageTimelineEditor(AppiumDriver driver) {
        super(driver);
        UiSnapshot.waitFor(driver, s -> s.containsText(HEADER),
                15_000, "marriage timeline modal");
    }

    public MarriageTimelineEditor selectOption(String option) {
        clickByDesc(option);
        UiSnapshot.sleep(800);
        return this;
    }

    public void save() {
        clickByDesc(SAVE_LABEL);
        UiSnapshot.waitFor(driver, s -> !s.containsText(HEADER),
                15_000, "marriage modal to close");
    }

    /**
     * Maps a profile-row value back to its option label. The row may be
     * lowercased and "In "-prefixed (e.g. option "Less than a year" shows as
     * "In less than a year"), so matching is case-insensitive.
     */
    public static String optionForRowValue(String rowValue) {
        return OPTIONS.stream()
                .filter(o -> rowValue.equalsIgnoreCase(o) || rowValue.equalsIgnoreCase("In " + o))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No option for: " + rowValue));
    }

    /** Picks any option different from the given row value. */
    public static String differentOptionThan(String rowValue) {
        String current = optionForRowValue(rowValue);
        return OPTIONS.stream().filter(o -> !o.equals(current)).findFirst().orElseThrow();
    }
}
