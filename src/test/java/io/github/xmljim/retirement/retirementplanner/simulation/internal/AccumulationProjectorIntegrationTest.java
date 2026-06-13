/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.xmljim.retirement.retirementplanner.accumulation.SleeveYieldEngine;
import io.github.xmljim.retirement.retirementplanner.contribution.ContributionEngine;
import io.github.xmljim.retirement.retirementplanner.plan.Assumptions;
import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountSleeve;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountType;
import io.github.xmljim.retirement.retirementplanner.plan.account.OwnerRef;
import io.github.xmljim.retirement.retirementplanner.plan.account.SleeveId;
import io.github.xmljim.retirement.retirementplanner.plan.account.SleeveKind;
import io.github.xmljim.retirement.retirementplanner.plan.account.SleeveYieldPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.ContributionPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.EmployerMatch;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.MatchTier;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.PercentOfSalary;
import io.github.xmljim.retirement.retirementplanner.plan.household.FilingStatus;
import io.github.xmljim.retirement.retirementplanner.plan.household.Household;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfile;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;
import io.github.xmljim.retirement.retirementplanner.shared.Money;
import io.github.xmljim.retirement.retirementplanner.simulation.AccountBalance;
import io.github.xmljim.retirement.retirementplanner.simulation.MonthlyProjection;
import io.github.xmljim.retirement.retirementplanner.simulation.ProjectionPhase;

