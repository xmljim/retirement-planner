/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.ContributionPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.EscalationPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.FixedDollar;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.PercentOfSalary;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfile;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Computes intended (pre-cap) employee contributions from
 * {@link ContributionPolicy} + monthly salary, applying any
 * {@link EscalationPolicy} at year boundaries.
 *
 * <p>Extracted from {@link ContributionEngineImpl} so that engine
 * imports stay below the project's coupling threshold and the
 * cap-application logic in the engine isn't entangled with policy
 * interpretation.
 */
final class MonthlyContributionPlanner {

    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    private MonthlyContributionPlanner() {}

    /** Age in completed years on {@code date}. */
    static int ageAt(Person person, LocalDate date) {
        return Period.between(person.dob(), date).getYears();
    }

    /** Whether {@code policy} is active in {@code period}. */
    static boolean isActive(ContributionPolicy policy, YearMonth period) {
        LocalDate monthStart = period.atDay(1);
        LocalDate monthEnd = period.atEndOfMonth();
        boolean afterStart = policy.startDate().map(d -> !d.isAfter(monthEnd)).orElse(true);
        boolean beforeEnd = policy.endDate().map(d -> !d.isBefore(monthStart)).orElse(true);
        return afterStart && beforeEnd;
    }

    /** Salary at {@code period}'s month-end, plus any bonus payable in that month. */
    static Money monthlySalary(SalaryProfile salaryProfile, YearMonth period) {
        Money base = salaryProfile.salaryAt(period.atEndOfMonth()).dividedBy(MONTHS_PER_YEAR);
        Optional<Money> bonus = salaryProfile.bonusFor(period);
        return bonus.map(base::plus).orElse(base);
    }

    /** Intended (pre-cap) employee contribution for one account in one month. */
    static Money intendedContribution(Account account, Money monthlySalary, int year) {
        ContributionPolicy policy = account.contributionPolicy().orElseThrow();
        BigDecimal effectiveRate = effectiveRate(policy, year);
        return switch (policy.employee()) {
            case PercentOfSalary ignored -> monthlySalary.times(effectiveRate);
            case FixedDollar fixed -> fixed.annualAmount().dividedBy(MONTHS_PER_YEAR);
        };
    }

    private static BigDecimal effectiveRate(ContributionPolicy policy, int year) {
        if (!(policy.employee() instanceof PercentOfSalary pct)) {
            return BigDecimal.ZERO;
        }
        if (policy.escalation().isEmpty()) {
            return pct.pct();
        }
        EscalationPolicy escalation = policy.escalation().get();
        int startYear = policy.startDate().map(LocalDate::getYear).orElse(year);
        int yearsSinceStart = Math.max(0, year - startYear);
        BigDecimal escalated = pct.pct().add(escalation.annualIncrease().multiply(BigDecimal.valueOf(yearsSinceStart)));
        return escalated.min(escalation.cap());
    }
}
