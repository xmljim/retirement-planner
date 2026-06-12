/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.contribution;

import java.util.Objects;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * {@link ContributionAmount} expressed as a fixed annual dollar amount
 * (ADR-003). Annual matches the framing of IRS limits; the contribution
 * engine prorates monthly. Negative amounts are rejected.
 */
public record FixedDollar(Money annualAmount) implements ContributionAmount {

    public FixedDollar {
        Objects.requireNonNull(annualAmount, "annualAmount");
        if (annualAmount.amount().signum() < 0) {
            throw new IllegalArgumentException("annualAmount must be non-negative: " + annualAmount);
        }
    }
}
