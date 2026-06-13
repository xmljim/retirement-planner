/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import io.github.xmljim.retirement.retirementplanner.contribution.CashFlowLedger;
import io.github.xmljim.retirement.retirementplanner.contribution.IrsLimits;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountType;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Applies the per-pool employee contribution caps (§402(g) across
 * 401(k)/403(b), §408 across Trad+Roth IRA, §223 for HSA) to a list of
 * {@link PlannedContribution}s, producing {@link CappedContribution}s
 * with employee amounts trimmed to remaining headroom.
 *
 * <p>Headroom is computed once per pool from the ledger's year-to-date
 * total minus this month's pool cap, then distributed across that
 * pool's members in declaration order. Intended dollars from earlier
 * members reduce the headroom available to later members.
 *
 * <p>Extracted from {@link ContributionEngineImpl} so that the engine
 * orchestrator stays below the project's coupling threshold.
 */
final class EmployeeCapAllocator {

    private static final Set<AccountType> ELECTIVE_401K_403B = EnumSet.of(
            AccountType.TRADITIONAL_401K, AccountType.ROTH_401K, AccountType.TRADITIONAL_403B, AccountType.ROTH_403B);
    private static final Set<AccountType> IRA_TYPES = EnumSet.of(AccountType.TRADITIONAL_IRA, AccountType.ROTH_IRA);
    private static final Set<CashFlowKind> ELECTIVE_DEFERRAL_KINDS =
            EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX, CashFlowKind.EMPLOYEE_ROTH, CashFlowKind.EMPLOYEE_ROTH_CATCHUP);
    private static final Set<CashFlowKind> IRA_KINDS =
            EnumSet.of(CashFlowKind.EMPLOYEE_TRADITIONAL_IRA, CashFlowKind.EMPLOYEE_ROTH);
    private static final Set<CashFlowKind> HSA_KINDS = EnumSet.of(CashFlowKind.EMPLOYEE_HSA);

    private EmployeeCapAllocator() {}

    static List<CappedContribution> apply(
            List<PlannedContribution> planned, CashFlowLedger ledger, YearMonth period, IrsLimits limits, int age) {
        List<Pool> pools = List.of(
                new Pool(
                        p -> ELECTIVE_401K_403B.contains(p.account().type()),
                        ELECTIVE_DEFERRAL_KINDS,
                        SectionCapResolver.section402gCap(limits, age)),
                new Pool(
                        p -> IRA_TYPES.contains(p.account().type()),
                        IRA_KINDS,
                        SectionCapResolver.section408Cap(limits, age)),
                new Pool(
                        p -> p.account().type() == AccountType.HSA,
                        HSA_KINDS,
                        SectionCapResolver.section223Cap(limits, age)));
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
                .map(pool -> CappedContribution.of(
                        current, allocateFromPool(planned, index, pool, ledger, period), Money.ZERO_USD))
                .orElseGet(() -> CappedContribution.of(current, current.intended(), Money.ZERO_USD));
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

    private static Money headroom(
            CashFlowLedger ledger, int year, List<AccountId> accountIds, Set<CashFlowKind> kinds, Money cap) {
        if (accountIds.isEmpty()) {
            return Money.ZERO_USD;
        }
        Money ytd = ledger.forYear(year).forAccounts(accountIds).forKinds(kinds).total();
        Money headroom = cap.minus(ytd);
        return headroom.amount().signum() < 0 ? Money.ZERO_USD : headroom;
    }

    private static Money min(Money a, Money b) {
        return a.amount().compareTo(b.amount()) <= 0 ? a : b;
    }

    private record Pool(Predicate<PlannedContribution> membership, Set<CashFlowKind> kinds, Money cap) {}
}
