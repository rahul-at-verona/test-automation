package club.verona.automation.pages.editors;

import io.appium.java_client.AppiumDriver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reads suggestion lists from ONE getPageSource() dump parsed client-side.
 *
 * Why: repeated on-device XPath queries (WebDriverWait polls every 500ms)
 * against this app's constantly re-rendering lists crash the UiAutomator2
 * instrumentation ("socket hang up" / proxy timeouts). A single page-source
 * snapshot per read is far gentler and proved stable.
 *
 * Suggestion rows = elements with a non-empty content-desc that appear in
 * document order AFTER the first EditText (the search box) and BEFORE the
 * Save button ('Save' / 'Save changes'). 'Open to all …' helper rows are
 * skipped. Works for all three editor screens because each renders its own
 * subtree before any stale screens kept in the RN tree.
 */
final class SourceList {

    private SourceList() {}

    static List<String> suggestions(AppiumDriver driver, Set<String> stopLabels) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(new ByteArrayInputStream(
                            driver.getPageSource().getBytes(StandardCharsets.UTF_8)));
            List<String> out = new ArrayList<>();
            walk(doc.getDocumentElement(), new boolean[]{false}, out, stopLabels);
            return out;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse page source", e);
        }
    }

    /** Depth-first document-order walk; returns true when the stop label is hit. */
    private static boolean walk(Element el, boolean[] afterSearchBox,
                                List<String> out, Set<String> stopLabels) {
        if (el.getTagName().contains("EditText")) {
            afterSearchBox[0] = true;
        }
        String desc = el.getAttribute("content-desc");
        if (afterSearchBox[0] && desc != null && !desc.isEmpty()) {
            if (stopLabels.contains(desc)) {
                return true;
            }
            if (!desc.startsWith("Open to all")) {
                out.add(desc);
            }
        }
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node c = children.item(i);
            if (c instanceof Element && walk((Element) c, afterSearchBox, out, stopLabels)) {
                return true;
            }
        }
        return false;
    }
}
