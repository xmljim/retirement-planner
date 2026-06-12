/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import java.math.BigDecimal;
import java.time.Month;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;

import io.github.xmljim.retirement.retirementplanner.contribution.CashFlowLedger;
import io.github.xmljim.retirement.retirementplanner.contribution.ContributionEngine;
import io.github.xmljim.retirement.retirementplanner.contribution.IrsLimits;
import io.github.xmljim.retirement.retirementplanner.contribution.IrsLimitsService;
import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountType;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.ContributionPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfile;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Default {@link ContributionEngine} implementation (ADR-003).
 *
 * <p>Algorithm per call:
 * <ol>
 *   <li>Skip accounts without an active contribution policy.</li>
 *   <li>Compute intended employee monthly contribution via
 *       {@link MonthlyContributionPlanner}, which applies any
 *       escalation policy at year boundaries.</li>
 *   <li>Apply per-pool YTD caps using ledger queries:
 *       §402(g) across 401(k)/403(b); §408 across Trad+Roth IRA;
 *       §223 across HSA. Headroom is allocated across this month's
 *       intended contributions in account declaration order.</li>
 *   <li>Compute employer match against post-cap employee on
 *       401(k)/403(b) accounts.</li>
 *   <li>Apply §415(c) per (owner, plan-family) via
 *       {@link Section415cTrimmer} — trims match first, then employee,
 *       when the cap binds.</li>
 *   <li>Emit non-zero {@link CashFlow}s.</li>
 * </ol>
 */
@Service
class ContributionEngineImpl implements ContributionEngine {

