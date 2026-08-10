package club.verona.automation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Segregation marker: this test originated from the "PhoneNumberScreenTests" suite.
 * Purely a readability/classification marker. Actual filtering is done via the
 * matching TestNG group set on each {@code @Test(groups = "PhoneNumberScreenTests")}, e.g.
 *   mvn test -Dgroups=PhoneNumberScreenTests
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface PhoneNumberScreenTests {
}
