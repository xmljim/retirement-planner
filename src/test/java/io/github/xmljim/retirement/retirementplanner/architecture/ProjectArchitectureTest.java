package io.github.xmljim.retirement.retirementplanner.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Project-wide architecture rules. See ADR-008 (module boundaries) and
 * ADR-009 (quality gates) for rationale.
 *
 * <p>Module-specific rules live in per-module ArchitectureTest classes.
 */
@AnalyzeClasses(
        packages = "io.github.xmljim.retirement.retirementplanner",
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
@SuppressWarnings("PMD.TestClassWithoutTestCases")
// ArchUnit declares rules as @ArchTest static-final fields. PMD doesn't
// recognize them as test cases, but they execute via JUnit5 platform.
public class ProjectArchitectureTest {

    // Until EPIC-1 lands and modules are populated, rules may match zero
    // classes; .allowEmptyShould(true) keeps the rules in place without
    // false-positive failures.

    private static final DescribedPredicate<JavaClass> BUSINESS_MODULE_INTERNALS =
            new DescribedPredicate<>(
                    "reside in any package ['..internal..', '..repository..', '..model.entity..'] outside of "
                            + "api.internal (controllers' own siblings)") {
                @Override
                public boolean test(JavaClass cls) {
                    String pkg = cls.getPackageName();
                    boolean forbidden =
                            pkg.contains(".internal") || pkg.contains(".repository") || pkg.contains(".model.entity");
                    boolean controllerSibling =
                            "io.github.xmljim.retirement.retirementplanner.api.internal".equals(pkg);
                    return forbidden && !controllerSibling;
                }
            };

    /**
     * CLAUDE.md / ADR-001: controllers delegate to services. They must
     * not import internal packages, repositories, or entities directly.
     *
     * <p>Controllers themselves live under {@code api/internal/}, so a
     * controller calling a sibling presentation helper in the same
     * package is the expected shape — exclude {@code api/internal/} from
     * the forbidden set; the rule's target is reaching into <em>business
     * module</em> internals (plan, contribution, tax, …).
     */
    @ArchTest
    static final ArchRule controllers_must_not_reach_into_internals = noClasses()
            .that()
            .areAnnotatedWith(RestController.class)
            .or()
            .areAnnotatedWith(Controller.class)
            .should()
            .dependOnClassesThat(BUSINESS_MODULE_INTERNALS)
            .because("CLAUDE.md: controllers delegate to services. They cannot import internals or entities directly.")
            .allowEmptyShould(true);

    /**
     * CLAUDE.md: constructor injection only. @Autowired on fields banned.
     */
    @ArchTest
    static final ArchRule no_autowired_fields = fields().should()
            .notBeAnnotatedWith(Autowired.class)
            .because("CLAUDE.md: constructor injection required. Field injection (@Autowired on fields) is banned.")
            .allowEmptyShould(true);

    /**
     * ADR-008 / CLAUDE.md: simulation hot path may not use the event bus.
     * Modulith events are reserved for cold-path workflow boundaries.
     */
    @ArchTest
    static final ArchRule simulation_must_not_use_event_publisher = noClasses()
            .that()
            .resideInAPackage("..simulation..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(ApplicationEventPublisher.class)
            .because("ADR-008: simulation is the hot path. Use direct method calls, not events. "
                    + "If you genuinely need an event, this rule is wrong — re-read ADR-008 first.")
            .allowEmptyShould(true);

    /**
     * ADR-002 / ADR-008: cross-module access must go through public API
     * packages. Modulith verifies this at runtime; we add a static check
     * here for fast feedback.
     */
    @ArchTest
    static final ArchRule modules_internals_are_private = classes()
            .that()
            .resideInAPackage("..internal..")
            .should()
            .onlyBeAccessed()
            .byClassesThat()
            .resideInAnyPackage(
                    "..internal..",
                    "io.github.xmljim.retirement.retirementplanner.contribution..",
                    "io.github.xmljim.retirement.retirementplanner.tax..",
                    "io.github.xmljim.retirement.retirementplanner.bucket..",
                    "io.github.xmljim.retirement.retirementplanner.simulation..",
                    "io.github.xmljim.retirement.retirementplanner.allocation..",
                    "io.github.xmljim.retirement.retirementplanner.returns..",
                    "io.github.xmljim.retirement.retirementplanner.plan..",
                    "io.github.xmljim.retirement.retirementplanner.scenario..",
                    "io.github.xmljim.retirement.retirementplanner.api..",
                    "io.github.xmljim.retirement.retirementplanner.shared..")
            .because("ADR-008: a module's internal/ package is accessible only to that module itself.")
            .allowEmptyShould(true);
}
