/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.accumulation.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.xmljim.retirement.retirementplanner.accumulation.SleeveYieldEngine;
import io.github.xmljim.retirement.retirementplanner.plan.Assumptions;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountSleeve;
import io.github.xmljim.retirement.retirementplanner.plan.account.SleeveKind;
import io.github.xmljim.retirement.retirementplanner.plan.account.SleeveYieldPolicy;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

class SleeveYieldEngineImplTest {

    private static final YearMonth JAN_2026 = YearMonth.of(2026, 1);
    private static final BigDecimal RATE_4_5_PCT = new BigDecimal("0.045");
    private static final BigDecimal RATE_7_PCT = new BigDecimal("0.07");
    private static final Assumptions ASSUMPTIONS = new Assumptions(RATE_7_PCT, RATE_4_5_PCT);
    private static final BigDecimal TOLERANCE_DOLLAR_FRACTION = new BigDecimal("0.000001");

    /**
     * Loose tolerance for 12-month compounding tests: 12 successive
     * scale-6 {@code HALF_EVEN} rounds (one per Money multiplication
     * inside the engine, plus the per-month plus) accumulate ≈ 1e-4
     * of the principal in the worst case. 1¢ on a $10k balance is
     * comfortably above that and well below any user-visible drift.
     */
    private static final BigDecimal TOLERANCE_TWELVE_MONTH_CENTS = new BigDecimal("0.01");

    private static final String TEN_THOUSAND = "10000.00";

    private final SleeveYieldEngine engine = new SleeveYieldEngineImpl();

    @Test
    @DisplayName("FixedRate cash sleeve compounds geometrically over 12 months back to the annual rate")
    void fixedRateCompoundsToAnnualOver12Months() {
        Money startBalance = Money.usd(TEN_THOUSAND);
        AccountSleeve sleeve =
                AccountSleeve.of(new SleeveKind.Cash(), startBalance, new SleeveYieldPolicy.FixedRate(RATE_4_5_PCT));

        Money balance = compoundFor12Months(sleeve);

        // 10,000 × 1.045 = 10,450 — geometric monthly compounding must
        // recover the annual rate to within accumulated scale-6 drift.
        Money expected = startBalance.times(BigDecimal.ONE.add(RATE_4_5_PCT));
        assertThat(balance.amount()).isCloseTo(expected.amount(), within(TOLERANCE_TWELVE_MONTH_CENTS));
    }

    @Test
    @DisplayName("MoneyMarket sleeve uses its own currentRate, not Assumptions.cashInterestRate")
    void moneyMarketUsesItsOwnRate() {
        BigDecimal sweepRate = new BigDecimal("0.0525");
        AccountSleeve sleeve = AccountSleeve.of(
                new SleeveKind.Cash(), Money.usd("5000.00"), new SleeveYieldPolicy.MoneyMarket(sweepRate));
        Assumptions assumptionsWithDifferentCash = new Assumptions(RATE_7_PCT, new BigDecimal("0.01"));

        Money accrual = engine.accruePerMonth(sleeve, JAN_2026, assumptionsWithDifferentCash);

        // Expected monthly rate = (1.0525)^(1/12) - 1 ≈ 0.004273... × 5000 ≈ 21.366
        Money expected = Money.usd("5000.00").times(monthlyRate(sweepRate));
        assertThat(accrual.amount()).isCloseTo(expected.amount(), within(TOLERANCE_DOLLAR_FRACTION));
    }

    @Test
    @DisplayName("TracksAllocation reads preRetirementReturnRate from Assumptions")
    void tracksAllocationReadsAssumptions() {
        AccountSleeve sleeve = AccountSleeve.of(
                new SleeveKind.AssetAllocation(), Money.usd("100000.00"), new SleeveYieldPolicy.TracksAllocation());

        Money accrual = engine.accruePerMonth(sleeve, JAN_2026, ASSUMPTIONS);

        Money expected = Money.usd("100000.00").times(monthlyRate(RATE_7_PCT));
        assertThat(accrual.amount()).isCloseTo(expected.amount(), within(TOLERANCE_DOLLAR_FRACTION));
    }

