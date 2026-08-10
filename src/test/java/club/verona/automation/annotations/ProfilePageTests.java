package club.verona.automation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Segregation marker: this test originated from the "ProfilePageTests" suite.
 * Purely a readability/classification marker. Actual filtering is done via the
 * matching TestNG group set on each {@code @Test(groups = "ProfilePageTests")}, e.g.
 *   mvn test -Dgroups=ProfilePageTests
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ProfilePageTests {
}
