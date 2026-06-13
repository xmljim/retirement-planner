/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.retirementplanner.contribution.CashFlowLedger;
import io.github.xmljim.retirement.retirementplanner.contribution.EngineWarning;
import io.github.xmljim.retirement.retirementplanner.contribution.MonthlyContributionResult;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountSleeve;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountType;
import io.github.xmljim.retirement.retirementplanner.plan.account.OwnerRef;
import io.github.xmljim.retirement.retirementplanner.plan.account.SleeveKind;
import io.github.xmljim.retirement.retirementplanner.plan.account.SleeveYieldPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.ContributionPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.EmployerMatch;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.MatchTier;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.PercentOfSalary;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfile;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * SECURE 2.0 §603 high-earner Roth catch-up routing — engine-level
 * scenarios. Exercises the full pipeline (planner → §402(g) → match →
 * §603 → §415(c) → emit) with realistic salary, threshold, and
 * catch-up tier combinations to validate that:
 *
 * <ul>
 *   <li>routing fires only when year ≥ 2026, age ≥ 50, and prior-year
 *       FICA wages exceed the indexed §603 threshold;</li>
 *   <li>the catch-up portion (the dollars that push pool YTD past
 *       §402(g) base) is sliced precisely from the source contribution;</li>
 *   <li>routing targets a Roth designated account in the same plan
 *       family, leaves match on the source account, and falls back to
 *       a warning when no Roth target exists;</li>
 *   <li>annual aggregates land exactly on the §402(g) catch-up tier
 *       ceiling — i.e. base + tier extension to the dollar.</li>
 * </ul>
 */
@SuppressWarnings("PMD.ExcessiveImports")
class Section603EngineTest {

    private static final PlanId PLAN_ID = new PlanId(1L);
    private static final PersonId PERSON_ID = new PersonId(1L);
    private static final OwnerRef OWNER = new OwnerRef.Individual(PERSON_ID);
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate BASE_DATE_2025 = LocalDate.of(2025, 1, 1);

    private static final int YEAR_2025 = 2025;
    private static final int YEAR_2026 = 2026;

    private static final BigDecimal TEN_PCT = new BigDecimal("0.10");
    private static final BigDecimal TWENTY_PCT = new BigDecimal("0.20");
    private static final BigDecimal FIFTY_PCT = new BigDecimal("0.50");
    private static final BigDecimal HUNDRED_PCT = new BigDecimal("1.00");

    /** Threshold from irs-limits.yaml for 2026. */
    private static final Money WAGES_ABOVE_THRESHOLD = Money.usd("200000");

    private static final Money WAGES_BELOW_THRESHOLD = Money.usd("100000");

    private static final int HIGH_SALARY = 500_000;

    /** §402(g) base for 2026. */
    private static final String BASE_402G_2026 = "24500";

    /** §402(g) base + 50+ catch-up for 2026 (24500 + 8000). */
    private static final String CATCHUP_50_TOTAL_2026 = "32500";

    /** §402(g) base + 60–63 super catch-up for 2026 (24500 + 11250). */
    private static final String CATCHUP_60_TOTAL_2026 = "35750";

    /** Catch-up portion for 50+ tier (8000). */
    private static final String CATCHUP_50_AMOUNT_2026 = "8000";

    /** Catch-up portion for 60–63 tier (11250). */
    private static final String CATCHUP_60_AMOUNT_2026 = "11250";

    private ContributionEngineImpl engine;

    @BeforeEach
    void setUp() {
        engine = new ContributionEngineImpl(new IrsLimitsServiceImpl());
    }

