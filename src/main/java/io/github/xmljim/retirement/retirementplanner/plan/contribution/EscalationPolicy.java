/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.contribution;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Annual auto-escalation rule for a {@link ContributionPolicy}
 * (ADR-003). At each year boundary the contribution engine increases
 * the employee contribution by {@link #annualIncrease()}, stopping once
 * the running rate reaches {@link #cap()}.
 *
 * <p>Both fields are decimals — {@code 0.01} means a one-percentage-
 * point bump per year; {@code 0.15} caps escalation at 15 %.
 *
 * <p>For {@link PercentOfSalary} contributions, escalation adds
 * {@code annualIncrease} to the percentage. For {@link FixedDollar}
 * contributions, escalation has no defined meaning at the value-type
 * level; the contribution engine treats {@code FixedDollar + escalation}
 * as a configuration error or a no-op (decision deferred to S-2.4).
 */
public record EscalationPolicy(BigDecimal annualIncrease, BigDecimal cap) {

    public EscalationPolicy {
        Objects.requireNonNull(annualIncrease, "annualIncrease");
        Objects.requireNonNull(cap, "cap");
        if (annualIncrease.signum() < 0) {
            throw new IllegalArgumentException("annualIncrease must be non-negative: " + annualIncrease);
        }
        if (cap.signum() < 0) {
            throw new IllegalArgumentException("cap must be non-negative: " + cap);
        }
    }
}
