/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Conversions between annual and per-period rates under geometric
 * compounding. Used wherever the codebase needs to convert an annual
 * rate (return, inflation, contribution-limit growth, salary growth)
 * to its monthly-equivalent compounding rate.
 *
 * <p>Formula: {@code monthlyRate = (1 + annual)^(1/12) - 1}. By
 * construction, compounding {@code monthlyRate} 12 times recovers
 * {@code 1 + annual} exactly (within working precision), so a
 * year-over-year invariant holds: starting balance &times; (1 + annual)
 * equals the same balance compounded monthly for 12 months.
 *
 * <p>Computed via Newton-Raphson on {@code f(x) = x^12 - (1 + annual)}
 * at {@link MathContext#DECIMAL128}; the high-precision root is then
 * available to callers that re-round through {@link Money} (scale 6,
 * {@code HALF_EVEN}) per ADR-007. Negative annual rates are supported
 * so long as {@code 1 + annual > 0}.
 */
public final class CompoundRate {

    /** Months in a year — used for the 1/12 root exponent. */
    private static final int MONTHS_PER_YEAR = 12;

    /** Working precision for the Newton-Raphson 12th-root iteration. */
    private static final MathContext MC = MathContext.DECIMAL128;

    /** Convergence threshold for the iteration; tighter than scale 6. */
    private static final BigDecimal EPSILON = new BigDecimal("1E-20");

    /** Iteration cap — Newton's method on a smooth function converges quickly; this is paranoia. */
    private static final int MAX_ITERATIONS = 100;

    private CompoundRate() {}

    /**
     * Returns the effective monthly rate that, compounded 12 times,
     * recovers {@code 1 + annualRate}.
     *
     * @param annualRate the annual rate as a decimal fraction
     *                   ({@code 0.07} for 7%); negative values
     *                   permitted so long as {@code 1 + r > 0}
     * @return the monthly rate {@code (1 + annualRate)^(1/12) - 1}
     * @throws IllegalArgumentException if {@code 1 + annualRate <= 0}
     */
    public static BigDecimal monthlyFromAnnual(BigDecimal annualRate) {
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
