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
import io.github.xmljim.retirement.retirementplanner.plan.contribution.MatchTier;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.PercentOfSalary;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfile;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * SECURE 2.0 §604 optional Roth match — engine-level scenarios. When
 * the {@link EmployerMatch#asRoth()} flag is set on a 401(k)/403(b)
 * account, employer match accrues as
 * {@link CashFlowKind#EMPLOYER_MATCH_ROTH} rather than
 * {@link CashFlowKind#EMPLOYER_MATCH}; the tax engine (ADR-004 / EPIC-3)
 * will derive a current-year W-2 wages adjustment from these flows. §604
 * is orthogonal to §603 — the catch-up routing rules act on employee
 * deferrals only.
 */
@SuppressWarnings("PMD.ExcessiveImports")
class Section604EngineTest {

    private static final PlanId PLAN_ID = new PlanId(1L);
    private static final PersonId PERSON_ID = new PersonId(1L);
    private static final OwnerRef OWNER = new OwnerRef.Individual(PERSON_ID);
    private static final LocalDate BASE_DATE = LocalDate.of(2026, 1, 1);
    private static final int YEAR_2026 = 2026;

    private static final BigDecimal SIX_PCT = new BigDecimal("0.06");
    private static final BigDecimal TEN_PCT = new BigDecimal("0.10");
    private static final BigDecimal HUNDRED_PCT = new BigDecimal("1.00");
    private static final BigDecimal HALF = new BigDecimal("0.50");

    private static final Money WAGES_ABOVE_THRESHOLD = Money.usd("200000");
    private static final int MID_SALARY = 120_000;
    private static final int HIGH_SALARY = 500_000;

    private ContributionEngineImpl engine;

    @BeforeEach
    void setUp() {
        engine = new ContributionEngineImpl(new IrsLimitsServiceImpl());
    }

    @Test
    @DisplayName("default match (asRoth=false) → EMPLOYER_MATCH only, no EMPLOYER_MATCH_ROTH")
    void defaultMatchEmitsTraditionalKind() {
        Account account = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(SIX_PCT, simpleMatch(false)));

        MonthlyContributionResult result = engine.contributeForMonth(
                person(40), List.of(account), salary(MID_SALARY), CashFlowLedger.empty(), YEAR_2026, Month.JANUARY);

        assertThat(flowAmount(result.flows(), CashFlowKind.EMPLOYER_MATCH).amount())
                .isGreaterThan(BigDecimal.ZERO);
        assertThat(flowAmount(result.flows(), CashFlowKind.EMPLOYER_MATCH_ROTH)).isEqualTo(Money.ZERO_USD);
    }

    @Test
    @DisplayName("§604 elected (asRoth=true) → match emitted as EMPLOYER_MATCH_ROTH; no traditional match flow")
    void rothMatchEmitsRothKindOnly() {
        Account account = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(SIX_PCT, simpleMatch(true)));

        MonthlyContributionResult result = engine.contributeForMonth(
                person(40), List.of(account), salary(MID_SALARY), CashFlowLedger.empty(), YEAR_2026, Month.JANUARY);

        assertThat(flowAmount(result.flows(), CashFlowKind.EMPLOYER_MATCH_ROTH).amount())
                .isGreaterThan(BigDecimal.ZERO);
        assertThat(flowAmount(result.flows(), CashFlowKind.EMPLOYER_MATCH)).isEqualTo(Money.ZERO_USD);
    }

    @Test
    @DisplayName("§604 elected: match dollars equal what they would have been pre-tax")
    void rothMatchDollarsUnchanged() {
        // Compute the same employee deferral once with traditional match, once with §604 — match $$ identical.
        Account tradMatchAccount =
                account(1L, AccountType.TRADITIONAL_401K, percentPolicy(SIX_PCT, simpleMatch(false)));
        Account rothMatchAccount = account(2L, AccountType.TRADITIONAL_401K, percentPolicy(SIX_PCT, simpleMatch(true)));

        Money tradMatch = flowAmount(
                engine.contributeForMonth(
                                person(40),
                                List.of(tradMatchAccount),
                                salary(MID_SALARY),
                                CashFlowLedger.empty(),
                                YEAR_2026,
                                Month.JANUARY)
                        .flows(),
                CashFlowKind.EMPLOYER_MATCH);
        Money rothMatch = flowAmount(
                engine.contributeForMonth(
                                person(40),
                                List.of(rothMatchAccount),
                                salary(MID_SALARY),
                                CashFlowLedger.empty(),
                                YEAR_2026,
                                Month.JANUARY)
                        .flows(),
                CashFlowKind.EMPLOYER_MATCH_ROTH);
        assertThat(rothMatch).isEqualTo(tradMatch);
    }

    @Test
    @DisplayName("mixed accounts: some §604, some not → each account's match emits its own kind")
    void mixedRothAndTraditionalMatchAcrossAccounts() {
        Account k401Trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(SIX_PCT, simpleMatch(false)));
        Account b403Roth = account(2L, AccountType.TRADITIONAL_403B, percentPolicy(SIX_PCT, simpleMatch(true)));

        MonthlyContributionResult result = engine.contributeForMonth(
                person(40),
                List.of(k401Trad, b403Roth),
                salary(MID_SALARY),
                CashFlowLedger.empty(),
                YEAR_2026,
                Month.JANUARY);

        Money k401Match = result.flows().stream()
                .filter(f -> f.accountId() == 1L && f.kind() == CashFlowKind.EMPLOYER_MATCH)
                .map(CashFlow::amount)
                .findFirst()
                .orElse(Money.ZERO_USD);
        Money b403MatchRoth = result.flows().stream()
                .filter(f -> f.accountId() == 2L && f.kind() == CashFlowKind.EMPLOYER_MATCH_ROTH)
                .map(CashFlow::amount)
                .findFirst()
                .orElse(Money.ZERO_USD);
        Money k401MatchRoth = result.flows().stream()
                .filter(f -> f.accountId() == 1L && f.kind() == CashFlowKind.EMPLOYER_MATCH_ROTH)
                .map(CashFlow::amount)
                .findFirst()
                .orElse(Money.ZERO_USD);
        Money b403Match = result.flows().stream()
                .filter(f -> f.accountId() == 2L && f.kind() == CashFlowKind.EMPLOYER_MATCH)
                .map(CashFlow::amount)
                .findFirst()
                .orElse(Money.ZERO_USD);

        assertThat(k401Match.amount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(b403MatchRoth.amount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(k401MatchRoth).isEqualTo(Money.ZERO_USD);
        assertThat(b403Match).isEqualTo(Money.ZERO_USD);
    }

    @Test
    @DisplayName("§415(c) cap binds: Roth match is trimmed identically to traditional match")
    void section415cTrimsRothMatch() {
        // Same lavish-match scenario as ContributionEngineImplTest.section415cCapBindsTrimsMatch,
        // but with §604 elected. Trimmer must include EMPLOYER_MATCH_ROTH in §415(c) kind set.
        SalaryProfile salary = salary(MID_SALARY);
        EmployerMatch lavishMatch =
                EmployerMatch.ofRoth(List.of(new MatchTier(new BigDecimal("0.50"), new BigDecimal("2.00"))));
        Account account = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(new BigDecimal("0.50"), lavishMatch));

        CashFlowLedger ledger = runYear(person(40), List.of(account), salary, YEAR_2026);

        Money totalCombined = ledger.forYear(YEAR_2026).total();
        // §415(c) cap for 2026 is 72k.
        assertThat(totalCombined.amount()).isLessThanOrEqualTo(new BigDecimal("72000.000000"));
        // The match that landed must be Roth, not traditional.
        Money rothMatch = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYER_MATCH_ROTH))
                .total();
        Money tradMatch = ledger.forYear(YEAR_2026)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYER_MATCH))
                .total();
        assertThat(rothMatch.amount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(tradMatch).isEqualTo(Money.ZERO_USD);
    }

    @Test
    @DisplayName("§603 + §604 orthogonal: high earner gets Roth catch-up + Roth match together")
    void section603AndSection604OrthogonalForHighEarner() {
        // High earner (age 55, prior-year wages > §603 threshold), Trad 401(k) source with §604 Roth match,
        // Roth 401(k) target for §603 catch-up. Expect:
        //   - traditional source: EMPLOYEE_PRETAX (base) + EMPLOYER_MATCH_ROTH (§604), no EMPLOYER_MATCH
        //   - Roth target: EMPLOYEE_ROTH_CATCHUP (§603-routed)
        SalaryProfile salary = highEarnerSalary(WAGES_ABOVE_THRESHOLD);
        EmployerMatch rothMatch = EmployerMatch.ofRoth(List.of(new MatchTier(SIX_PCT, HUNDRED_PCT)));
        Account trad = account(1L, AccountType.TRADITIONAL_401K, percentPolicy(TEN_PCT, rothMatch));
        Account roth = account(2L, AccountType.ROTH_401K, percentPolicy(BigDecimal.ZERO, null));

        CashFlowLedger ledger = runYear(person(55), List.of(trad, roth), salary, YEAR_2026);

        Money tradMatchRoth = ledger.forYear(YEAR_2026)
                .forAccount(new AccountId(1L))
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYER_MATCH_ROTH))
                .total();
        Money tradMatchTraditional = ledger.forYear(YEAR_2026)
                .forAccount(new AccountId(1L))
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYER_MATCH))
                .total();
        Money rothCatchup = ledger.forYear(YEAR_2026)
                .forAccount(new AccountId(2L))
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_ROTH_CATCHUP))
                .total();

        assertThat(tradMatchRoth.amount()).isGreaterThan(BigDecimal.ZERO);
        assertThat(tradMatchTraditional).isEqualTo(Money.ZERO_USD);
        assertThat(rothCatchup.amount()).isGreaterThan(BigDecimal.ZERO);
    }

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

    private static EmployerMatch simpleMatch(boolean asRoth) {
        List<MatchTier> tiers = List.of(
                new MatchTier(new BigDecimal("0.03"), HUNDRED_PCT), new MatchTier(new BigDecimal("0.05"), HALF));
        return asRoth ? EmployerMatch.ofRoth(tiers) : EmployerMatch.of(tiers);
    }

    private static SalaryProfile salary(int annual) {
        return new SalaryProfile(
                Optional.empty(),
                Money.usd(Integer.toString(annual)),
                BASE_DATE,
                BigDecimal.ZERO,
                Month.JANUARY,
                List.of(),
                Optional.empty(),
                Optional.of(Money.ZERO_USD));
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

    private static Money flowAmount(List<CashFlow> flows, CashFlowKind kind) {
        return flows.stream()
                .filter(f -> f.kind() == kind)
                .map(CashFlow::amount)
                .findFirst()
                .orElse(Money.ZERO_USD);
    }
}
