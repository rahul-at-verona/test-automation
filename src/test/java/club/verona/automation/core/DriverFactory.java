package club.verona.automation.core;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

/**
 * Creates the AppiumDriver for the platform passed via -Dplatform=android|ios.
 * Usage: mvn test -Dplatform=android
 */
public final class DriverFactory {

    private DriverFactory() {}

    public static AppiumDriver create() {
        String platform = System.getProperty("platform", "android").toLowerCase();
        URL serverUrl = serverUrl();

        return switch (platform) {
            case "android" -> new AndroidDriver(serverUrl, androidOptions());
            case "ios"     -> new IOSDriver(serverUrl, iosOptions());
            default -> throw new IllegalArgumentException("Unknown platform: " + platform);
        };
    }

    private static UiAutomator2Options androidOptions() {
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setAutomationName("UiAutomator2")
                .setUdid(System.getProperty("udid", "emulator-5554"))
                .setAppPackage("club.verona")
                .setAppActivity("club.verona.MainActivity")
                .setNoReset(true)                       // keep logged-in state
                .setAutoGrantPermissions(true)
                .setNewCommandTimeout(Duration.ofSeconds(300));

        // CRITICAL for the Verona app: it runs a continuous animation, so the UI
        // never reports "idle". Without this, every findElement blocks ~10s and
        // `adb shell uiautomator dump` fails with "could not get idle state".
        options.setCapability("appium:settings[waitForIdleTimeout]", 0);

        // Always cold-start the app so every session begins on the Home screen
        options.setCapability("appium:forceAppLaunch", true);

        // UiAutomator2's XPath2 engine fails on following::/preceding-sibling::
        // axes ("ListItr cannot be cast to NodeType") — force the XPath1 engine.
        options.setCapability("appium:settings[enforceXPath1]", true);

        // Stability: the animated UI makes XPath snapshots flaky and can hang
        // the UiAutomator2 instrumentation on long runs.
        options.setCapability("appium:disableWindowAnimation", true);
        options.setCapability("appium:adbExecTimeout", 60000);
        options.setCapability("appium:uiautomator2ServerLaunchTimeout", 60000);
        options.setCapability("appium:uiautomator2ServerInstallTimeout", 60000);
        // The instrumentation occasionally hangs on this app; fail commands in
        // 60s (default 240s) so the test layer can rebuild the session quickly.
        options.setCapability("appium:uiautomator2ServerReadTimeout", 60000);
        return options;
    }

    private static XCUITestOptions iosOptions() {
        return new XCUITestOptions()
                .setPlatformName("iOS")
                .setAutomationName("XCUITest")
                .setDeviceName(System.getProperty("deviceName", "iPhone 16"))
                .setBundleId("club.verona")             // confirm with: xcrun simctl listapps booted
                .setNoReset(true)
                .setNewCommandTimeout(Duration.ofSeconds(300));
    }

    /**
     * Kills leftover UiAutomator2 instrumentation on the device. Creating a
     * session while a previous (possibly hung) server instance lingers causes
     * "socket hang up" / proxy-timeout failures on this app.
     */
    public static void cleanupInstrumentation() {
        String udid = System.getProperty("udid", "emulator-5554");
        String androidHome = System.getenv("ANDROID_HOME");
        String adb = (androidHome != null ? androidHome : System.getProperty("user.home")
                + "/Library/Android/sdk") + "/platform-tools/adb";
        for (String pkg : new String[]{
                "io.appium.uiautomator2.server", "io.appium.uiautomator2.server.test"}) {
            try {
                new ProcessBuilder(adb, "-s", udid, "shell", "am", "force-stop", pkg)
                        .start().waitFor();
            } catch (Exception ignored) {
                // adb not found or command failed — session creation may still work
            }
        }
    }

    /**
     * Clears all app data (`adb shell pm clear club.verona`) so the next
     * launch starts logged out at the landing screen. Call BEFORE creating the
     * session (forceAppLaunch then cold-starts into the fresh state).
     */
    public static void clearAppStorage() {
        runAdb("shell", "pm", "clear", "club.verona");
    }

    private static void runAdb(String... args) {
        String udid = System.getProperty("udid", "emulator-5554");
        String androidHome = System.getenv("ANDROID_HOME");
        String adb = (androidHome != null ? androidHome : System.getProperty("user.home")
                + "/Library/Android/sdk") + "/platform-tools/adb";
        java.util.List<String> cmd = new java.util.ArrayList<>();
        cmd.add(adb); cmd.add("-s"); cmd.add(udid);
        cmd.addAll(java.util.Arrays.asList(args));
        try {
            new ProcessBuilder(cmd).start().waitFor();
        } catch (Exception e) {
            throw new IllegalStateException("adb command failed: " + String.join(" ", args), e);
        }
    }

    private static URL serverUrl() {
        try {
            return new URL(System.getProperty("appium.server", "http://127.0.0.1:4723"));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Bad Appium server URL", e);
        }
    }
}
