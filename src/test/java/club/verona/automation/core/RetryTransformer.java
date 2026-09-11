package club.verona.automation.core;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Wires {@link RetryAnalyzer} onto every {@code @Test} method in the suite,
 * so a single flaky failure (a one-off BestQ remote-grid hiccup, a slow
 * render) gets one automatic retry without each of the ~75+ test methods
 * needing {@code retryAnalyzer = RetryAnalyzer.class} added by hand.
 * Registered as a listener in testng.xml.
 */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                           Constructor testConstructor, Method testMethod) {
        annotation.setRetryAnalyzer(RetryAnalyzer.class);
    }
}
