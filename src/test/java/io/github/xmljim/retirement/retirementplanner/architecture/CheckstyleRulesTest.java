/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;

/**
 * Verifies the project's Checkstyle ADR-007 primitive-double-ban rule fires where it should and
 * stays quiet inside the {@code simulation.montecarlo.internal} carve-out (ADR-005).
 *
 * <p>Loads the real {@code config/checkstyle/checkstyle.xml} and {@code suppressions.xml} so the
 * test is regression-protection for any future change to either file. Uses fixture sources written
 * under fake project-shaped paths so the suppression file's path pattern actually matches.
 */
class CheckstyleRulesTest {

    private static final String CHECKSTYLE_CONFIG = "config/checkstyle/checkstyle.xml";
    private static final String SUPPRESSIONS_FILE = "config/checkstyle/suppressions.xml";
    private static final String ADR_007_TAG = "ADR-007";

    private static final String VIOLATING_DOUBLE = """
            package fixture;
            class HasDouble {
                double rate = 0;
            }
            """;

    private static final String VIOLATING_FLOAT_ARRAY = """
            package fixture;
            class HasFloatArray {
                float[] history = new float[10];
            }
            """;

    private static final String VIOLATING_FLOAT_LOCAL = """
            package fixture;
            class HasFloatLocal {
                void m() {
                    float weight = 1;
                    System.out.println(weight);
                }
            }
            """;

    private static Configuration config;

    @BeforeAll
    static void loadConfig() throws CheckstyleException {
        var props = new Properties();
        props.setProperty("suppressions.file", absolute(SUPPRESSIONS_FILE));
        config = ConfigurationLoader.loadConfiguration(
                absolute(CHECKSTYLE_CONFIG),
                new PropertiesExpander(props),
                ConfigurationLoader.IgnoredModulesOptions.OMIT);
    }

    @Test
    @DisplayName("primitive double in a non-MC package fires the ADR-007 rule")
    void doubleOutsideMcFires(@TempDir Path tmp) throws CheckstyleException, IOException {
        var file = writeFixture(tmp, "plan/HasDouble.java", VIOLATING_DOUBLE);
        var events = audit(file);
        assertThat(events).extracting(AuditEvent::getMessage).anyMatch(m -> m.contains(ADR_007_TAG));
    }

    @Test
    @DisplayName("float array in a non-MC package fires the ADR-007 rule")
    void floatArrayOutsideMcFires(@TempDir Path tmp) throws CheckstyleException, IOException {
        var file = writeFixture(tmp, "plan/HasFloatArray.java", VIOLATING_FLOAT_ARRAY);
        var events = audit(file);
        assertThat(events).extracting(AuditEvent::getMessage).anyMatch(m -> m.contains(ADR_007_TAG));
    }

    @Test
    @DisplayName("primitive double inside simulation.montecarlo.internal is suppressed")
    void doubleInsideMcCarveoutIsSuppressed(@TempDir Path tmp) throws CheckstyleException, IOException {
        var file = writeFixture(tmp, "simulation/montecarlo/internal/InnerLoop.java", VIOLATING_DOUBLE);
        var events = audit(file);
        assertThat(events)
                .as("MC inner loop is the bounded ADR-005 exception; rule must not fire here")
                .extracting(AuditEvent::getMessage)
                .noneMatch(m -> m.contains(ADR_007_TAG));
    }

    @Test
    @DisplayName("float local variable inside MC carve-out is also suppressed")
    void floatInsideMcCarveoutIsSuppressed(@TempDir Path tmp) throws CheckstyleException, IOException {
        var file = writeFixture(tmp, "simulation/montecarlo/internal/Step.java", VIOLATING_FLOAT_LOCAL);
        var events = audit(file);
        assertThat(events).extracting(AuditEvent::getMessage).noneMatch(m -> m.contains(ADR_007_TAG));
    }

    @Test
    @DisplayName("MC carve-out is targeted: other Regexp rules still apply inside MC")
    void otherRegexpRulesStillApplyInsideMc(@TempDir Path tmp) throws CheckstyleException, IOException {
        // BigDecimal-from-double-literal is a separate Regexp rule. The
        // suppression for primitive-double-ban must not silence it.
        var src = """
                package fixture;
                import java.math.BigDecimal;
                class BadConstruction {
                    BigDecimal x = new BigDecimal(2.45);
                }
                """;
        var file = writeFixture(tmp, "simulation/montecarlo/internal/BadConstruction.java", src);
        var events = audit(file);
        assertThat(events)
                .as("BigDecimal-from-double rule applies even inside MC")
                .extracting(AuditEvent::getMessage)
                .anyMatch(m -> m.contains("BigDecimal"));
    }

    // --- helpers ---

    private static String absolute(String relative) {
        return Paths.get(relative).toAbsolutePath().toString();
    }

    private static File writeFixture(Path tmp, String relativePath, String contents) throws IOException {
        var path = tmp.resolve(relativePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, contents);
        return path.toFile();
    }

    private static List<AuditEvent> audit(File file) throws CheckstyleException {
        var checker = new Checker();
        checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
        checker.configure(config);
        var captured = new ArrayList<AuditEvent>();
        checker.addListener(new AuditListener() {
            @Override
            public void auditStarted(AuditEvent event) {}

            @Override
            public void auditFinished(AuditEvent event) {}

            @Override
            public void fileStarted(AuditEvent event) {}

            @Override
            public void fileFinished(AuditEvent event) {}

            @Override
            public void addError(AuditEvent event) {
                captured.add(event);
            }

            @Override
            public void addException(AuditEvent event, Throwable throwable) {
                throw new IllegalStateException("Checker exception on " + event.getFileName(), throwable);
            }
        });
        try {
            checker.process(List.of(file));
        } finally {
            checker.destroy();
        }
        return captured;
    }
}
