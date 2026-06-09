package io.github.xmljim.retirement.retirementplanner.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Spring Modulith verifies all module boundaries")
    void verifiesModuleBoundaries() {
        ApplicationModules.of(RetirementPlannerApplication.class).verify();
    }

    @Test
    @DisplayName("project declares exactly 10 top-level modules (ADR-008)")
    void modulesAreInspectable() {
        // Per ADR-008, the project declares 10 top-level modules. New
        // modules require an ADR amendment, so this assertion is
        // intentionally exact: drift means someone moved something
        // without updating the architectural contract.
        var modules = ApplicationModules.of(RetirementPlannerApplication.class);
        assertThat(modules.stream().count()).isEqualTo(10);
    }
}
