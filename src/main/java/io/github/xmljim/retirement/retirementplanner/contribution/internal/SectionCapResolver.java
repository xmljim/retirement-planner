/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import io.github.xmljim.retirement.retirementplanner.contribution.IrsLimits;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Resolves the per-section employee caps (§402(g), §408, §223) for a
 * given age tier from {@link IrsLimits}.
 *
 * <p>Extracted from {@link ContributionEngineImpl} so the engine
 * orchestrator stays below the project's coupling threshold and the
 * tier-ladder math sits with the limits domain.
 */
final class SectionCapResolver {

    private SectionCapResolver() {}

    /** §402(g) cap with age 50+ or 60–63 catch-up extension as applicable. */
    static Money section402gCap(IrsLimits limits, int age) {
        Money cap = limits.employee401kBase();
        if (age >= 60) {
            return cap.plus(limits.employee401k60PlusCatchup());
        }
        if (age >= 50) {
            return cap.plus(limits.employee401k50PlusCatchup());
        }
        return cap;
    }

    /** §408 (IRA) cap with age 50+ catch-up extension as applicable. */
    static Money section408Cap(IrsLimits limits, int age) {
        return age >= 50 ? limits.iraBase().plus(limits.ira50PlusCatchup()) : limits.iraBase();
    }

    /** §223 (HSA self-only) cap with age 55+ catch-up extension as applicable. */
    static Money section223Cap(IrsLimits limits, int age) {
        Money cap = limits.hsaSelfOnly();
        return age >= 55 ? cap.plus(limits.hsa55PlusCatchup()) : cap;
    }
}
