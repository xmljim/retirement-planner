/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import java.math.BigDecimal;
import java.time.Month;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;

import io.github.xmljim.retirement.retirementplanner.contribution.CashFlowLedger;
import io.github.xmljim.retirement.retirementplanner.contribution.ContributionEngine;
import io.github.xmljim.retirement.retirementplanner.contribution.IrsLimits;
import io.github.xmljim.retirement.retirementplanner.contribution.IrsLimitsService;
import io.github.xmljim.retirement.retirementplanner.contribution.MonthlyContributionResult;
import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.ContributionPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfile;
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
 *   <li>Apply per-pool YTD caps via {@link EmployeeCapAllocator}:
 *       §402(g) across 401(k)/403(b); §408 across Trad+Roth IRA;
 *       §223 across HSA.</li>
 *   <li>Compute employer match against post-cap employee on
 *       401(k)/403(b) accounts.</li>
 *   <li>Apply SECURE 2.0 §603 high-earner Roth catch-up routing via
 *       {@link Section603Router} when applicable; the catch-up portion
 *       of any 401(k)/403(b) elective deferral is re-routed to a Roth
 *       designated account or disallowed with a warning.</li>
 *   <li>Apply §415(c) per (owner, plan-family) via
 *       {@link Section415cTrimmer} — trims match first, then employee,
 *       when the cap binds. Routed Roth catch-up is summed into the
 *       same plan-family bucket as the source.</li>
 *   <li>Emit non-zero cash flows alongside any §603 warnings.</li>
 * </ol>
 */
@Service
class ContributionEngineImpl implements ContributionEngine {

    private final IrsLimitsService limitsService;

    ContributionEngineImpl(IrsLimitsService limitsService) {
        this.limitsService = limitsService;
    }

    @Override
    public MonthlyContributionResult contributeForMonth(
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
        Money priorYearWages = salaryProfile.priorYearWagesFor(year);

        List<PlannedContribution> planned = accounts.stream()
                .filter(a -> a.contributionPolicy()
                        .filter(p -> MonthlyContributionPlanner.isActive(p, period))
                        .isPresent())
                .map(a -> new PlannedContribution(
                        a, MonthlyContributionPlanner.intendedContribution(a, monthlySalary, year)))
                .toList();

        List<CappedContribution> employeeCapped = EmployeeCapAllocator.apply(planned, ledger, period, limits, age);
        List<CappedContribution> matchApplied = applyEmployerMatch(employeeCapped, monthlySalary);
        Section603Router.Result routed =
                Section603Router.route(matchApplied, accounts, ledger, period, limits, priorYearWages, age);
        List<CappedContribution> finalContribs = Section415cTrimmer.apply(routed.contribs(), ledger, period, limits);

        return new MonthlyContributionResult(
                finalContribs.stream()
                        .flatMap(c -> c.toCashFlows(period).stream())
                        .toList(),
                routed.warnings());
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
        return c.withAmounts(c.allowedEmployee(), match);
    }

    private static BigDecimal effectiveEmployeePctFor(CappedContribution c, Money monthlySalary) {
        if (monthlySalary.amount().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return c.allowedEmployee().amount().divide(monthlySalary.amount(), Money.INTERNAL_SCALE, Money.ROUNDING);
    }
}
