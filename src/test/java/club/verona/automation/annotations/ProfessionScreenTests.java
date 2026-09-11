package club.verona.automation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Segregation marker: this test originated from the "ProfessionScreenTests" suite
 * ("Your current profession" — the 5 profession-type options, their differing
 * Organization/Designation/Profession selectors, LinkedIn field, and Continue gating).
 * Purely a readability/classification marker. Actual filtering is done via the
 * matching TestNG group set on each {@code @Test(groups = "ProfessionScreenTests")}, e.g.
 *   mvn test -Dgroups=ProfessionScreenTests
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ProfessionScreenTests {
}
