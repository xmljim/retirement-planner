/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.contribution;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * One step of a tiered employer-match formula (ADR-003). Each tier
 * matches employee contributions up to {@link #employeeContribPctUpTo()}
 * (cumulative across this tier and all earlier tiers) at
 * {@link #matchPct()} of each employee dollar.
 *
 * <p>Example — &ldquo;100&nbsp;% of the first 3&nbsp;%, 50&nbsp;% of the
 * next 2&nbsp;%&rdquo; encodes as:
 * <pre>
 * [ MatchTier(0.03, 1.00), MatchTier(0.05, 0.50) ]
 * </pre>
 *
 * <p>Both fields are decimals — never percent-points.
 */
public record MatchTier(BigDecimal employeeContribPctUpTo, BigDecimal matchPct) {

    public MatchTier {
        Objects.requireNonNull(employeeContribPctUpTo, "employeeContribPctUpTo");
        Objects.requireNonNull(matchPct, "matchPct");
        if (employeeContribPctUpTo.signum() <= 0) {
            throw new IllegalArgumentException("employeeContribPctUpTo must be positive: " + employeeContribPctUpTo);
        }
        if (matchPct.signum() < 0) {
            throw new IllegalArgumentException("matchPct must be non-negative: " + matchPct);
        }
    }
}