    @Test
    @DisplayName("Mixed-sleeve account: per-sleeve accruals sum to the account-level monthly accrual")
    void mixedSleeveAggregation() {
        AccountSleeve cashSleeve = AccountSleeve.of(
                new SleeveKind.Cash(),
                Money.usd("12500.00"),
                new SleeveYieldPolicy.MoneyMarket(new BigDecimal("0.045")));
        AccountSleeve equitySleeve = AccountSleeve.of(
                new SleeveKind.AssetAllocation(), Money.usd("87500.00"), new SleeveYieldPolicy.TracksAllocation());

        Money cashAccrual = engine.accruePerMonth(cashSleeve, JAN_2026, ASSUMPTIONS);
        Money equityAccrual = engine.accruePerMonth(equitySleeve, JAN_2026, ASSUMPTIONS);
        Money total = cashAccrual.plus(equityAccrual);

        Money expectedCash = Money.usd("12500.00").times(monthlyRate(RATE_4_5_PCT));
        Money expectedEquity = Money.usd("87500.00").times(monthlyRate(RATE_7_PCT));
        Money expectedTotal = expectedCash.plus(expectedEquity);
        assertThat(total.amount()).isCloseTo(expectedTotal.amount(), within(TOLERANCE_DOLLAR_FRACTION));
    }

    @Test
    @DisplayName("Cash sleeve over 12 months reaches the simple-rate annual ceiling within geometric tolerance")
    void cashSleeveOver12Months() {
        Money startBalance = Money.usd(TEN_THOUSAND);
        AccountSleeve sleeve =
                AccountSleeve.of(new SleeveKind.Cash(), startBalance, new SleeveYieldPolicy.MoneyMarket(RATE_4_5_PCT));

        Money endBalance = compoundFor12Months(sleeve);

        // Geometric compounding of monthly accruals must equal annual growth.
        Money expected = startBalance.times(BigDecimal.ONE.add(RATE_4_5_PCT));
        assertThat(endBalance.amount()).isCloseTo(expected.amount(), within(TOLERANCE_TWELVE_MONTH_CENTS));
    }

    @Test
    @DisplayName("Zero balance produces zero accrual regardless of rate")
    void zeroBalanceProducesZeroAccrual() {
        AccountSleeve sleeve = AccountSleeve.of(
                new SleeveKind.AssetAllocation(), Money.ZERO_USD, new SleeveYieldPolicy.FixedRate(RATE_7_PCT));

        Money accrual = engine.accruePerMonth(sleeve, JAN_2026, ASSUMPTIONS);

        assertThat(accrual).isEqualTo(Money.ZERO_USD);
    }

    @Test
    @DisplayName("Zero rate produces zero accrual regardless of balance")
    void zeroRateProducesZeroAccrual() {
        AccountSleeve sleeve = AccountSleeve.of(
                new SleeveKind.Cash(), Money.usd(TEN_THOUSAND), new SleeveYieldPolicy.FixedRate(BigDecimal.ZERO));

        Money accrual = engine.accruePerMonth(sleeve, JAN_2026, ASSUMPTIONS);

        assertThat(accrual).isEqualTo(Money.ZERO_USD);
    }

    @Test
    @DisplayName("Negative annual rate accrues a negative monthly amount")
    void negativeRateProducesNegativeAccrual() {
        AccountSleeve sleeve = AccountSleeve.of(
                new SleeveKind.AssetAllocation(),
                Money.usd(TEN_THOUSAND),
                new SleeveYieldPolicy.FixedRate(new BigDecimal("-0.10")));

        Money accrual = engine.accruePerMonth(sleeve, JAN_2026, ASSUMPTIONS);

        assertThat(accrual.amount().signum()).isNegative();
    }

    @ParameterizedTest(name = "monthlyRateFromAnnual({0}) recovers to (1+{0}) after 12 compounds")
    @ValueSource(strings = {"0.03", "0.045", "0.07", "0.10"})
    @DisplayName("monthlyRateFromAnnual recovers the annual rate to scale-6 precision after 12 compounds")
    void monthlyRateRecoversAnnual(String annualRateLiteral) {
        BigDecimal precisionTolerance = new BigDecimal("0.0000000001");
        BigDecimal annual = new BigDecimal(annualRateLiteral);
        BigDecimal monthly = SleeveYieldEngineImpl.monthlyRateFromAnnual(annual);
        BigDecimal compounded = BigDecimal.ONE.add(monthly).pow(12);
        BigDecimal expected = BigDecimal.ONE.add(annual);
        assertThat(compounded).isCloseTo(expected, within(precisionTolerance));
    }

    private Money compoundFor12Months(AccountSleeve sleeve) {
        return IntStream.range(0, 12)
                .mapToObj(JAN_2026::plusMonths)
                .reduce(
                        sleeve.balance(),
                        (balance, period) -> balance.plus(engine.accruePerMonth(
                                AccountSleeve.of(sleeve.kind(), balance, sleeve.yieldPolicy()), period, ASSUMPTIONS)),
                        (a, _) -> a);
    }

    private static BigDecimal monthlyRate(BigDecimal annual) {
        return SleeveYieldEngineImpl.monthlyRateFromAnnual(annual);
    }
}
