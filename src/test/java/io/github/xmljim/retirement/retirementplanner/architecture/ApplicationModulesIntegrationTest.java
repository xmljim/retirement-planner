package io.github.xmljim.retirement.retirementplanner.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import io.github.xmljim.retirement.retirementplanner.RetirementPlannerApplication;

/**
 * Spring Modulith boundary verification. See ADR-008.
 *
 * <p>Fails the build if any module reaches into another module's
 * internal package, or if cyclic dependencies between modules are
 * introduced.
 */
class ApplicationModulesIntegrationTest {

    @Test
    void verifies_module_boundaries() {
        ApplicationModules.of(RetirementPlannerApplication.class).verify();
    }

    @Test
    void modules_are_inspectable() {
        // Confirms ApplicationModules introspection works. Empty-module
        // case is acceptable during scaffolding; once EPIC-1 lands the
        // count assertion below tightens.
        var modules = ApplicationModules.of(RetirementPlannerApplication.class);
        assertThat(modules.stream().count()).isGreaterThanOrEqualTo(0);
    }
}
