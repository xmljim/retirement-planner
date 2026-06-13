/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CompoundRateTest {

    private static final BigDecimal PRECISION_TOLERANCE = new BigDecimal("0.0000000001");

    @ParameterizedTest(name = "monthlyFromAnnual({0}) recovers (1+{0}) after 12 compounds")
    @ValueSource(strings = {"0.03", "0.045", "0.07", "0.10"})
    @DisplayName("monthlyFromAnnual recovers the annual rate after 12 compounds for typical positive rates")
    void monthlyRecoversAnnualForPositiveRates(String annualRateLiteral) {
        BigDecimal annual = new BigDecimal(annualRateLiteral);
        BigDecimal monthly = CompoundRate.monthlyFromAnnual(annual);
        BigDecimal compounded = BigDecimal.ONE.add(monthly).pow(12);
        BigDecimal expected = BigDecimal.ONE.add(annual);
        assertThat(compounded).isCloseTo(expected, within(PRECISION_TOLERANCE));
    }

    @Test
    @DisplayName("monthlyFromAnnual handles a zero annual rate")
    void zeroAnnualRateProducesZeroMonthly() {
        BigDecimal monthly = CompoundRate.monthlyFromAnnual(BigDecimal.ZERO);
        assertThat(monthly).isCloseTo(BigDecimal.ZERO, within(PRECISION_TOLERANCE));
    }

    @Test
    @DisplayName("monthlyFromAnnual handles a negative annual rate (deflation / market drawdown)")
    void negativeAnnualRateProducesNegativeMonthly() {
        BigDecimal annual = new BigDecimal("-0.10");
        BigDecimal monthly = CompoundRate.monthlyFromAnnual(annual);
        assertThat(monthly.signum()).isNegative();
        BigDecimal compounded = BigDecimal.ONE.add(monthly).pow(12);
        BigDecimal expected = BigDecimal.ONE.add(annual);
        assertThat(compounded).isCloseTo(expected, within(PRECISION_TOLERANCE));
    }

    @Test
    @DisplayName("monthlyFromAnnual rejects an annual rate that produces a non-positive growth factor")
    void rejectsRatesThatBreakOnePlusR() {
        assertThatThrownBy(() -> CompoundRate.monthlyFromAnnual(new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-positive growth factor");
        assertThatThrownBy(() -> CompoundRate.monthlyFromAnnual(new BigDecimal("-1.50")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
