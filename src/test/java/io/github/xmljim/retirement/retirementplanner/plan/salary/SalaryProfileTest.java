/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.salary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

class SalaryProfileTest {

    private static final LocalDate JAN_1_2026 = LocalDate.of(2026, 1, 1);
    private static final BigDecimal FOUR_PCT = new BigDecimal("0.04");
    private static final String BASE = "100000";
    private static final String PROMOTION = "130000";
    private static final String FIXED_BONUS = "10000";

    @Test
    @DisplayName("salaryAt returns the base value when asked for the base date")
    void salaryAtBaseDate() {
        SalaryProfile profile = SalaryProfile.of(Money.usd(BASE), JAN_1_2026, FOUR_PCT);
        assertThat(profile.salaryAt(JAN_1_2026)).isEqualTo(Money.usd(BASE));
    }

    @Test
    @DisplayName("salaryAt holds salary flat between Jan-1 raises within the same year")
    void salaryAtMidYearNoGrowth() {
        SalaryProfile profile = SalaryProfile.of(Money.usd(BASE), JAN_1_2026, FOUR_PCT);
        assertThat(profile.salaryAt(LocalDate.of(2026, 7, 1))).isEqualTo(Money.usd(BASE));
        assertThat(profile.salaryAt(LocalDate.of(2026, 12, 31))).isEqualTo(Money.usd(BASE));
    }

    @Test
    @DisplayName("salaryAt compounds growth on each Jan-1 anniversary")
    void salaryAtCompoundedGrowth() {
        SalaryProfile profile = SalaryProfile.of(Money.usd(BASE), JAN_1_2026, FOUR_PCT);
        assertThat(profile.salaryAt(LocalDate.of(2027, 1, 1))).isEqualTo(Money.usd("104000"));
        assertThat(profile.salaryAt(LocalDate.of(2028, 1, 1))).isEqualTo(Money.usd("108160"));
        assertThat(profile.salaryAt(LocalDate.of(2029, 6, 1))).isEqualTo(Money.usd("112486.40"));
    }

    @Test
    @DisplayName("salaryAt with a single override re-anchors on the override's effective date")
    void salaryAtSingleOverride() {
        SalaryOverride promotion = new SalaryOverride(LocalDate.of(2027, 6, 1), Money.usd(PROMOTION));
        SalaryProfile profile = new SalaryProfile(
                Optional.empty(),
                Money.usd(BASE),
                JAN_1_2026,
                FOUR_PCT,
                Month.JANUARY,
                List.of(promotion),
                Optional.empty());
        assertThat(profile.salaryAt(LocalDate.of(2027, 5, 31))).isEqualTo(Money.usd("104000"));
        assertThat(profile.salaryAt(LocalDate.of(2027, 6, 1))).isEqualTo(Money.usd(PROMOTION));
        assertThat(profile.salaryAt(LocalDate.of(2027, 12, 31))).isEqualTo(Money.usd(PROMOTION));
        assertThat(profile.salaryAt(LocalDate.of(2028, 1, 1))).isEqualTo(Money.usd("135200"));
    }

    @Test
    @DisplayName("salaryAt growth resumes after each override")
    void salaryAtMultipleOverrides() {
        SalaryOverride first = new SalaryOverride(LocalDate.of(2027, 6, 1), Money.usd(PROMOTION));
        SalaryOverride second = new SalaryOverride(LocalDate.of(2030, 3, 15), Money.usd("180000"));
        SalaryProfile profile = new SalaryProfile(
                Optional.empty(),
                Money.usd(BASE),
                JAN_1_2026,
                FOUR_PCT,
                Month.JANUARY,
                List.of(first, second),
                Optional.empty());
        assertThat(profile.salaryAt(LocalDate.of(2030, 1, 1))).isEqualTo(Money.usd("146232.32"));
        assertThat(profile.salaryAt(LocalDate.of(2030, 3, 15))).isEqualTo(Money.usd("180000"));
        assertThat(profile.salaryAt(LocalDate.of(2031, 1, 1))).isEqualTo(Money.usd("187200"));
        assertThat(profile.salaryAt(LocalDate.of(2032, 1, 1))).isEqualTo(Money.usd("194688"));
    }

