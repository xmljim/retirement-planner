/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * {@link ContributionAmount} expressed as a fraction of prevailing
 * salary (ADR-003). The rate is a decimal — {@code 0.05} means 5 %.
 * Negative rates are rejected; rates above {@code 1.0} are allowed at
 * the policy level (the {@code §402(g)} cap is applied later by the
 * contribution engine, not by this value type).
 */
public record PercentOfSalary(BigDecimal pct) implements ContributionAmount {

    public PercentOfSalary {
        Objects.requireNonNull(pct, "pct");
        if (pct.signum() < 0) {
            throw new IllegalArgumentException("pct must be non-negative: " + pct);
        }
    }
}
