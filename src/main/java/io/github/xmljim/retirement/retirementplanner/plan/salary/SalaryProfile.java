/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.salary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Salary timeline for a single {@link Person} (ADR-003, FR-1.4).
 *
 * <p>Salary at any date is piecewise-defined:
 * <ul>
 *   <li>{@link #currentSalary()} is the salary as of {@link #baseDate()}.</li>
 *   <li>Each {@link SalaryOverride} re-anchors salary to its
 *       {@link SalaryOverride#newSalary()} on its
 *       {@link SalaryOverride#effectiveDate()}.</li>
 *   <li>Between anchors, salary grows by {@link #annualGrowthRate()}
 *       compounded once per year on {@link #raiseMonth()} day 1.
 *       The default raise month is January, matching most employer
 *       review cycles; non-standard schedules supply another month.</li>
 * </ul>
 *
 * <p>Optional {@link BonusPolicy} pays once per year in its configured
 * month, on top of regular salary.
 *
 * <p>{@link #priorYearFicaWages()} is the user-supplied baseline for
 * the simulation's first contribution year — typically a copy of the
 * most recent W-2 Box 3. It exists for {@link #priorYearWagesFor(int)}
 * to answer the SECURE 2.0 §603 high-earner test in year 0, where no
 * prior simulated year exists. When absent, the engine back-derives
 * an approximation from {@link #currentSalary()} and
 * {@link #annualGrowthRate()}; users straddling the §603 threshold
 * should supply the explicit baseline.
 *
 * <p>{@code id} is absent on a freshly constructed profile that has
 * not yet been persisted; the repository populates it on save.
 */
public record SalaryProfile(
        Optional<SalaryProfileId> id,
        Money currentSalary,
        LocalDate baseDate,
        BigDecimal annualGrowthRate,
        Month raiseMonth,
        List<SalaryOverride> overrides,
        Optional<BonusPolicy> bonus,
        Optional<Money> priorYearFicaWages) {

    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    public SalaryProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(currentSalary, "currentSalary");
        Objects.requireNonNull(baseDate, "baseDate");
        Objects.requireNonNull(annualGrowthRate, "annualGrowthRate");
        Objects.requireNonNull(raiseMonth, "raiseMonth");
        Objects.requireNonNull(overrides, "overrides");
        Objects.requireNonNull(bonus, "bonus");
        Objects.requireNonNull(priorYearFicaWages, "priorYearFicaWages");
        if (annualGrowthRate.signum() < 0) {
            throw new IllegalArgumentException("annualGrowthRate must be non-negative: " + annualGrowthRate);
        }
        priorYearFicaWages.filter(w -> w.amount().signum() < 0).ifPresent(w -> {
            throw new IllegalArgumentException("priorYearFicaWages must be non-negative: " + w);
        });
        overrides.stream()
                .filter(o -> o.effectiveDate().isBefore(baseDate))
                .findAny()
                .ifPresent(o -> {
                    throw new IllegalArgumentException(
                            "override effectiveDate " + o.effectiveDate() + " precedes baseDate " + baseDate);
                });
        overrides = List.copyOf(overrides);
    }

    /** Convenience constructor: January raise month, no bonus, no overrides, no priorYearFicaWages. */
    public static SalaryProfile of(Money currentSalary, LocalDate baseDate, BigDecimal annualGrowthRate) {
        return new SalaryProfile(
                Optional.empty(),
                currentSalary,
                baseDate,
                annualGrowthRate,
                Month.JANUARY,
                List.of(),
                Optional.empty(),
                Optional.empty());
    }

    /**
     * Returns the salary in effect on {@code asOf}. The profile's most
     * recent anchor (latest override on or before {@code asOf}, or
     * {@link #baseDate()} if none) supplies the base; salary then grows
     * by {@link #annualGrowthRate()} once per year on
     * {@link #raiseMonth()} day 1, counting only raise dates strictly
     * after the anchor and on or before {@code asOf}.
     *
     * @throws IllegalArgumentException if {@code asOf} is before
     *                                  {@link #baseDate()}
     */
    public Money salaryAt(LocalDate asOf) {
        Objects.requireNonNull(asOf, "asOf");
        if (asOf.isBefore(baseDate)) {
            throw new IllegalArgumentException("asOf " + asOf + " is before baseDate " + baseDate);
        }
        Optional<SalaryOverride> latestPriorOverride = overrides.stream()
                .filter(o -> !o.effectiveDate().isAfter(asOf))
                .max(Comparator.comparing(SalaryOverride::effectiveDate));
        LocalDate anchorDate =
                latestPriorOverride.map(SalaryOverride::effectiveDate).orElse(baseDate);
        Money anchorSalary = latestPriorOverride.map(SalaryOverride::newSalary).orElse(currentSalary);
        int raiseCount = countRaises(anchorDate, asOf);
        if (raiseCount == 0) {
            return anchorSalary;
        }
        BigDecimal multiplier = BigDecimal.ONE.add(annualGrowthRate).pow(raiseCount);
        return anchorSalary.times(multiplier);
    }

    /**
     * Returns the wages used to test SECURE 2.0 §603 eligibility for
     * contribution year {@code contributionYear} — i.e. the wages
     * earned in {@code contributionYear - 1}.
     *
     * <p>Sourcing rules (per ADR-003):
     * <ul>
     *   <li>If {@code contributionYear - 1} is at or after the year of
     *       {@link #baseDate()}, integrates from the salary stream:
     *       12 × monthly salary at end of the prior year, plus any
     *       bonus paid in the prior year. This is exact for years the
     *       simulation has projected.</li>
     *   <li>Otherwise (the simulation's first year, where no prior
     *       year is in scope), returns {@link #priorYearFicaWages()}
     *       when present (user-supplied baseline — typically the most
     *       recent W-2 Box 3). When absent, back-derives an
     *       approximation as {@code currentSalary / (1 + annualGrowthRate)}.
     *       The approximation assumes a single annual raise at the
     *       configured rate and ignores bonuses; users straddling the
     *       §603 threshold should supply
     *       {@link #priorYearFicaWages()} explicitly.</li>
     * </ul>
     */
    public Money priorYearWagesFor(int contributionYear) {
        int priorYear = contributionYear - 1;
        if (priorYear < baseDate.getYear()) {
            return priorYearFicaWages.orElseGet(this::backDerivedPriorYearWages);
        }
        Money annualizedSum = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> YearMonth.of(priorYear, m))
                .filter(ym -> !ym.atEndOfMonth().isBefore(baseDate))
                .map(ym -> salaryAt(ym.atEndOfMonth()))
                .reduce(Money.ZERO_USD, Money::plus);
        Money monthlySum = annualizedSum.dividedBy(MONTHS_PER_YEAR);
        Money bonusPaid = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> bonusFor(YearMonth.of(priorYear, m)))
                .flatMap(Optional::stream)
                .reduce(Money.ZERO_USD, Money::plus);
        return monthlySum.plus(bonusPaid);
    }

    private Money backDerivedPriorYearWages() {
        BigDecimal divisor = BigDecimal.ONE.add(annualGrowthRate);
        if (divisor.signum() <= 0) {
            return currentSalary;
        }
        return currentSalary.dividedBy(divisor);
    }

    /**
     * Returns the bonus paid in the given calendar month, if any. Empty
     * when the profile has no bonus, the month is not the configured
     * payout month, or {@code yearMonth} is before {@link #baseDate()}.
     */
    public Optional<Money> bonusFor(YearMonth yearMonth) {
        Objects.requireNonNull(yearMonth, "yearMonth");
        LocalDate firstOfMonth = yearMonth.atDay(1);
        if (firstOfMonth.isBefore(YearMonth.from(baseDate).atDay(1))) {
            return Optional.empty();
        }
        return bonus.filter(b -> b.payoutMonth() == yearMonth.getMonth())
                .map(b -> b.payout(salaryAt(maxDate(firstOfMonth, baseDate))));
    }

    private static LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private int countRaises(LocalDate anchor, LocalDate asOf) {
        return (int) IntStream.rangeClosed(anchor.getYear(), asOf.getYear())
                .mapToObj(y -> LocalDate.of(y, raiseMonth, 1))
                .filter(d -> d.isAfter(anchor) && !d.isAfter(asOf))
                .count();
    }
}