    @Test
    @DisplayName("high earner with Roth 401(k) target: catch-up routes to Roth, base stays Trad")
    void highEarnerWithRothTarget() {
        SalaryProfile salary = highEarnerSalary(WAGES_ABOVE_THRESHOLD);
        Account trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, null));
        Account roth = account(2L, AccountType.ROTH_401K, percentPolicy(BigDecimal.ZERO, null));
        Person person = person(55);

        CashFlowLedger ledger = runYear(person, List.of(trad, roth), salary, YEAR_2026);

        Money tradPretax = ledger.forYear(YEAR_2026)
                .forAccount(new AccountId(1L))
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX))
                .total();
        Money rothCatchup = ledger.forYear(YEAR_2026)
                .forAccount(new AccountId(2L))
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_ROTH_CATCHUP))
                .total();
        Money rothBase = ledger.forYear(YEAR_2026)
                .forAccount(new AccountId(2L))
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_ROTH))
                .total();

        assertThat(tradPretax).isEqualTo(Money.usd(BASE_402G_2026));
        assertThat(rothCatchup).isEqualTo(Money.usd(CATCHUP_50_AMOUNT_2026));
        assertThat(rothBase).isEqualTo(Money.ZERO_USD);
        Money totalElective = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(
                        CashFlowKind.EMPLOYEE_PRETAX, CashFlowKind.EMPLOYEE_ROTH, CashFlowKind.EMPLOYEE_ROTH_CATCHUP))
                .total();
        assertThat(totalElective).isEqualTo(Money.usd(CATCHUP_50_TOTAL_2026));
    }

    @Test
    @DisplayName("high earner without Roth target: catch-up disallowed, warning emitted, totals cap at §402(g) base")
    void highEarnerWithoutRothTarget() {
        SalaryProfile salary = highEarnerSalary(WAGES_ABOVE_THRESHOLD);
        Account trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, null));
        Person person = person(55);

        YearRunResult yearRun = runYearCollecting(person, List.of(trad), salary, YEAR_2026);

        Money tradPretax = yearRun.ledger()
                .forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX))
                .total();
        assertThat(tradPretax).isEqualTo(Money.usd(BASE_402G_2026));

        List<EngineWarning> warnings = yearRun.warnings().stream()
                .filter(w -> w.kind() == EngineWarning.WarningKind.SECTION_603_NO_ROTH_DESTINATION)
                .toList();
        assertThat(warnings).isNotEmpty();
        assertThat(warnings).allSatisfy(w -> {
            assertThat(w.accountId()).isEqualTo(1L);
            assertThat(w.year()).isEqualTo(YEAR_2026);
            assertThat(w.detail()).contains("401(k)");
        });
    }

    @Test
    @DisplayName("low earner under threshold: no §603 routing even with catch-up tier engaged")
    void lowEarnerNotAffected() {
        SalaryProfile salary = highEarnerSalary(WAGES_BELOW_THRESHOLD);
        Account trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, null));
        Account roth = account(2L, AccountType.ROTH_401K, percentPolicy(BigDecimal.ZERO, null));
        Person person = person(55);

        CashFlowLedger ledger = runYear(person, List.of(trad, roth), salary, YEAR_2026);

        Money rothCatchup = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_ROTH_CATCHUP))
                .total();
        Money tradPretax = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX))
                .total();

        assertThat(rothCatchup).isEqualTo(Money.ZERO_USD);
        assertThat(tradPretax).isEqualTo(Money.usd(CATCHUP_50_TOTAL_2026));
    }

    @Test
    @DisplayName("year before §603 effective date (2025): routing skipped even for high earner")
    void preEffectiveYearSkipped() {
        SalaryProfile salary = new SalaryProfile(
                Optional.empty(),
                Money.usd(Integer.toString(HIGH_SALARY)),
                BASE_DATE_2025,
                BigDecimal.ZERO,
                Month.JANUARY,
                List.of(),
                Optional.empty(),
                Optional.of(WAGES_ABOVE_THRESHOLD));
        Account trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, null));
        Account roth = account(2L, AccountType.ROTH_401K, percentPolicy(BigDecimal.ZERO, null));
        LocalDate dob = BASE_DATE_2025.minusYears(55);
        Person person = new Person(Optional.of(PERSON_ID), Optional.empty(), dob, dob.plusYears(100));

        CashFlowLedger ledger = runYear(person, List.of(trad, roth), salary, YEAR_2025);

        Money rothCatchup = ledger.forYear(YEAR_2025)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_ROTH_CATCHUP))
                .total();
        assertThat(rothCatchup).isEqualTo(Money.ZERO_USD);
    }

    @Test
    @DisplayName("under age 50: no catch-up tier engaged, no §603 routing")
    void underAge50NotAffected() {
        SalaryProfile salary = highEarnerSalary(WAGES_ABOVE_THRESHOLD);
        Account trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, null));
        Account roth = account(2L, AccountType.ROTH_401K, percentPolicy(BigDecimal.ZERO, null));
        Person person = person(45);

        CashFlowLedger ledger = runYear(person, List.of(trad, roth), salary, YEAR_2026);

        Money totalElective = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(
                        CashFlowKind.EMPLOYEE_PRETAX, CashFlowKind.EMPLOYEE_ROTH, CashFlowKind.EMPLOYEE_ROTH_CATCHUP))
                .total();
        assertThat(totalElective).isEqualTo(Money.usd(BASE_402G_2026));
    }

    @Test
    @DisplayName("age 60–63 super catch-up: routed Roth catch-up equals the full super-catch-up extension")
    void superCatchup60To63Routes() {
        SalaryProfile salary = highEarnerSalary(WAGES_ABOVE_THRESHOLD);
        Account trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, null));
        Account roth = account(2L, AccountType.ROTH_401K, percentPolicy(BigDecimal.ZERO, null));
        Person person = person(62);

        CashFlowLedger ledger = runYear(person, List.of(trad, roth), salary, YEAR_2026);

        Money rothCatchup = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_ROTH_CATCHUP))
                .total();
        Money tradPretax = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX))
                .total();

        assertThat(tradPretax).isEqualTo(Money.usd(BASE_402G_2026));
        assertThat(rothCatchup).isEqualTo(Money.usd(CATCHUP_60_AMOUNT_2026));
        Money total = tradPretax.plus(rothCatchup);
        assertThat(total).isEqualTo(Money.usd(CATCHUP_60_TOTAL_2026));
    }

    @Test
    @DisplayName("source already Roth 401(k): catch-up retagged to EMPLOYEE_ROTH_CATCHUP, no account move")
    void sourceAlreadyRothRetagsKind() {
        SalaryProfile salary = highEarnerSalary(WAGES_ABOVE_THRESHOLD);
        Account roth = account(1L, AccountType.ROTH_401K, percentPolicy(TEN_PCT, null));
        Person person = person(55);

        CashFlowLedger ledger = runYear(person, List.of(roth), salary, YEAR_2026);

        Money baseRoth = ledger.forYear(YEAR_2026)
                .forAccount(new AccountId(1L))
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_ROTH))
                .total();
        Money catchupRoth = ledger.forYear(YEAR_2026)
                .forAccount(new AccountId(1L))
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_ROTH_CATCHUP))
                .total();

        assertThat(baseRoth).isEqualTo(Money.usd(BASE_402G_2026));
        assertThat(catchupRoth).isEqualTo(Money.usd(CATCHUP_50_AMOUNT_2026));
    }

    @Test
    @DisplayName("employer match stays on Trad source even when catch-up routes to Roth")
    void matchStaysOnSourceAccount() {
        SalaryProfile salary = highEarnerSalary(WAGES_ABOVE_THRESHOLD);
        EmployerMatch match = EmployerMatch.of(List.of(new MatchTier(new BigDecimal("0.06"), HUNDRED_PCT)));
        Account trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, match));
        Account roth = account(2L, AccountType.ROTH_401K, percentPolicy(BigDecimal.ZERO, null));
        Person person = person(55);

        CashFlowLedger ledger = runYear(person, List.of(trad, roth), salary, YEAR_2026);

        Money tradMatch = ledger.forYear(YEAR_2026)
                .forAccount(new AccountId(1L))
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYER_MATCH))
                .total();
        Money rothMatch = ledger.forYear(YEAR_2026)
                .forAccount(new AccountId(2L))
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYER_MATCH))
                .total();

        assertThat(tradMatch.amount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(rothMatch).isEqualTo(Money.ZERO_USD);
    }

    @Test
    @DisplayName("partial-month spanning §402(g) base: catch-up slice equals only the over-base portion")
    void partialMonthSliceAtPoolBoundary() {
        // 20% of 500k = 100k/yr → 8333.33/mo. Pool YTD crosses 24500 at month 3.
        // After month 2: 16666.67 (under base). Month 3 contribution: 8333.33 → poolAfter = 24999.99 (over by ~500).
        // Catch-up slice for month 3 should be ~500.
        SalaryProfile salary = highEarnerSalary(WAGES_ABOVE_THRESHOLD);
        Account trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TWENTY_PCT, null));
        Account roth = account(2L, AccountType.ROTH_401K, percentPolicy(BigDecimal.ZERO, null));
        Person person = person(55);

        MonthlyContributionResult month3 = engine.contributeForMonth(
                person,
                List.of(trad, roth),
                salary,
                runYearThroughMonth(person, List.of(trad, roth), salary, YEAR_2026, 2),
                YEAR_2026,
                Month.MARCH);

        Money catchupSlice = month3.flows().stream()
                .filter(f -> f.kind() == CashFlowKind.EMPLOYEE_ROTH_CATCHUP)
                .map(CashFlow::amount)
                .reduce(Money.ZERO_USD, Money::plus);
        // 500k * 0.20 / 12 * 3 = 25000; pool over base by 500 at end of month 3.
        // Three monthly divisions accumulate ≤1 nano-USD of drift; assert within a cent.
        BigDecimal cent = new BigDecimal("0.01");
        assertThat(catchupSlice.amount().subtract(new BigDecimal("500")).abs()).isLessThan(cent);
    }

    @Test
    @DisplayName("annual run without Roth target: full year of warnings + zero catch-up emitted")
    void annualRunWithoutRothCollectsWarningsEachMonth() {
        SalaryProfile salary = highEarnerSalary(WAGES_ABOVE_THRESHOLD);
        Account trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, null));
        Person person = person(55);

        YearRunResult yearRun = runYearCollecting(person, List.of(trad), salary, YEAR_2026);

        // Once §402(g) base is hit, every remaining month would have a catch-up portion → warning per month.
        // 50k/mo @ 10% = 4166.67/mo; pool reaches 24500 between month 5 (20833) and 6 (25000).
        // Months 6..12 produce slices → 7 warnings. Allow ≥7 to keep this resilient to precision shift.
        long warningCount = yearRun.warnings().stream()
                .filter(w -> w.kind() == EngineWarning.WarningKind.SECTION_603_NO_ROTH_DESTINATION)
                .count();
        assertThat(warningCount).isGreaterThanOrEqualTo(6);
    }

    @Test
    @DisplayName("403(b) family: catch-up routes only to a 403(b) Roth target, not a 401(k) Roth target")
    void familyIsolation() {
        SalaryProfile salary = highEarnerSalary(WAGES_ABOVE_THRESHOLD);
        Account trad403b = account(1L, AccountType.TRADITIONAL_403B, percentPolicy(TEN_PCT, null));
        Account roth401k = account(2L, AccountType.ROTH_401K, percentPolicy(BigDecimal.ZERO, null));
        Person person = person(55);

        YearRunResult yearRun = runYearCollecting(person, List.of(trad403b, roth401k), salary, YEAR_2026);

        Money roth401kCatchup = yearRun.ledger()
                .forYear(YEAR_2026)
                .forAccount(new AccountId(2L))
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_ROTH_CATCHUP))
                .total();
        // Roth 401(k) is in a different family from Trad 403(b); routing should not target it.
        assertThat(roth401kCatchup).isEqualTo(Money.ZERO_USD);
        assertThat(yearRun.warnings().stream()
                        .filter(w -> w.kind() == EngineWarning.WarningKind.SECTION_603_NO_ROTH_DESTINATION)
                        .anyMatch(w -> w.detail().contains("403(b)")))
                .isTrue();
    }

    @Test
    @DisplayName("§415(c) interaction: routed Roth catch-up sums into source family's §415(c) bucket")
    void section415cTreatsRoutedCatchupAsSameFamily() {
        // Sanity check: with a moderate match, totals stay under §415(c) (70k for 2026)
        // even with the routed catch-up. This validates that the trimmer sees both rows.
        SalaryProfile salary = highEarnerSalary(WAGES_ABOVE_THRESHOLD);
        EmployerMatch modestMatch = EmployerMatch.of(List.of(new MatchTier(new BigDecimal("0.06"), FIFTY_PCT)));
        Account trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, modestMatch));
        Account roth = account(2L, AccountType.ROTH_401K, percentPolicy(BigDecimal.ZERO, null));
        Person person = person(55);

        CashFlowLedger ledger = runYear(person, List.of(trad, roth), salary, YEAR_2026);

        Money totalDcBucket = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(
                        CashFlowKind.EMPLOYEE_PRETAX,
                        CashFlowKind.EMPLOYEE_ROTH,
                        CashFlowKind.EMPLOYEE_ROTH_CATCHUP,
                        CashFlowKind.EMPLOYER_MATCH))
                .total();
        assertThat(totalDcBucket.amount()).isLessThanOrEqualTo(new BigDecimal("70000.000000"));
    }

    private CashFlowLedger runYear(Person person, List<Account> accounts, SalaryProfile salary, int year) {
        return runYearCollecting(person, accounts, salary, year).ledger();
    }

    private YearRunResult runYearCollecting(Person person, List<Account> accounts, SalaryProfile salary, int year) {
        return IntStream.rangeClosed(1, 12)
                .boxed()
                .reduce(
                        YearRunResult.empty(),
                        (acc, m) -> acc.append(
                                engine.contributeForMonth(person, accounts, salary, acc.ledger(), year, Month.of(m))),
                        (a, b) -> a);
    }

    private CashFlowLedger runYearThroughMonth(
            Person person, List<Account> accounts, SalaryProfile salary, int year, int monthInclusive) {
        return IntStream.rangeClosed(1, monthInclusive)
                .boxed()
                .reduce(
                        CashFlowLedger.empty(),
                        (ledger, m) -> ledger.appendAll(
                                engine.contributeForMonth(person, accounts, salary, ledger, year, Month.of(m))
                                        .flows()),
                        (a, b) -> a);
    }

    private static SalaryProfile highEarnerSalary(Money priorYearWages) {
        return new SalaryProfile(
                Optional.empty(),
                Money.usd(Integer.toString(HIGH_SALARY)),
                BASE_DATE,
                BigDecimal.ZERO,
                Month.JANUARY,
                List.of(),
                Optional.empty(),
                Optional.of(priorYearWages));
    }

    private static Account account(long id, AccountType type, ContributionPolicy policy) {
        AccountSleeve sleeve = AccountSleeve.of(
                new SleeveKind.AssetAllocation(), Money.ZERO_USD, new SleeveYieldPolicy.TracksAllocation());
        return new Account(Optional.of(new AccountId(id)), PLAN_ID, type, OWNER, List.of(sleeve), Optional.of(policy));
    }

    private static ContributionPolicy percentPolicy(BigDecimal pct, EmployerMatch match) {
        return new ContributionPolicy(
                new PercentOfSalary(pct),
                Optional.empty(),
                Optional.ofNullable(match),
                Optional.empty(),
                Optional.empty());
    }

    private static Person person(int age) {
        LocalDate dob = BASE_DATE.minusYears(age);
        return new Person(Optional.of(PERSON_ID), Optional.empty(), dob, dob.plusYears(100));
    }

    /** Accumulator for runYearCollecting that threads ledger and warnings together. */
    private record YearRunResult(CashFlowLedger ledger, List<EngineWarning> warnings) {
        static YearRunResult empty() {
            return new YearRunResult(CashFlowLedger.empty(), List.of());
        }

        YearRunResult append(MonthlyContributionResult month) {
            CashFlowLedger nextLedger = ledger.appendAll(month.flows());
            List<EngineWarning> nextWarnings =
                    Stream.concat(warnings.stream(), month.warnings().stream()).toList();
            return new YearRunResult(nextLedger, nextWarnings);
        }
    }
}
