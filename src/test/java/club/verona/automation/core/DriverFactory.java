package club.verona.automation.core;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;
import org.openqa.selenium.remote.http.ClientConfig;
import org.openqa.selenium.remote.http.Filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Properties;

/**
 * Creates the AppiumDriver for the platform passed via -Dplatform=android|ios.
 * Usage: mvn test -Dplatform=android
 *
 * Targets a local Appium server by default. Set tnt.use.local.grid=true in
 * verona.properties (or -Dtnt.use.local.grid=true) to run against the BestQ
 * device farm instead, using the grid URL/credentials/device id from that
 * file (see https://bestq.best-quality.in/ui/#verona/stf-devices).
 */
public final class DriverFactory {

    private static final Properties GRID = loadGridProperties();
    private static final String DEVICE_ID_PREFIX = "DeviceId_";

    private DriverFactory() {}

    public static boolean isRemoteGrid() {
        return Boolean.parseBoolean(
                System.getProperty("tnt.use.local.grid", GRID.getProperty("tnt.use.local.grid", "false")));
    }

    public static AppiumDriver create() {
        String platform = System.getProperty("platform", "android").toLowerCase();

        // The BestQ grid's 401 responses omit WWW-Authenticate, so Selenium's
        // reactive ClientConfig#authenticateAs (java.net.Authenticator) never
        // fires and credentials embedded in the URL are dropped outright.
        // Confirmed against the live grid: only a preemptive Authorization
        // header on every request gets past its auth proxy.
        if (isRemoteGrid()) {
            ClientConfig config = ClientConfig.defaultConfig()
                    .baseUrl(remoteGridUrl())
                    .withFilter(basicAuthFilter(
                            require("tnt.local.grid.user"), require("tnt.local.grid.password")));
            return switch (platform) {
                case "android" -> new AndroidDriver(config, androidOptions());
                case "ios"     -> new IOSDriver(config, iosOptions());
                default -> throw new IllegalArgumentException("Unknown platform: " + platform);
            };
        }

        URL serverUrl = localServerUrl();
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
                .setUdid(resolveUdid())
                .setAppPackage("club.verona")
                .setAppActivity("club.verona.MainActivity")
                .setNoReset(true)                       // keep logged-in state
                .setAutoGrantPermissions(true)
                .setNewCommandTimeout(Duration.ofSeconds(300));

        resolveAppCapability(options);

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

    /**
     * appium:app only works when it points somewhere the Appium SERVER can
     * reach. A local emulator's server runs on this machine, so a local
     * apps/ path is fine — Appium reads it directly and, with noReset,
     * skips reinstalling if the same build is already on the device.
     *
     * BestQ's grid server can't reach this machine's filesystem at all
     * (confirmed live: it 502s with "does not exist or is not accessible"
     * for a local path), so on the remote grid we only set appium:app when
     * app.remote.url points to a build BestQ's server can fetch itself.
     * Without that, the app must already be installed on the target device
     * (e.g. via the BestQ dashboard) before running noReset sessions.
     */
    private static void resolveAppCapability(UiAutomator2Options options) {
        String remoteUrl = System.getProperty("app.remote.url", GRID.getProperty("app.remote.url", ""));
        if (!remoteUrl.isBlank()) {
            options.setApp(remoteUrl);
            return;
        }
        if (isRemoteGrid()) {
            return;
        }
        options.setApp(AppProvisioner.resolve(
                GRID.getProperty("app.file.name", "verona-staging.apk"),
                System.getProperty("app.download.url", GRID.getProperty("app.download.url", ""))));
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
        if (isRemoteGrid()) {
            return; // no local adb access to a device sitting in the BestQ cloud
        }
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
        if (isRemoteGrid()) {
            return; // remote sessions are reset via the noReset/fullReset capability instead
        }
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

    private static URL localServerUrl() {
        try {
            return new URL(System.getProperty("appium.server", "http://127.0.0.1:4723"));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Bad Appium server URL", e);
        }
    }

    private static Filter basicAuthFilter(String user, String password) {
        String token = Base64.getEncoder().encodeToString(
                (user + ":" + password).getBytes(StandardCharsets.UTF_8));
        return next -> request -> {
            request.setHeader("Authorization", "Basic " + token);
            return next.execute(request);
        };
    }

    private static URL remoteGridUrl() {
        try {
            return new URL(require("tnt.local.grid.ip"));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Bad BestQ grid URL (tnt.local.grid.ip)", e);
        }
    }

    /**
     * Resolves the target device udid: explicit -Dudid wins, then the BestQ
     * device id from verona.properties (browser_caps.device, stripping its
     * "DeviceId_" prefix), then the local emulator default.
     */
    private static String resolveUdid() {
        String explicit = System.getProperty("udid");
        if (explicit != null) {
            return explicit;
        }
        if (isRemoteGrid()) {
            String deviceId = require("browser_caps.device");
            return deviceId.startsWith(DEVICE_ID_PREFIX) ? deviceId.substring(DEVICE_ID_PREFIX.length()) : deviceId;
        }
        return "emulator-5554";
    }

    /**
     * -Dkey=value (e.g. injected from a CI secret) always wins over both
     * property files, so credentials never have to be written to disk in CI.
     */
    private static String require(String key) {
        String value = System.getProperty(key, GRID.getProperty(key));
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing required property '" + key + "' (checked -D" + key
                            + ", verona.local.properties, verona.properties)");
        }
        return value;
    }

    /**
     * Loads verona.properties (tracked, non-secret defaults), then layers
     * verona.local.properties on top if present. The local file is
     * git-ignored and holds real grid credentials — see
     * verona.local.properties.example for the keys it should contain.
     */
    private static Properties loadGridProperties() {
        Properties props = new Properties();
        loadResourceInto(props, "verona.properties");
        loadResourceInto(props, "verona.local.properties");
        return props;
    }

    private static void loadResourceInto(Properties props, String resourceName) {
        try (InputStream in = DriverFactory.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load " + resourceName, e);
        }
    }
}
