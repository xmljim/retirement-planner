/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import io.github.xmljim.retirement.retirementplanner.contribution.CashFlowLedger;
import io.github.xmljim.retirement.retirementplanner.contribution.IrsLimits;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountType;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Applies the §415(c) total-defined-contribution-plan cap (employee +
 * employer combined per plan per year) to a list of post-employee-cap
 * contributions (ADR-003).
 *
 * <p>S-2.4 grouping: all of a person's 401(k)/Roth 401(k) accounts share
 * one bucket; all 403(b)/Roth 403(b) accounts share another. The
 * &ldquo;same-employer&rdquo; correction (different §415(c) buckets per
 * employer plan) is tracked as tech debt — the model lacks an
 * employer-plan reference today.
 *
 * <p>When a group's running YTD + this month's planned employee + match
 * exceeds the §415(c) cap, the trimmer removes match dollars first
 * (the engine has already approved the employee deferral against
 * §402(g)) and only trims employee if match alone cannot absorb the
 * excess.
 */
final class Section415cTrimmer {

    private static final Set<CashFlowKind> SECTION_415C_KINDS =
            EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX, CashFlowKind.EMPLOYEE_ROTH, CashFlowKind.EMPLOYER_MATCH);
    private static final Set<AccountType> PLAN_FAMILY_401K =
            EnumSet.of(AccountType.TRADITIONAL_401K, AccountType.ROTH_401K);
    private static final Set<AccountType> PLAN_FAMILY_403B =
            EnumSet.of(AccountType.TRADITIONAL_403B, AccountType.ROTH_403B);

    private Section415cTrimmer() {}

    static List<CappedContribution> apply(
            List<CappedContribution> contribs, CashFlowLedger ledger, YearMonth period, IrsLimits limits) {
        Money cap = limits.totalDc();
        List<CappedContribution> contribs401k = filterByFamily(contribs, PLAN_FAMILY_401K);
        List<CappedContribution> contribs403b = filterByFamily(contribs, PLAN_FAMILY_403B);
        Money trim401k = excess(contribs401k, ledger, period, cap);
        Money trim403b = excess(contribs403b, ledger, period, cap);

        return contribs.stream()
                .map(c -> trimForFamily(c, contribs401k, contribs403b, trim401k, trim403b))
                .toList();
    }

    private static List<CappedContribution> filterByFamily(List<CappedContribution> contribs, Set<AccountType> family) {
        return contribs.stream()
                .filter(c -> family.contains(c.plan().account().type()))
                .toList();
    }

    private static CappedContribution trimForFamily(
            CappedContribution c,
            List<CappedContribution> contribs401k,
            List<CappedContribution> contribs403b,
            Money trim401k,
            Money trim403b) {
        AccountType type = c.plan().account().type();
        if (PLAN_FAMILY_401K.contains(type)) {
            return applyGroupTrim(c, contribs401k, trim401k);
        }
        if (PLAN_FAMILY_403B.contains(type)) {
            return applyGroupTrim(c, contribs403b, trim403b);
        }
        return c;
    }

    private static Money excess(List<CappedContribution> group, CashFlowLedger ledger, YearMonth period, Money cap) {
        if (group.isEmpty()) {
            return Money.ZERO_USD;
        }
        List<AccountId> ids = group.stream()
                .map(c -> c.plan()
                        .account()
                        .id()
                        .orElseThrow(() -> new IllegalStateException(
                                "Account must be persisted (have an id) before contributing")))
                .toList();
        Money ytd = ledger.forYear(period.getYear())
                .forAccounts(ids)
                .forKinds(SECTION_415C_KINDS)
                .total();
        Money thisMonth =
                group.stream().map(c -> c.allowedEmployee().plus(c.match())).reduce(Money.ZERO_USD, Money::plus);
        Money projected = ytd.plus(thisMonth);
        Money excess = projected.minus(cap);
        return excess.amount().signum() <= 0 ? Money.ZERO_USD : excess;
    }

    private static CappedContribution applyGroupTrim(
            CappedContribution c, List<CappedContribution> group, Money totalTrim) {
        if (totalTrim.amount().signum() <= 0) {
            return c;
        }
        Money groupMatch = group.stream().map(CappedContribution::match).reduce(Money.ZERO_USD, Money::plus);
        Money matchTrim = min(totalTrim, groupMatch);
        Money employeeTrim = totalTrim.minus(matchTrim);
        Money matchAfter = trimProRata(c.match(), groupMatch, matchTrim);
        Money groupEmployee =
                group.stream().map(CappedContribution::allowedEmployee).reduce(Money.ZERO_USD, Money::plus);
        Money employeeAfter = trimProRata(c.allowedEmployee(), groupEmployee, employeeTrim);
        return new CappedContribution(c.plan(), employeeAfter, matchAfter);
    }

    private static Money trimProRata(Money portion, Money groupTotal, Money trim) {
        if (trim.amount().signum() <= 0
                || portion.amount().signum() <= 0
                || groupTotal.amount().signum() <= 0) {
            return portion;
        }
        BigDecimal share = portion.amount().divide(groupTotal.amount(), Money.INTERNAL_SCALE, Money.ROUNDING);
        Money portionTrim = trim.times(share);
        Money result = portion.minus(portionTrim);
        return result.amount().signum() < 0 ? Money.ZERO_USD : result;
    }

    private static Money min(Money a, Money b) {
        return a.amount().compareTo(b.amount()) <= 0 ? a : b;
    }
}
