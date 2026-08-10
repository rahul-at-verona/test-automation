# Verona Mobile Automation (Appium + Java)

Automation for the Verona Matchmaking app, built from a **live UI dump** of the running app — no guessed locators.

## The approach, step by step

```
App open on emulator/simulator
        ↓ 1. Dump the real UI tree        (adb / Appium getPageSource on Android, xcrun/WDA on iOS)
        ↓ 2. Read real attributes         (resource-id, text, content-desc / name, label)
        ↓ 3. Build locators from them     (accessibility id first, XPath only when needed)
        ↓ 4. Write the Java test class    (Page Object + TestNG)
        ↓ 5. Run: mvn test -Dplatform=android   |   mvn test -Dplatform=ios
```

1. **Open the app** on the emulator/simulator so the screen you want to automate is live.
2. **Dump the hierarchy.** Android: `adb shell uiautomator dump` or Appium's `getPageSource()`. iOS: the XCUITest driver's page source (WebDriverAgent); `xcrun simctl` helps find bundle ids (`xcrun simctl listapps booted`).
3. **Read real attributes** from the XML: `resource-id`, `text`, `content-desc` (Android) / `name`, `label`, `value` (iOS). Locators built from what actually exists never fail on "element not found because I guessed".
4. **Build locators.** Preference order: accessibility id → resource-id → text-anchored XPath. Index/bounds-based XPath is a last resort.
5. **Write the test class** — Page Object holds locators + actions, test class holds assertions, DriverFactory switches platform via `-Dplatform`.
6. **Run** with Maven; the same test code drives Android (UiAutomator2) or iOS (XCUITest).

## What the dump of the Profile page revealed

- The app is **React Native** (`com.facebook.react` views). Almost **no testIDs** → locators anchor on `text`/`content-desc`.
- The app **never goes UI-idle** (continuous animation), so plain `uiautomator dump` fails with `ERROR: could not get idle state`. Fix: use Appium's page source with the setting `waitForIdleTimeout=0` (set in `DriverFactory`). Without it every findElement blocks ~10 s.
- RN keeps the **previous screen in the tree** (`RouteScreen: 3` = Settings, which also has a "My Profile" card). Ambiguous locators are scoped with `not(ancestor::*[starts-with(@resource-id,'RouteScreen')])`.

## Verified locators (Profile page, Edit tab)

| Element | Locator | Matches |
|---|---|---|
| Header title | `//android.widget.TextView[@text='My Profile' and not(ancestor::*[starts-with(@resource-id,'RouteScreen')])]` | 1 |
| Back arrow | header title `/preceding-sibling::android.view.ViewGroup[@clickable='true']` | 1 |
| Completion badge (92%) | `//android.view.ViewGroup[@clickable='true' and contains(@content-desc,'%')]` | 1 |
| Edit / View tabs | accessibility id `Edit` / `View` | 1 each |
| Name ("Rahul Garg, 25") | `//android.widget.TextView[@text='Tap to edit. Drag to reorder']/following-sibling::android.widget.TextView[1]` | 1 |
| Info (i) icon | same anchor `/following-sibling::android.view.ViewGroup[@clickable='true'][1]` | 1 |
| Verified badge | accessibility id `Verified` | 1 |
| Show-initials toggle | `//android.widget.TextView[@text='Show initials only']/following-sibling::android.view.ViewGroup[@clickable='true'][1]` | 1 |
| Add-photo "+" slots | photo-hint anchor `/preceding-sibling::android.view.ViewGroup[@clickable='true']` | 3 |
| Remove-photo "X" | photo-hint anchor `/preceding-sibling::android.view.ViewGroup[not(@clickable='true')]//android.view.ViewGroup[@clickable='true']` | 3 |
| My location row | `//android.view.ViewGroup[starts-with(@content-desc,'My location')]` | 1 |
| Open to living in row | `starts-with(@content-desc,'Open to living in')` | 1 |
| Marriage timeline row | `starts-with(@content-desc,'Looking to get married')` | 1 |
| Education entries | `//android.widget.TextView[@text='Education']/following-sibling::android.view.ViewGroup[@clickable='true']` | 2 |

All counts verified against the actual dump with xmllint (same XPath engine semantics UiAutomator2 uses).

## Project layout

```
verona-automation/
├── pom.xml
├── testng.xml
└── src/test/java/club/verona/automation/
    ├── core/DriverFactory.java      # -Dplatform switch, Verona-specific caps
    ├── pages/ProfilePage.java       # verified locators + actions
    └── tests/BaseTest.java
        tests/ProfilePageTest.java   # 9 smoke tests
```

## Run it

```bash
appium &                                  # start server (v3, port 4723)
mvn test -Dplatform=android               # emulator-5554 by default
mvn test -Dplatform=android -Dudid=emulator-5556
mvn test -Dplatform=ios                   # after confirming bundle id
```

Prereqs: Java 17+, Maven, Appium with `uiautomator2` driver (already installed on this machine), app logged in on the emulator.

## Tip for the dev team

Adding `testID` props in the React Native code (they surface as `resource-id` on Android, `name` on iOS) would make every locator a one-liner and platform-independent — the bottom navigation already has them (`bottom-navigation-bar`), the rest of the app doesn't.
