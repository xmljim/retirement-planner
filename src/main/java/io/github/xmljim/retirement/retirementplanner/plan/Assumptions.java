/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Plan-wide deterministic assumptions used by the accumulation engine
 * (FR-5.1, ADR-002). Carries the inputs the engine needs when no
 * stochastic draw is available — the deterministic substitute for
 * EPIC-5's historical-returns dataset.
 *
 * <p>Rates are decimal fractions, not percentages — {@code 0.07} for
 * 7%, never {@code 7.0}. Per ADR-007, all rates are {@link BigDecimal}.
 *
 * <p>Other assumption fields named in FR-5.1 (inflation, contribution-limit
 * growth) live elsewhere today: contribution-limit growth is on
 * {@code IrsLimits}; inflation lands here when the drawdown phase
 * adds it. A {@code postRetirementReturnRate} sibling will join when
 * EPIC-4 (drawdown) lands — pre/post split reflects the typically
 * lower equity weight of a glide path's late years.
 */
public record Assumptions(BigDecimal preRetirementReturnRate, BigDecimal cashInterestRate) {

    public Assumptions {
        Objects.requireNonNull(preRetirementReturnRate, "preRetirementReturnRate");
        Objects.requireNonNull(cashInterestRate, "cashInterestRate");
    }
}