    @Test
    @DisplayName("salaryAt honors a non-January raise month")
    void salaryAtCustomRaiseMonth() {
        SalaryProfile profile = new SalaryProfile(
                Optional.empty(), Money.usd(BASE), JAN_1_2026, FOUR_PCT, Month.JULY, List.of(), Optional.empty());
        assertThat(profile.salaryAt(LocalDate.of(2026, 6, 30))).isEqualTo(Money.usd(BASE));
        assertThat(profile.salaryAt(LocalDate.of(2026, 7, 1))).isEqualTo(Money.usd("104000"));
        assertThat(profile.salaryAt(LocalDate.of(2027, 7, 1))).isEqualTo(Money.usd("108160"));
    }

    @Test
    @DisplayName("salaryAt rejects dates before the base date")
    void salaryAtRejectsPreBaseDate() {
        SalaryProfile profile = SalaryProfile.of(Money.usd(BASE), JAN_1_2026, FOUR_PCT);
        assertThatThrownBy(() -> profile.salaryAt(LocalDate.of(2025, 12, 31)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("bonusFor pays a fixed bonus only in the configured month")
    void fixedBonusPayoutMonth() {
        BonusPolicy bonus = new FixedBonus(Money.usd(FIXED_BONUS), Month.MARCH);
        SalaryProfile profile = new SalaryProfile(
                Optional.empty(), Money.usd(BASE), JAN_1_2026, FOUR_PCT, Month.JANUARY, List.of(), Optional.of(bonus));
        assertThat(profile.bonusFor(YearMonth.of(2026, 3))).contains(Money.usd(FIXED_BONUS));
        assertThat(profile.bonusFor(YearMonth.of(2027, 3))).contains(Money.usd(FIXED_BONUS));
        assertThat(profile.bonusFor(YearMonth.of(2026, 4))).isEmpty();
    }

    @Test
    @DisplayName("bonusFor scales a percentage bonus by the salary at payout")
    void percentBonusUsesCurrentSalary() {
        BonusPolicy bonus = new PercentOfSalaryBonus(new BigDecimal("0.10"), Month.MARCH);
        SalaryProfile profile = new SalaryProfile(
                Optional.empty(), Money.usd(BASE), JAN_1_2026, FOUR_PCT, Month.JANUARY, List.of(), Optional.of(bonus));
        assertThat(profile.bonusFor(YearMonth.of(2026, 3))).contains(Money.usd(FIXED_BONUS));
        assertThat(profile.bonusFor(YearMonth.of(2027, 3))).contains(Money.usd("10400"));
    }

    @Test
    @DisplayName("bonusFor returns empty when no bonus policy is configured")
    void bonusForEmptyWhenAbsent() {
        SalaryProfile profile = SalaryProfile.of(Money.usd(BASE), JAN_1_2026, FOUR_PCT);
        assertThat(profile.bonusFor(YearMonth.of(2026, 3))).isEmpty();
    }

    @Test
    @DisplayName("bonusFor returns empty for months before the base date")
    void bonusForPreBaseDate() {
        BonusPolicy bonus = new FixedBonus(Money.usd(FIXED_BONUS), Month.MARCH);
        SalaryProfile profile = new SalaryProfile(
                Optional.empty(), Money.usd(BASE), JAN_1_2026, FOUR_PCT, Month.JANUARY, List.of(), Optional.of(bonus));
        assertThat(profile.bonusFor(YearMonth.of(2025, 3))).isEmpty();
    }

    @Test
    @DisplayName("SalaryProfile rejects negative growth rates")
    void rejectsNegativeGrowth() {
        assertThatThrownBy(() -> SalaryProfile.of(Money.usd(BASE), JAN_1_2026, new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SalaryProfile rejects overrides effective before the base date")
    void rejectsOverrideBeforeBaseDate() {
        SalaryOverride bad = new SalaryOverride(LocalDate.of(2025, 12, 31), Money.usd("110000"));
        assertThatThrownBy(() -> new SalaryProfile(
                        Optional.empty(),
                        Money.usd(BASE),
                        JAN_1_2026,
                        FOUR_PCT,
                        Month.JANUARY,
                        List.of(bad),
                        Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SalaryProfile defensively copies the overrides list")
    void copiesOverrides() {
        SalaryProfile profile = SalaryProfile.of(Money.usd(BASE), JAN_1_2026, FOUR_PCT);
        assertThatThrownBy(() ->
                        profile.overrides().add(new SalaryOverride(LocalDate.of(2027, 1, 1), Money.usd("110000"))))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