// Integration tests legitimately couple to the full domain surface — Plan, Account, Sleeve, ContributionPolicy,
// SalaryProfile, multiple sealed-interface variants — so the import count exceeds the project default. Refactoring
// to a fixture helper class would obscure the test data without improving maintainability.
@SuppressWarnings("PMD.ExcessiveImports")
@Testcontainers
@SpringBootTest
class AccumulationProjectorIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("PMD.MutableStaticState") // Testcontainers requires @Container fields to be static
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("retirement_planner")
            .withUsername("retirement")
            .withPassword("retirement");

    private static final PlanId PLAN_ID = new PlanId(1L);
    private static final PersonId PERSON_ID = new PersonId(10L);
    private static final AccountId ACCOUNT_401K = new AccountId(100L);
    private static final AccountId ACCOUNT_IRA = new AccountId(101L);
    private static final SleeveId SLEEVE_401K = new SleeveId(1000L);
    private static final SleeveId SLEEVE_IRA = new SleeveId(1001L);
    private static final BigDecimal RETURN_RATE_7PCT = new BigDecimal("0.07");
    private static final BigDecimal CASH_RATE_4PCT = new BigDecimal("0.04");
    private static final Assumptions ASSUMPTIONS = new Assumptions(RETURN_RATE_7PCT, CASH_RATE_4PCT);
    private static final BigDecimal ONE_PERCENT_TOLERANCE = new BigDecimal("0.01");
    private static final String ONE_HUNDRED_K = "100000.00";

    @Autowired
    private ContributionEngine contributionEngine;

    @Autowired
    private SleeveYieldEngine sleeveYieldEngine;

    private AccumulationProjectorImpl projector() {
        return new AccumulationProjectorImpl(contributionEngine, sleeveYieldEngine);
    }

    @Test
    @DisplayName("Single-account compounding tracks Sheet2's annual single-rate model within 1%")
    void sheetTwoFidelitySingleAccount() {
        // Sheet2 model: $200,000 starting balance, 7% annual, 30 years, no contributions.
        // Reference closed-form: 200000 * (1.07)^30 ≈ 1,522,451.96
        Money startBalance = Money.usd("200000.00");
        LocalDate today = LocalDate.of(2026, 1, 1);
        LocalDate retirement = today.plusYears(30);
        Person person = new Person(Optional.of(PERSON_ID), Optional.empty(), today.minusYears(35), retirement);
        Plan plan = new Plan(
                Optional.of(PLAN_ID), 1L, Household.of(FilingStatus.SINGLE, "VA"), List.of(person), ASSUMPTIONS);
        Account account = noContributionAccount(ACCOUNT_401K, SLEEVE_401K, AccountType.TRADITIONAL_401K, startBalance);

        List<MonthlyProjection> projections =
                projector().project(plan, List.of(account), Map.of(), YearMonth.from(today));

        assertThat(projections).hasSize(361); // Jan 2026 .. Jan 2056 inclusive
        Money expected = Money.usd("1522451.96");
        Money actual = lastBalance(projections, ACCOUNT_401K);
        BigDecimal deviation = actual.amount()
                .subtract(expected.amount())
                .abs()
                .divide(expected.amount(), 6, java.math.RoundingMode.HALF_EVEN);
        assertThat(deviation).isLessThan(ONE_PERCENT_TOLERANCE);
        assertThat(projections.getFirst().phase()).isEqualTo(ProjectionPhase.ACCUMULATION);
    }

    @Test
    @DisplayName("Multi-account fixture with employer match produces expected retirement-date balances")
    void multiAccountWithMatch() {
        // 5-year horizon. 401(k) starts at $100k, 6% employee, 100% on first 5%, plus IRA at $50k both compounding 7%.
        // Salary $120,000 flat, no growth.
        Money start401k = Money.usd(ONE_HUNDRED_K);
        Money startIra = Money.usd("50000.00");
        Money salary = Money.usd("120000.00");
        LocalDate today = LocalDate.of(2026, 1, 1);
        LocalDate retirement = today.plusYears(5);
        Person person = new Person(Optional.of(PERSON_ID), Optional.empty(), today.minusYears(40), retirement);
        Plan plan = new Plan(
                Optional.of(PLAN_ID), 1L, Household.of(FilingStatus.SINGLE, "VA"), List.of(person), ASSUMPTIONS);

        ContributionPolicy policy = new ContributionPolicy(
                new PercentOfSalary(new BigDecimal("0.06")),
                Optional.empty(),
                Optional.of(EmployerMatch.of(List.of(new MatchTier(new BigDecimal("0.05"), BigDecimal.ONE)))),
                Optional.empty(),
                Optional.empty());
        Account account401k = new Account(
                Optional.of(ACCOUNT_401K),
                PLAN_ID,
                AccountType.TRADITIONAL_401K,
                new OwnerRef.Individual(PERSON_ID),
                List.of(new AccountSleeve(
                        Optional.of(SLEEVE_401K),
                        new SleeveKind.AssetAllocation(),
                        start401k,
                        new SleeveYieldPolicy.TracksAllocation())),
                Optional.of(policy));
        Account accountIra = noContributionAccount(ACCOUNT_IRA, SLEEVE_IRA, AccountType.TRADITIONAL_IRA, startIra);

        SalaryProfile profile = new SalaryProfile(
                Optional.empty(),
                salary,
                today,
                BigDecimal.ZERO,
                Month.JANUARY,
                List.of(),
                Optional.empty(),
                Optional.of(salary));

        List<MonthlyProjection> projections = projector()
                .project(plan, List.of(account401k, accountIra), Map.of(PERSON_ID, profile), YearMonth.from(today));

        // 60 months Jan 2026 .. Jan 2031 inclusive (5 years × 12 + 1)
        assertThat(projections).hasSize(61);

        // IRA is yield-only: 50000 * (1.07)^5 ≈ 70,127.50; allow ≤1% deviation.
        Money iraExpected = Money.usd("70127.50");
        Money iraActual = lastBalance(projections, ACCOUNT_IRA);
        assertThat(deviationPct(iraActual, iraExpected)).isLessThan(ONE_PERCENT_TOLERANCE);

        // 401(k) annual contribution = $7,200 employee + $6,000 match = $13,200/yr.
        // Reference closed-form (start-of-month yield, contribution at end of month):
        // FV = start*(1.07)^5 + sum_{i=1..60} (1100 * (1+r_m)^(60-i)) where r_m = (1.07)^(1/12)-1.
        // Closed-form value ≈ 222,019.
        Money fourOhOneKActual = lastBalance(projections, ACCOUNT_401K);
        assertThat(fourOhOneKActual.amount()).isGreaterThan(new BigDecimal("215000"));
        assertThat(fourOhOneKActual.amount()).isLessThan(new BigDecimal("230000"));

        // Cash-flow ledger sanity: 60 months × ($600 employee + $500 match) = $66,000 total contributions.
        Money totalEmployee = projections.stream()
                .flatMap(p -> p.cashFlows().stream())
                .filter(f -> f.kind() == CashFlowKind.EMPLOYEE_PRETAX)
                .map(CashFlow::amount)
                .reduce(Money.ZERO_USD, Money::plus);
        Money totalMatch = projections.stream()
                .flatMap(p -> p.cashFlows().stream())
                .filter(f -> f.kind() == CashFlowKind.EMPLOYER_MATCH)
                .map(CashFlow::amount)
                .reduce(Money.ZERO_USD, Money::plus);
        assertThat(totalEmployee.amount()).isEqualByComparingTo("36000.00");
        assertThat(totalMatch.amount()).isEqualByComparingTo("30000.00");
    }

    @Test
    @DisplayName("Person stops contributing at retirement; yield continues to horizon")
    void retirementStopsContributionsButNotYield() {
        Money startBalance = Money.usd(ONE_HUNDRED_K);
        LocalDate today = LocalDate.of(2026, 1, 1);
        LocalDate retire = today.plusYears(2);
        // Project to 2 years; no contribution flows expected at all when retirement = horizon.
        Person person = new Person(Optional.of(PERSON_ID), Optional.empty(), today.minusYears(50), retire);
        Plan plan = new Plan(
                Optional.of(PLAN_ID), 1L, Household.of(FilingStatus.SINGLE, "VA"), List.of(person), ASSUMPTIONS);
        Account account = noContributionAccount(ACCOUNT_401K, SLEEVE_401K, AccountType.TRADITIONAL_401K, startBalance);

        SalaryProfile profile = SalaryProfile.of(Money.usd(ONE_HUNDRED_K), today, BigDecimal.ZERO);

        List<MonthlyProjection> projections =
                projector().project(plan, List.of(account), Map.of(PERSON_ID, profile), YearMonth.from(today));

        // Last "actively contributing" month is the month before retirement; from retirement onward, no flows.
        long flowsAfterRetirement = projections.stream()
                .filter(p -> !p.period().atDay(1).isBefore(retire))
                .flatMap(p -> p.cashFlows().stream())
                .count();
        assertThat(flowsAfterRetirement).isZero();
        // Horizon is the retirement YearMonth — projection includes that month.
        assertThat(projections.getLast().period()).isEqualTo(YearMonth.from(retire));
    }

    @Test
    @DisplayName("Different per-person retirement dates: horizon = max, contributions stop independently")
    void perPersonRetirementHorizons() {
        Money start = Money.usd(ONE_HUNDRED_K);
        LocalDate today = LocalDate.of(2026, 1, 1);
        PersonId personA = new PersonId(10L);
        PersonId personB = new PersonId(11L);
        Person a = new Person(Optional.of(personA), Optional.empty(), today.minusYears(60), today.plusYears(2));
        Person b = new Person(Optional.of(personB), Optional.empty(), today.minusYears(50), today.plusYears(5));
        Plan plan = new Plan(
                Optional.of(PLAN_ID),
                1L,
                Household.of(FilingStatus.MARRIED_FILING_JOINTLY, "VA"),
                List.of(a, b),
                ASSUMPTIONS);
        Account accountA = noContributionAccount(
                new AccountId(200L), new SleeveId(2000L), AccountType.TRADITIONAL_IRA, start, personA);
        Account accountB = noContributionAccount(
                new AccountId(201L), new SleeveId(2001L), AccountType.TRADITIONAL_IRA, start, personB);

        List<MonthlyProjection> projections =
                projector().project(plan, List.of(accountA, accountB), Map.of(), YearMonth.from(today));

        // Horizon = max retirement = today + 5 years => 61 months.
        assertThat(projections).hasSize(61);
        // Both accounts compound across the entire horizon; person A's earlier retirement only matters once
        // contributions exist.
        Money expectedFiveYearGrowth =
                start.times(BigDecimal.ONE.add(RETURN_RATE_7PCT).pow(5));
        Money lastA = lastBalance(projections, new AccountId(200L));
        Money lastB = lastBalance(projections, new AccountId(201L));
        assertThat(deviationPct(lastA, expectedFiveYearGrowth)).isLessThan(ONE_PERCENT_TOLERANCE);
        assertThat(deviationPct(lastB, expectedFiveYearGrowth)).isLessThan(ONE_PERCENT_TOLERANCE);
    }

    @Test
    @DisplayName("Empty plan with start after retirement returns no projections")
    void emptyWhenStartAfterRetirement() {
        LocalDate today = LocalDate.of(2026, 1, 1);
        Person person = new Person(Optional.of(PERSON_ID), Optional.empty(), today.minusYears(60), today.minusYears(1));
        Plan plan = new Plan(
                Optional.of(PLAN_ID), 1L, Household.of(FilingStatus.SINGLE, "VA"), List.of(person), ASSUMPTIONS);

        List<MonthlyProjection> projections = projector().project(plan, List.of(), Map.of(), YearMonth.from(today));

        assertThat(projections).isEmpty();
    }

    private static Account noContributionAccount(
            AccountId accountId, SleeveId sleeveId, AccountType type, Money balance) {
        return noContributionAccount(accountId, sleeveId, type, balance, PERSON_ID);
    }

    private static Account noContributionAccount(
            AccountId accountId, SleeveId sleeveId, AccountType type, Money balance, PersonId owner) {
        return new Account(
                Optional.of(accountId),
                PLAN_ID,
                type,
                new OwnerRef.Individual(owner),
                List.of(new AccountSleeve(
                        Optional.of(sleeveId),
                        new SleeveKind.AssetAllocation(),
                        balance,
                        new SleeveYieldPolicy.TracksAllocation())),
                Optional.empty());
    }

    private static Money lastBalance(List<MonthlyProjection> projections, AccountId accountId) {
        return projections.getLast().accountBalances().stream()
                .filter(b -> b.accountId().equals(accountId))
                .map(AccountBalance::endingBalance)
                .findFirst()
                .orElseThrow();
    }

    private static BigDecimal deviationPct(Money actual, Money expected) {
        return actual.amount()
                .subtract(expected.amount())
                .abs()
                .divide(expected.amount(), 6, java.math.RoundingMode.HALF_EVEN);
    }
}
