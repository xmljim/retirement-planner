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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.retirementplanner.contribution.CashFlowLedger;
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
import io.github.xmljim.retirement.retirementplanner.plan.contribution.EscalationPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.FixedDollar;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.MatchTier;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.PercentOfSalary;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.salary.FixedBonus;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfile;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Engine integration test: composes Account, Person, SalaryProfile,
 * ContributionPolicy + variants, EmployerMatch, MatchTier, IRS limits,
 * and CashFlow types. The wide import surface is intrinsic to what is
 * being tested — splitting it across helper files would obscure the
 * scenario, not improve it.
 */
@SuppressWarnings("PMD.ExcessiveImports")
class ContributionEngineImplTest {

    private static final PlanId PLAN_ID = new PlanId(1L);
    private static final PersonId PERSON_ID = new PersonId(1L);
    private static final OwnerRef OWNER = new OwnerRef.Individual(PERSON_ID);
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 1, 1);
    private static final int YEAR_2026 = 2026;
    private static final int YEAR_2027 = 2027;
    private static final BigDecimal SIX_PCT = new BigDecimal("0.06");
    private static final BigDecimal FIVE_PCT = new BigDecimal("0.05");
    private static final BigDecimal TEN_PCT = new BigDecimal("0.10");
    private static final BigDecimal THREE_PCT = new BigDecimal("0.03");
    private static final BigDecimal HUNDRED_PCT = new BigDecimal("1.00");
    private static final BigDecimal HALF = new BigDecimal("0.50");
    private static final String SECTION_402G_LIMIT = "24500";
    private static final int HIGH_SALARY = 500_000;
    private static final int MID_SALARY = 120_000;

    private ContributionEngineImpl engine;

    @BeforeEach
    void setUp() {
        engine = new ContributionEngineImpl(new IrsLimitsServiceImpl());
    }

    @Test
    @DisplayName("baseline: single 401(k), no caps binding, employee + match flow emitted")
    void baselineSingle401kWithMatch() {
        SalaryProfile salary = salary(MID_SALARY, 0);
        Account account = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(SIX_PCT, simpleMatch()));
        Person person = person(40);

        MonthlyContributionResult result = engine.contributeForMonth(
                person, List.of(account), salary, CashFlowLedger.empty(), YEAR_2026, Month.JANUARY);

        assertThat(result.flows()).hasSize(2);
        assertThat(result.warnings()).isEmpty();
        assertThat(flowAmount(result.flows(), CashFlowKind.EMPLOYEE_PRETAX)).isEqualTo(monthly(MID_SALARY, "0.06"));
        assertThat(flowAmount(result.flows(), CashFlowKind.EMPLOYER_MATCH)).isEqualTo(monthly(MID_SALARY, "0.04"));
    }

    @Test
    @DisplayName("§402(g) cap binds: high earner over base limit → annual deferral truncated")
    void section402gCapBinds() {
        SalaryProfile salary = salary(HIGH_SALARY, 0);
        Account account = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, null));
        Person person = person(40);

        CashFlowLedger ledger = runYear(person, List.of(account), salary, YEAR_2026);
        Money totalEmployee = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX))
                .total();
        assertThat(totalEmployee).isEqualTo(Money.usd(SECTION_402G_LIMIT));
    }

    @Test
    @DisplayName("Trad + Roth 401(k) at same employer share §402(g) cap")
    void tradAndRoth401kShareSection402g() {
        SalaryProfile salary = salary(HIGH_SALARY, 0);
        Account trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(SIX_PCT, null));
        Account roth = account(2L, AccountType.ROTH_401K, percentPolicy(SIX_PCT, null));
        Person person = person(40);

        CashFlowLedger ledger = runYear(person, List.of(trad, roth), salary, YEAR_2026);
        Money totalElective = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX, CashFlowKind.EMPLOYEE_ROTH))
                .total();
        assertThat(totalElective).isEqualTo(Money.usd(SECTION_402G_LIMIT));
        assertThat(ledger.forYear(YEAR_2026)
                        .forAccount(new AccountId(1L))
                        .total()
                        .amount())
                .isGreaterThan(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Trad + Roth IRA share §408 cap")
    void tradAndRothIraShareSection408() {
        SalaryProfile salary = salary(MID_SALARY, 0);
        Account tradIra = account(1L, AccountType.TRADITIONAL_IRA, percentPolicy(FIVE_PCT, null));
        Account rothIra = account(2L, AccountType.ROTH_IRA, percentPolicy(FIVE_PCT, null));
        Person person = person(40);

        CashFlowLedger ledger = runYear(person, List.of(tradIra, rothIra), salary, YEAR_2026);
        Money totalIra = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_TRADITIONAL_IRA, CashFlowKind.EMPLOYEE_ROTH))
                .total();
        assertThat(totalIra).isEqualTo(Money.usd("7500"));
    }

    @Test
    @DisplayName("age 50+ catch-up extends §402(g) headroom")
    void age50PlusCatchupExtendsSection402g() {
        SalaryProfile salary = salary(HIGH_SALARY, 0);
        Account account = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, null));
        Person person = person(55);

        CashFlowLedger ledger = runYear(person, List.of(account), salary, YEAR_2026);
        Money totalEmployee = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX))
                .total();
        assertThat(totalEmployee).isEqualTo(Money.usd("32500"));
    }

    @Test
    @DisplayName("escalation: rate increases at year boundaries, capped")
    void escalationCapsAtCeiling() {
        SalaryProfile salary = salary(MID_SALARY, 0);
        Account account =
                account(1L, AccountType.TRADITIONAL_401K, percentPolicyEscalating(THREE_PCT, startedYear(YEAR_2026)));

        Money y2026 = totalEmployeeFor(account, salary, person(40), YEAR_2026);
        Money y2027 = totalEmployeeFor(account, salary, person(40), YEAR_2027);
        Money y2028 = totalEmployeeFor(account, salary, person(40), 2028);

        assertThat(y2026).isEqualTo(Money.usd("3600"));
        assertThat(y2027).isEqualTo(Money.usd("4800"));
        assertThat(y2028).isEqualTo(Money.usd("6000"));
    }

    @Test
    @DisplayName("bonus month: deferral applies to regular salary + bonus")
    void bonusMonthDeferralIncludesBonus() {
        SalaryProfile salary = salaryWithBonus(MID_SALARY, "10000", Month.MARCH);
        Account account = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(SIX_PCT, null));
        Person person = person(40);

        List<CashFlow> jan = engine.contributeForMonth(
                        person, List.of(account), salary, CashFlowLedger.empty(), YEAR_2026, Month.JANUARY)
                .flows();
        List<CashFlow> mar = engine.contributeForMonth(
                        person, List.of(account), salary, CashFlowLedger.empty(), YEAR_2026, Month.MARCH)
                .flows();

        Money janEmployee = flowAmount(jan, CashFlowKind.EMPLOYEE_PRETAX);
        Money marEmployee = flowAmount(mar, CashFlowKind.EMPLOYEE_PRETAX);
        assertThat(marEmployee.amount()).isGreaterThan(janEmployee.amount());
        assertThat(marEmployee).isEqualTo(janEmployee.plus(Money.usd("10000").times(SIX_PCT)));
    }

    @Test
    @DisplayName("year boundary: ledger query for new year returns no YTD → fresh headroom")
    void yearBoundaryResetsHeadroom() {
        SalaryProfile salary = salary(HIGH_SALARY, 0);
        Account account = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, null));
        Person person = person(40);

        CashFlowLedger ledger = runYear(person, List.of(account), salary, YEAR_2026);
        assertThat(ledger.forYear(YEAR_2026)
                        .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX))
                        .total())
                .isEqualTo(Money.usd(SECTION_402G_LIMIT));

        List<CashFlow> jan2027 = engine.contributeForMonth(
                        person, List.of(account), salary, ledger, YEAR_2027, Month.JANUARY)
                .flows();
        assertThat(flowAmount(jan2027, CashFlowKind.EMPLOYEE_PRETAX)).isEqualTo(monthly(HIGH_SALARY, "0.10"));
    }

    @Test
    @DisplayName("HSA 55+ catch-up extends §223 headroom")
    void hsa55PlusCatchup() {
        SalaryProfile salary = salary(MID_SALARY, 0);
        Account account = account(1L, AccountType.HSA, ContributionPolicy.of(new FixedDollar(Money.usd("10000"))));
        Person person = person(60);

        CashFlowLedger ledger = runYear(person, List.of(account), salary, YEAR_2026);
        Money totalHsa = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_HSA))
                .total();
        assertThat(totalHsa).isEqualTo(Money.usd("5400"));
    }

    @Test
    @DisplayName("IRA 50+ catch-up extends §408 headroom")
    void ira50PlusCatchup() {
        SalaryProfile salary = salary(MID_SALARY, 0);
        Account account =
                account(1L, AccountType.TRADITIONAL_IRA, ContributionPolicy.of(new FixedDollar(Money.usd("12000"))));
        Person person = person(55);

        CashFlowLedger ledger = runYear(person, List.of(account), salary, YEAR_2026);
        Money totalIra = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_TRADITIONAL_IRA))
                .total();
        assertThat(totalIra).isEqualTo(Money.usd("8500"));
    }

    @Test
    @DisplayName("60+ super catch-up extends §402(g) headroom further than 50+")
    void super60PlusCatchup() {
        SalaryProfile salary = salary(HIGH_SALARY, 0);
        Account account = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, null));
        Person person = person(62);

        CashFlowLedger ledger = runYear(person, List.of(account), salary, YEAR_2026);
        Money totalEmployee = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX))
                .total();
        assertThat(totalEmployee).isEqualTo(Money.usd("35750"));
    }

    @Test
    @DisplayName("Taxable brokerage with policy → AFTER_TAX flow, no IRS cap")
    void taxableBrokerageAfterTaxFlow() {
        SalaryProfile salary = salary(MID_SALARY, 0);
        Account account =
                account(1L, AccountType.TAXABLE_BROKERAGE, ContributionPolicy.of(new PercentOfSalary(SIX_PCT)));
        Person person = person(40);

        List<CashFlow> flows = engine.contributeForMonth(
                        person, List.of(account), salary, CashFlowLedger.empty(), YEAR_2026, Month.JANUARY)
                .flows();
        assertThat(flowAmount(flows, CashFlowKind.EMPLOYEE_AFTER_TAX)).isEqualTo(monthly(MID_SALARY, "0.06"));
    }

    @Test
    @DisplayName("§415(c) cap binds: combined employee + match exceeds total DC limit → match trimmed")
    void section415cCapBindsTrimsMatch() {
        SalaryProfile salary = salary(MID_SALARY, 0);
        EmployerMatch lavishMatch =
                EmployerMatch.of(List.of(new MatchTier(new BigDecimal("0.50"), new BigDecimal("2.00"))));
        Account account = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(new BigDecimal("0.50"), lavishMatch));
        Person person = person(40);

        CashFlowLedger ledger = runYear(person, List.of(account), salary, YEAR_2026);
        Money totalCombined = ledger.forYear(YEAR_2026).total();
        assertThat(totalCombined.amount()).isLessThanOrEqualTo(new BigDecimal("72000.000000"));
    }

    @Test
    @DisplayName("FixedDollar policy prorates monthly")
    void fixedDollarProratesMonthly() {
        SalaryProfile salary = salary(MID_SALARY, 0);
        Account account =
                account(1L, AccountType.TRADITIONAL_IRA, ContributionPolicy.of(new FixedDollar(Money.usd("6000"))));
        Person person = person(40);

        List<CashFlow> flows = engine.contributeForMonth(
                        person, List.of(account), salary, CashFlowLedger.empty(), YEAR_2026, Month.JANUARY)
                .flows();
        assertThat(flowAmount(flows, CashFlowKind.EMPLOYEE_TRADITIONAL_IRA)).isEqualTo(Money.usd("500"));
    }

    @Test
    @DisplayName("no policy → no flows")
    void noPolicyNoFlows() {
        SalaryProfile salary = salary(MID_SALARY, 0);
        Account base = Account.withDefaultSleeve(PLAN_ID, AccountType.TAXABLE_BROKERAGE, OWNER, Money.ZERO_USD);
        Account persisted = new Account(
                Optional.of(new AccountId(1L)),
                base.planId(),
                base.type(),
                base.owner(),
                base.sleeves(),
                base.contributionPolicy());
        Person person = person(40);

        List<CashFlow> flows = engine.contributeForMonth(
                        person, List.of(persisted), salary, CashFlowLedger.empty(), YEAR_2026, Month.JANUARY)
                .flows();
        assertThat(flows).isEmpty();
    }

    @Test
    @DisplayName("policy outside start/end window → no flows")
    void policyOutsideWindowNoFlows() {
        SalaryProfile salary = salary(MID_SALARY, 0);
        ContributionPolicy policy = new ContributionPolicy(
                new PercentOfSalary(SIX_PCT),
                Optional.empty(),
                Optional.empty(),
                Optional.of(LocalDate.of(YEAR_2027, 1, 1)),
                Optional.empty());
        Account account = account(1L, AccountType.TRADITIONAL_401K, policy);
        Person person = person(40);

        List<CashFlow> flows = engine.contributeForMonth(
                        person, List.of(account), salary, CashFlowLedger.empty(), YEAR_2026, Month.JUNE)
                .flows();
        assertThat(flows).isEmpty();
    }

    /**
     * Runs the engine for all 12 months of {@code year} and returns the
     * accumulated ledger. Replaces hand-rolled for-loops in tests.
     */
    private CashFlowLedger runYear(Person person, List<Account> accounts, SalaryProfile salary, int year) {
        return IntStream.rangeClosed(1, 12)
                .boxed()
                .reduce(
                        CashFlowLedger.empty(),
                        (ledger, m) -> ledger.appendAll(
                                engine.contributeForMonth(person, accounts, salary, ledger, year, Month.of(m))
                                        .flows()),
                        (a, b) -> a);
    }

    private Money totalEmployeeFor(Account account, SalaryProfile salary, Person person, int year) {
        return runYear(person, List.of(account), salary, year)
                .forYear(year)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX))
                .total();
    }

    private static SalaryProfile salary(int annual, int growthBps) {
        return new SalaryProfile(
                Optional.empty(),
                Money.usd(Integer.toString(annual)),
                BASE_DATE,
                BigDecimal.valueOf(growthBps).movePointLeft(4),
                Month.JANUARY,
                List.of(),
                Optional.empty(),
                Optional.of(Money.ZERO_USD));
    }

    private static SalaryProfile salaryWithBonus(int annual, String bonusAmount, Month payoutMonth) {
        return new SalaryProfile(
                Optional.empty(),
                Money.usd(Integer.toString(annual)),
                BASE_DATE,
                BigDecimal.ZERO,
                Month.JANUARY,
                List.of(),
                Optional.of(new FixedBonus(Money.usd(bonusAmount), payoutMonth)),
                Optional.of(Money.ZERO_USD));
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

    private static ContributionPolicy percentPolicyEscalating(BigDecimal startPct, LocalDate startDate) {
        return new ContributionPolicy(
                new PercentOfSalary(startPct),
                Optional.of(new EscalationPolicy(new BigDecimal("0.01"), FIVE_PCT)),
                Optional.empty(),
                Optional.of(startDate),
                Optional.empty());
    }

    private static EmployerMatch simpleMatch() {
        return EmployerMatch.of(List.of(new MatchTier(THREE_PCT, HUNDRED_PCT), new MatchTier(FIVE_PCT, HALF)));
    }

    private static Person person(int age) {
        return new Person(Optional.of(PERSON_ID), Optional.empty(), BASE_DATE.minusYears(age));
    }

    private static LocalDate startedYear(int year) {
        return LocalDate.of(year, 1, 1);
    }

    private static Money monthly(int annualSalary, String pct) {
        return Money.usd(Integer.toString(annualSalary))
                .dividedBy(new BigDecimal("12"))
                .times(new BigDecimal(pct));
    }

    private static Money flowAmount(List<CashFlow> flows, CashFlowKind kind) {
        return flows.stream()
                .filter(f -> f.kind() == kind)
                .map(CashFlow::amount)
                .findFirst()
                .orElse(Money.ZERO_USD);
    }
}
