package club.verona.automation.pages.editors;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One getPageSource() snapshot, parsed client-side, with coordinate taps.
 *
 * Why this exists: on this app, on-device element queries (XPath, UiSelector)
 * frequently hang the UiAutomator2 instrumentation, especially on editor
 * screens with animated/re-rendering lists. getPageSource() + W3C-actions
 * taps proved reliable, so editor interactions never touch the accessibility
 * query path at all.
 */
public final class UiSnapshot {

    static final Pattern BOUNDS = Pattern.compile("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]");

    public static final class Snap {
        public final String cls, desc, text;
        public final boolean clickable;
        /**
         * Backed by AccessibilityNodeInfo.isVisibleToUser() — the SAME flag
         * Appium's element.isDisplayed() returns. true = laid out within the
         * visible screen area (note: like isDisplayed(), it does NOT detect
         * occlusion by an overlay drawn on top in the same window).
         */
        public final boolean displayed;
        public final int cx, cy;
        public final Element element;

        Snap(Element el) {
            this.element = el;
            this.cls = el.getTagName();
            this.desc = el.getAttribute("content-desc");
            this.text = el.getAttribute("text");
            this.clickable = "true".equals(el.getAttribute("clickable"));
            this.displayed = "true".equals(el.getAttribute("displayed"));
            Matcher m = BOUNDS.matcher(el.getAttribute("bounds"));
            if (m.matches()) {
                this.cx = (Integer.parseInt(m.group(1)) + Integer.parseInt(m.group(3))) / 2;
                this.cy = (Integer.parseInt(m.group(2)) + Integer.parseInt(m.group(4))) / 2;
            } else {
                this.cx = -1;
                this.cy = -1;
            }
        }
    }

    private final List<Snap> nodes = new ArrayList<>();

    public static UiSnapshot capture(AppiumDriver driver) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(
                            driver.getPageSource().getBytes(StandardCharsets.UTF_8)));
            UiSnapshot s = new UiSnapshot();
            s.collect(doc.getDocumentElement());
            return s;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse page source", e);
        }
    }

    private void collect(Element el) {
        nodes.add(new Snap(el));
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node c = children.item(i);
            if (c instanceof Element) {
                collect((Element) c);
            }
        }
    }

    public List<Snap> all() { return nodes; }

    /** First node (document order) matching the predicate, or null. */
    public Snap first(Predicate<Snap> p) {
        return nodes.stream().filter(p).findFirst().orElse(null);
    }

    public Snap firstByDesc(String desc)        { return first(n -> desc.equals(n.desc)); }
    public Snap firstByDescPrefix(String p)     { return first(n -> n.desc != null && n.desc.startsWith(p)); }
    public Snap firstByText(String text)        { return first(n -> text.equals(n.text)); }
    public boolean containsText(String text)    { return firstByText(text) != null; }
    public boolean containsDescPrefix(String p) { return firstByDescPrefix(p) != null; }

    // ----- visibility-aware variants (equivalent to element.isDisplayed()) -----
    public boolean isTextDisplayed(String text) {
        return first(n -> text.equals(n.text) && n.displayed) != null;
    }
    public boolean isDescDisplayed(String desc) {
        return first(n -> desc.equals(n.desc) && n.displayed) != null;
    }

    /** First clickable descendant of the given node (e.g. a row's checkbox). */
    public Snap firstClickableDescendant(Snap parent) {
        List<Snap> out = new ArrayList<>();
        collectDescendants(parent.element, out);
        return out.stream().filter(n -> n.clickable).findFirst().orElse(null);
    }

    private void collectDescendants(Element el, List<Snap> out) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node c = children.item(i);
            if (c instanceof Element) {
                out.add(new Snap((Element) c));
                collectDescendants((Element) c, out);
            }
        }
    }

    /** Coordinate tap via W3C actions — bypasses the accessibility tree. */
    public static void tap(AppiumDriver driver, Snap node) {
        tap(driver, node.cx, node.cy);
    }

    public static void tap(AppiumDriver driver, int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence seq = new Sequence(finger, 1);
        seq.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        seq.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        seq.addAction(new Pause(finger, Duration.ofMillis(100)));
        seq.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(List.of(seq));
    }

    /** Polls (via fresh snapshots) until the condition holds. */
    public static UiSnapshot waitFor(AppiumDriver driver, Predicate<UiSnapshot> cond,
                              long timeoutMs, String what) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        UiSnapshot snap = null;
        while (System.currentTimeMillis() < deadline) {
            snap = capture(driver);
            if (cond.test(snap)) {
                return snap;
            }
            sleep(1200);
        }
        throw new org.openqa.selenium.TimeoutException("Timed out waiting for: " + what);
    }

    public static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
