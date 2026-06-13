/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import io.github.xmljim.retirement.retirementplanner.plan.account.AccountType;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Post-cap employee + match for one account, ready to emit as cash flows.
 *
 * <p>{@code employeeKindOverride} is set by {@link Section603Router} when
 * a routed catch-up slice must emit as
 * {@link CashFlowKind#EMPLOYEE_ROTH_CATCHUP} rather than the default kind
 * derived from the account type.
 */
record CappedContribution(
        PlannedContribution plan, Money allowedEmployee, Money match, Optional<CashFlowKind> employeeKindOverride) {

    private static final Map<AccountType, CashFlowKind> EMPLOYEE_KIND_BY_TYPE = Map.of(
            AccountType.TRADITIONAL_401K, CashFlowKind.EMPLOYEE_PRETAX,
            AccountType.TRADITIONAL_403B, CashFlowKind.EMPLOYEE_PRETAX,
            AccountType.ROTH_401K, CashFlowKind.EMPLOYEE_ROTH,
            AccountType.ROTH_403B, CashFlowKind.EMPLOYEE_ROTH,
            AccountType.ROTH_IRA, CashFlowKind.EMPLOYEE_ROTH,
            AccountType.TRADITIONAL_IRA, CashFlowKind.EMPLOYEE_TRADITIONAL_IRA,
            AccountType.HSA, CashFlowKind.EMPLOYEE_HSA,
            AccountType.TAXABLE_BROKERAGE, CashFlowKind.EMPLOYEE_AFTER_TAX,
            AccountType.CASH, CashFlowKind.EMPLOYEE_AFTER_TAX);

    CappedContribution {
        Objects.requireNonNull(plan, "plan");
        Objects.requireNonNull(allowedEmployee, "allowedEmployee");
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(employeeKindOverride, "employeeKindOverride");
    }

    /** Convenience for the common case of no kind override. */
    static CappedContribution of(PlannedContribution plan, Money allowedEmployee, Money match) {
        return new CappedContribution(plan, allowedEmployee, match, Optional.empty());
    }

    /** Returns this contribution with employee/match amounts replaced; preserves the kind override. */
    CappedContribution withAmounts(Money newAllowedEmployee, Money newMatch) {
        return new CappedContribution(plan, newAllowedEmployee, newMatch, employeeKindOverride);
    }

    List<CashFlow> toCashFlows(YearMonth period) {
        long accountId = plan.account().id().orElseThrow().value();
        CashFlowKind employeeKind = employeeKindOverride.orElseGet(this::resolveKindFromType);
        return Stream.of(
                        new CashFlow(period, accountId, employeeKind, allowedEmployee),
                        new CashFlow(period, accountId, CashFlowKind.EMPLOYER_MATCH, match))
                .filter(f -> f.amount().amount().signum() > 0)
                .toList();
    }

    private CashFlowKind resolveKindFromType() {
        CashFlowKind employeeKind = EMPLOYEE_KIND_BY_TYPE.get(plan.account().type());
        if (employeeKind == null) {
            throw new IllegalArgumentException(
                    "Account type " + plan.account().type() + " cannot have a contribution policy");
        }
        return employeeKind;
    }
}
