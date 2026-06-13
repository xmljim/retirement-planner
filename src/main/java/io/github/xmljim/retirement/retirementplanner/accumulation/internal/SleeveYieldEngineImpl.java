/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.accumulation.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.YearMonth;

import org.springframework.stereotype.Component;

import io.github.xmljim.retirement.retirementplanner.accumulation.SleeveYieldEngine;
import io.github.xmljim.retirement.retirementplanner.plan.Assumptions;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountSleeve;
import io.github.xmljim.retirement.retirementplanner.plan.account.SleeveYieldPolicy;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Geometric monthly compounding implementation of
 * {@link SleeveYieldEngine}. Uses {@link MathContext#DECIMAL128} for
 * the intermediate root extraction; the final {@code Money} is
 * normalized to scale 6 with {@code HALF_EVEN} per ADR-007.
 */
@Component
class SleeveYieldEngineImpl implements SleeveYieldEngine {

    /** Months in a year — used for the 1/12 root exponent. */
    private static final int MONTHS_PER_YEAR = 12;

    /** Working precision for the Newton-Raphson 12th-root iteration. */
    private static final MathContext MC = MathContext.DECIMAL128;

    /** Convergence threshold for the iteration; tighter than scale 6. */
    private static final BigDecimal EPSILON = new BigDecimal("1E-20");

    /** Iteration cap — Newton's method on a smooth function converges quickly; this is paranoia. */
    private static final int MAX_ITERATIONS = 100;

    @Override
    public Money accruePerMonth(AccountSleeve sleeve, YearMonth period, Assumptions assumptions) {
        BigDecimal annualRate = annualRateFor(sleeve.yieldPolicy(), assumptions);
        if (sleeve.balance().amount().signum() == 0 || annualRate.signum() == 0) {
            return Money.ZERO_USD;
        }
        BigDecimal monthlyRate = monthlyRateFromAnnual(annualRate);
        return sleeve.balance().times(monthlyRate);
    }

    private static BigDecimal annualRateFor(SleeveYieldPolicy policy, Assumptions assumptions) {
        return switch (policy) {
            case SleeveYieldPolicy.FixedRate fr -> fr.annualRate();
            case SleeveYieldPolicy.MoneyMarket mm -> mm.currentRate();
            case SleeveYieldPolicy.TracksAllocation _ -> assumptions.preRetirementReturnRate();
        };
    }

    /**
     * Returns the effective monthly rate {@code (1 + annual)^(1/12) - 1}
     * via Newton-Raphson on {@code f(x) = x^12 - (1 + annual)}. Allows
     * negative annual rates so long as {@code 1 + annual > 0}.
     */
    static BigDecimal monthlyRateFromAnnual(BigDecimal annualRate) {
        BigDecimal onePlusAnnual = BigDecimal.ONE.add(annualRate, MC);
        if (onePlusAnnual.signum() <= 0) {
            throw new IllegalArgumentException(
                    "annualRate produces a non-positive growth factor (1 + r <= 0): " + annualRate);
        }
        BigDecimal root = twelfthRoot(onePlusAnnual);
        return root.subtract(BigDecimal.ONE, MC);
    }

    /**
     * Computes the 12th root of {@code value} (which must be positive)
     * via Newton-Raphson: iterate {@code x' = x - (x^12 - v) / (12 x^11)}.
     */
    private static BigDecimal twelfthRoot(BigDecimal value) {
        BigDecimal x = initialGuess(value);
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            BigDecimal x11 = x.pow(11, MC);
            BigDecimal x12 = x11.multiply(x, MC);
            BigDecimal numerator = x12.subtract(value, MC);
            BigDecimal denominator = x11.multiply(BigDecimal.valueOf(MONTHS_PER_YEAR), MC);
            BigDecimal delta = numerator.divide(denominator, MC);
            BigDecimal next = x.subtract(delta, MC);
            if (delta.abs().compareTo(EPSILON) < 0) {
                return next;
            }
            x = next;
        }
        return x;
    }

    /**
     * A reasonable starting point: for {@code v = 1 + annual}, an
     * additive seed {@code 1 + annual/12} sits near the true root for
     * the rate magnitudes we use (single-digit percents).
     */
    private static BigDecimal initialGuess(BigDecimal value) {
        BigDecimal annual = value.subtract(BigDecimal.ONE, MC);
        return BigDecimal.ONE.add(annual.divide(BigDecimal.valueOf(MONTHS_PER_YEAR), MC), MC);
    }
}
