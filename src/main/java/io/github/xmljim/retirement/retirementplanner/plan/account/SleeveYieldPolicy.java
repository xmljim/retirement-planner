/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.account;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Yield model applied to an {@link AccountSleeve}'s balance (ADR-002).
 *
 * <p>Cash sleeves typically use {@link FixedRate} or {@link MoneyMarket};
 * allocation-driven sleeves use {@link TracksAllocation} so their
 * returns flow from the asset-class returns the allocation policy
 * resolves to.
 */
public sealed interface SleeveYieldPolicy {

    /** Constant nominal annual yield (e.g. {@code 0.045} for a 4.5% MMF). */
    record FixedRate(BigDecimal annualRate) implements SleeveYieldPolicy {

        public FixedRate {
            Objects.requireNonNull(annualRate, "annualRate");
        }
    }

    /**
     * Yield from a money-market / cash-sweep sleeve held inside a
     * wrapper account (an IRA or 401(k) commonly carries idle cash at
     * a sweep rate). The rate is the sleeve's current quoted annual
     * yield; callers update it as part of normal scenario maintenance.
     */
    record MoneyMarket(BigDecimal currentRate) implements SleeveYieldPolicy {

        public MoneyMarket {
            Objects.requireNonNull(currentRate, "currentRate");
        }
    }

    /** Yield derived from the sleeve's {@link SleeveKind} allocation and the asset-class return draws. */
    record TracksAllocation() implements SleeveYieldPolicy {}
}