    private static final Set<AccountType> ELECTIVE_401K_403B = EnumSet.of(
            AccountType.TRADITIONAL_401K, AccountType.ROTH_401K, AccountType.TRADITIONAL_403B, AccountType.ROTH_403B);
    private static final Set<AccountType> IRA_TYPES = EnumSet.of(AccountType.TRADITIONAL_IRA, AccountType.ROTH_IRA);
    private static final Set<CashFlowKind> ELECTIVE_DEFERRAL_KINDS =
            EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX, CashFlowKind.EMPLOYEE_ROTH);
    private static final Set<CashFlowKind> IRA_KINDS =
            EnumSet.of(CashFlowKind.EMPLOYEE_TRADITIONAL_IRA, CashFlowKind.EMPLOYEE_ROTH);
    private static final Set<CashFlowKind> HSA_KINDS = EnumSet.of(CashFlowKind.EMPLOYEE_HSA);

    private final IrsLimitsService limitsService;

    ContributionEngineImpl(IrsLimitsService limitsService) {
        this.limitsService = limitsService;
    }

    @Override
    public List<CashFlow> contributeForMonth(
            Person person,
            List<Account> accounts,
            SalaryProfile salaryProfile,
            CashFlowLedger ledger,
            int year,
            Month month) {
        YearMonth period = YearMonth.of(year, month.getValue());
        IrsLimits limits = limitsService.forYear(year);
        int age = MonthlyContributionPlanner.ageAt(person, period.atEndOfMonth());
        Money monthlySalary = MonthlyContributionPlanner.monthlySalary(salaryProfile, period);

        List<PlannedContribution> planned = accounts.stream()
                .filter(a -> a.contributionPolicy()
                        .filter(p -> MonthlyContributionPlanner.isActive(p, period))
                        .isPresent())
                .map(a -> new PlannedContribution(
                        a, MonthlyContributionPlanner.intendedContribution(a, monthlySalary, year)))
                .toList();

        List<CappedContribution> employeeCapped = applyEmployeeCaps(planned, ledger, period, limits, age);
        List<CappedContribution> matchApplied = applyEmployerMatch(employeeCapped, monthlySalary);
        List<CappedContribution> finalContribs = Section415cTrimmer.apply(matchApplied, ledger, period, limits);

        return finalContribs.stream()
                .flatMap(c -> c.toCashFlows(period).stream())
                .toList();
    }

    private static List<CappedContribution> applyEmployeeCaps(
            List<PlannedContribution> planned, CashFlowLedger ledger, YearMonth period, IrsLimits limits, int age) {
        List<Pool> pools = List.of(
                new Pool(
                        p -> ELECTIVE_401K_403B.contains(p.account().type()),
                        ELECTIVE_DEFERRAL_KINDS,
                        section402gCap(limits, age)),
                new Pool(p -> IRA_TYPES.contains(p.account().type()), IRA_KINDS, section408Cap(limits, age)),
                new Pool(p -> p.account().type() == AccountType.HSA, HSA_KINDS, section223Cap(limits, age)));

        return IntStream.range(0, planned.size())
                .mapToObj(i -> capForIndex(planned, i, ledger, period, pools))
                .toList();
    }

    private static CappedContribution capForIndex(
            List<PlannedContribution> planned, int index, CashFlowLedger ledger, YearMonth period, List<Pool> pools) {
        PlannedContribution current = planned.get(index);
        return pools.stream()
                .filter(pool -> pool.membership().test(current))
                .findFirst()
                .map(pool -> new CappedContribution(
                        current, allocateFromPool(planned, index, pool, ledger, period), Money.ZERO_USD))
                .orElseGet(() -> new CappedContribution(current, current.intended(), Money.ZERO_USD));
    }

    private static Money allocateFromPool(
            List<PlannedContribution> planned, int index, Pool pool, CashFlowLedger ledger, YearMonth period) {
        List<PlannedContribution> members =
                planned.stream().filter(pool.membership()).toList();
        if (members.isEmpty()) {
            return Money.ZERO_USD;
        }
        List<AccountId> memberIds = members.stream()
                .map(p -> p.account()
                        .id()
                        .orElseThrow(() -> new IllegalStateException("Account must be persisted before contributing")))
                .toList();
        Money headroom = headroom(ledger, period.getYear(), memberIds, pool.kinds(), pool.cap());
        Money intendedFromPriorMembers = planned.stream()
                .limit(index)
                .filter(pool.membership())
                .map(PlannedContribution::intended)
                .reduce(Money.ZERO_USD, Money::plus);
        Money remaining = headroom.minus(intendedFromPriorMembers);
        Money slotHeadroom = remaining.amount().signum() < 0 ? Money.ZERO_USD : remaining;
        return min(planned.get(index).intended(), slotHeadroom);
    }

    private record Pool(Predicate<PlannedContribution> membership, Set<CashFlowKind> kinds, Money cap) {}

    private static Money headroom(
            CashFlowLedger ledger, int year, List<AccountId> accountIds, Set<CashFlowKind> kinds, Money cap) {
        if (accountIds.isEmpty()) {
            return Money.ZERO_USD;
        }
        Money ytd = ledger.forYear(year).forAccounts(accountIds).forKinds(kinds).total();
        Money headroom = cap.minus(ytd);
        return headroom.amount().signum() < 0 ? Money.ZERO_USD : headroom;
    }

    private static Money section402gCap(IrsLimits limits, int age) {
        Money cap = limits.employee401kBase();
        if (age >= 60) {
            return cap.plus(limits.employee401k60PlusCatchup());
        }
        if (age >= 50) {
            return cap.plus(limits.employee401k50PlusCatchup());
        }
        return cap;
    }

    private static Money section408Cap(IrsLimits limits, int age) {
        return age >= 50 ? limits.iraBase().plus(limits.ira50PlusCatchup()) : limits.iraBase();
    }

    private static Money section223Cap(IrsLimits limits, int age) {
        Money cap = limits.hsaSelfOnly();
        return age >= 55 ? cap.plus(limits.hsa55PlusCatchup()) : cap;
    }

    private static List<CappedContribution> applyEmployerMatch(
            List<CappedContribution> employeeCapped, Money monthlySalary) {
        return employeeCapped.stream().map(c -> withMatch(c, monthlySalary)).toList();
    }

    private static CappedContribution withMatch(CappedContribution c, Money monthlySalary) {
        Money match = c.plan()
                .account()
                .contributionPolicy()
                .flatMap(ContributionPolicy::match)
                .map(m -> monthlySalary.times(m.matchPct(effectiveEmployeePctFor(c, monthlySalary))))
                .orElse(Money.ZERO_USD);
        return new CappedContribution(c.plan(), c.allowedEmployee(), match);
    }

    private static BigDecimal effectiveEmployeePctFor(CappedContribution c, Money monthlySalary) {
        if (monthlySalary.amount().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return c.allowedEmployee().amount().divide(monthlySalary.amount(), Money.INTERNAL_SCALE, Money.ROUNDING);
    }

    private static Money min(Money a, Money b) {
        return a.amount().compareTo(b.amount()) <= 0 ? a : b;
    }
}
