/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import io.github.xmljim.retirement.retirementplanner.contribution.CashFlowLedger;
import io.github.xmljim.retirement.retirementplanner.contribution.EngineWarning;
import io.github.xmljim.retirement.retirementplanner.contribution.IrsLimits;
import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountType;
import io.github.xmljim.retirement.retirementplanner.plan.account.OwnerRef;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Applies SECURE 2.0 §603: when a person's prior-year FICA wages exceed
 * the indexed threshold, the catch-up portion of any 401(k)/403(b)
 * elective deferral must be designated Roth (ADR-003).
 *
 * <p>Pipeline phase: runs after {@link MonthlyContributionPlanner} and
 * employer-match application, before {@link Section415cTrimmer}.
 * §603 only re-tags or re-routes the employee elective deferral; the
 * employer match is governed independently by §604 (S-2.6) and stays on
 * the source account here.
 *
 * <p>Catch-up slicing: §402(g) caps elective deferrals across all
 * 401(k)/403(b) accounts for the year. The "catch-up portion" of a
 * given month's contribution is whatever pushes the cumulative pool YTD
 * past {@link IrsLimits#employee401kBase()}. Walking pool members in
 * declaration order matches how {@link ContributionEngineImpl}
 * allocates §402(g) headroom, so each account's slice is exactly the
 * dollars within its allowed employee that fall above the base cap.
 *
 * <p>Routing per slice:
 * <ul>
 *   <li>Source already Roth 401(k)/403(b): the slice stays on the
 *       account, with the kind override
 *       {@link CashFlowKind#EMPLOYEE_ROTH_CATCHUP} so the tax engine
 *       can distinguish base Roth deferral from §603 catch-up.</li>
 *   <li>Source Trad 401(k)/403(b): the slice moves to a Roth
 *       designated account in the same family within the input
 *       accounts list. Match remains on the source.</li>
 *   <li>No Roth designated account exists: the slice is dropped from
 *       the source's allowed employee and an
 *       {@link EngineWarning.WarningKind#SECTION_603_NO_ROTH_DESTINATION}
 *       is emitted.</li>
 * </ul>
 *
 * <p>Single-employer assumption: per existing tech-debt (#93), all of a
 * person's 401(k)/403(b) accounts are assumed to belong to one employer
 * plan, so any Roth designated account in the matching family is a
 * valid target. When per-employer plan refs land, this lookup tightens
 * to "same employer plan as source".
 */
final class Section603Router {

    static final int FIRST_EFFECTIVE_YEAR = 2026;
    private static final int CATCHUP_AGE_FLOOR = 50;
    private static final Set<AccountType> ELECTIVE_401K_403B = EnumSet.of(
            AccountType.TRADITIONAL_401K, AccountType.ROTH_401K, AccountType.TRADITIONAL_403B, AccountType.ROTH_403B);
    private static final Set<AccountType> PLAN_FAMILY_401K =
            EnumSet.of(AccountType.TRADITIONAL_401K, AccountType.ROTH_401K);
    private static final Set<AccountType> PLAN_FAMILY_403B =
            EnumSet.of(AccountType.TRADITIONAL_403B, AccountType.ROTH_403B);
    private static final Set<CashFlowKind> ELECTIVE_DEFERRAL_KINDS =
            EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX, CashFlowKind.EMPLOYEE_ROTH, CashFlowKind.EMPLOYEE_ROTH_CATCHUP);

    private Section603Router() {}

    static Result route(
            List<CappedContribution> contribs,
            List<Account> allAccounts,
            CashFlowLedger ledger,
            YearMonth period,
            IrsLimits limits,
            Money priorYearWages,
            int age) {
        if (!applies(period, age, priorYearWages, limits)) {
            return new Result(contribs, List.of());
        }
        Money baseCap = limits.employee401kBase();
        List<AccountId> poolIds = poolAccountIds(contribs);
        Money poolYtdInitial = poolIds.isEmpty()
                ? Money.ZERO_USD
                : ledger.forYear(period.getYear())
                        .forAccounts(poolIds)
                        .forKinds(ELECTIVE_DEFERRAL_KINDS)
                        .total();
        List<RouteOutcome> outcomes = IntStream.range(0, contribs.size())
                .mapToObj(i -> routeOne(contribs, i, poolYtdInitial, baseCap, allAccounts, period))
                .toList();
        return new Result(
                outcomes.stream().flatMap(o -> o.emit().stream()).toList(),
                outcomes.stream()
                        .map(RouteOutcome::warning)
                        .flatMap(Optional::stream)
                        .toList());
    }

    private static boolean applies(YearMonth period, int age, Money priorYearWages, IrsLimits limits) {
        return period.getYear() >= FIRST_EFFECTIVE_YEAR
                && age >= CATCHUP_AGE_FLOOR
                && priorYearWages
                                .amount()
                                .compareTo(limits.secure2_0_603HighEarnerThreshold()
                                        .amount())
                        > 0;
    }

    private static List<AccountId> poolAccountIds(List<CappedContribution> contribs) {
        return contribs.stream()
                .filter(Section603Router::isElective)
                .map(c -> c.plan().account().id().orElseThrow())
                .toList();
    }

    private static RouteOutcome routeOne(
            List<CappedContribution> contribs,
            int index,
            Money poolYtdInitial,
            Money baseCap,
            List<Account> allAccounts,
            YearMonth period) {
        CappedContribution current = contribs.get(index);
        if (!isElective(current)) {
            return RouteOutcome.passthrough(current);
        }
        Money poolBefore = poolYtdInitial.plus(contribs.stream()
                .limit(index)
                .filter(Section603Router::isElective)
                .map(CappedContribution::allowedEmployee)
                .reduce(Money.ZERO_USD, Money::plus));
        Money poolAfter = poolBefore.plus(current.allowedEmployee());
        Money slice = catchupSlice(poolBefore, poolAfter, baseCap);
        if (slice.amount().signum() <= 0) {
            return RouteOutcome.passthrough(current);
        }
        return splitForCatchup(current, slice, allAccounts, period);
    }

    private static Money catchupSlice(Money poolBefore, Money poolAfter, Money baseCap) {
        Money excessAfter = nonNegative(poolAfter.minus(baseCap));
        Money excessBefore = nonNegative(poolBefore.minus(baseCap));
        return excessAfter.minus(excessBefore);
    }

    private static Money nonNegative(Money money) {
        return money.amount().signum() < 0 ? Money.ZERO_USD : money;
    }

    private static RouteOutcome splitForCatchup(
            CappedContribution source, Money slice, List<Account> allAccounts, YearMonth period) {
        Money baseRemainder = source.allowedEmployee().minus(slice);
        CappedContribution baseAfter = source.withAmounts(baseRemainder, source.match());
        AccountType srcType = source.plan().account().type();
        if (isRoth(srcType)) {
            CappedContribution catchup = new CappedContribution(
                    new PlannedContribution(source.plan().account(), slice),
                    slice,
                    Money.ZERO_USD,
                    Optional.of(CashFlowKind.EMPLOYEE_ROTH_CATCHUP));
            return new RouteOutcome(List.of(baseAfter, catchup), Optional.empty());
        }
        Optional<Account> rothTarget =
                findRothTarget(allAccounts, srcType, source.plan().account().owner());
        if (rothTarget.isPresent()) {
            CappedContribution catchup = new CappedContribution(
                    new PlannedContribution(rothTarget.get(), slice),
                    slice,
                    Money.ZERO_USD,
                    Optional.of(CashFlowKind.EMPLOYEE_ROTH_CATCHUP));
            return new RouteOutcome(List.of(baseAfter, catchup), Optional.empty());
        }
        EngineWarning warning = new EngineWarning(
                EngineWarning.WarningKind.SECTION_603_NO_ROTH_DESTINATION,
                source.plan().account().id().orElseThrow().value(),
                period.getYear(),
                period.getMonth(),
                "§603 catch-up portion " + slice + " disallowed: no Roth designated account in plan family "
                        + familyName(srcType));
        return new RouteOutcome(List.of(baseAfter), Optional.of(warning));
    }

    private static Optional<Account> findRothTarget(List<Account> allAccounts, AccountType sourceType, OwnerRef owner) {
        Set<AccountType> family = sameFamilyAs(sourceType);
        return allAccounts.stream()
                .filter(a -> a.owner().equals(owner))
                .filter(a -> family.contains(a.type()))
                .filter(a -> isRoth(a.type()))
                .findFirst();
    }

    private static Set<AccountType> sameFamilyAs(AccountType type) {
        return PLAN_FAMILY_401K.contains(type) ? PLAN_FAMILY_401K : PLAN_FAMILY_403B;
    }

    private static String familyName(AccountType type) {
        return PLAN_FAMILY_401K.contains(type) ? "401(k)" : "403(b)";
    }

    private static boolean isRoth(AccountType type) {
        return type == AccountType.ROTH_401K || type == AccountType.ROTH_403B;
    }

    private static boolean isElective(CappedContribution contrib) {
        return ELECTIVE_401K_403B.contains(contrib.plan().account().type());
    }

    /** Outcome of routing one contribution: 1–2 emitted contribs plus an optional warning. */
    private record RouteOutcome(List<CappedContribution> emit, Optional<EngineWarning> warning) {
        static RouteOutcome passthrough(CappedContribution contrib) {
            return new RouteOutcome(List.of(contrib), Optional.empty());
        }
    }

    /** Aggregate routing result. */
    record Result(List<CappedContribution> contribs, List<EngineWarning> warnings) {
        Result {
            contribs = List.copyOf(contribs);
            warnings = List.copyOf(warnings);
        }
    }
}
