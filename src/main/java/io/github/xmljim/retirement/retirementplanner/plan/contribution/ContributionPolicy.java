/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.contribution;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import io.github.xmljim.retirement.retirementplanner.plan.account.AccountType;

/**
 * Parametric description of how an {@link Account} is funded
 * (ADR-003, FR-2.4). The contribution engine consumes this each month
 * to derive employee and employer cash flows.
 *
 * <ul>
 *   <li>{@link #employee()} drives the employee dollars before any IRS
 *       cap is applied. {@link PercentOfSalary} consults the
 *       {@link SalaryProfile}; {@link FixedDollar} is annual.</li>
 *   <li>{@link #escalation()} optionally bumps the rate at year
 *       boundaries up to a cap.</li>
 *   <li>{@link #match()} optionally describes a tiered employer match.
 *       Matching is only valid on employer-sponsored DC plans
 *       (401(k)/403(b)) — see {@link #matchAllowedFor(AccountType)}.</li>
 *   <li>{@link #startDate()} / {@link #endDate()} bound the policy in
 *       time; both are {@link Optional} (open-ended on either side).</li>
 * </ul>
 *
 * <p>The §603 / §604 routing rules from ADR-003 act on the engine's
 * output, not on this value type — the policy is the user's intent;
 * runtime decides how the dollars actually flow.
 */
public record ContributionPolicy(
        ContributionAmount employee,
        Optional<EscalationPolicy> escalation,
        Optional<EmployerMatch> match,
        Optional<LocalDate> startDate,
        Optional<LocalDate> endDate) {

    private static final Set<AccountType> MATCH_ALLOWED_TYPES = EnumSet.of(
            AccountType.TRADITIONAL_401K, AccountType.ROTH_401K, AccountType.TRADITIONAL_403B, AccountType.ROTH_403B);

    public ContributionPolicy {
        Objects.requireNonNull(employee, "employee");
        Objects.requireNonNull(escalation, "escalation");
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        if (startDate.isPresent() && endDate.isPresent() && endDate.get().isBefore(startDate.get())) {
            throw new IllegalArgumentException("endDate " + endDate.get() + " is before startDate " + startDate.get());
        }
    }

    /** Convenience constructor: no escalation, no match, no dates. */
    public static ContributionPolicy of(ContributionAmount employee) {
        return new ContributionPolicy(employee, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * Whether an {@link EmployerMatch} can legitimately be attached to
     * an account of the given type. True for 401(k) / 403(b) variants
     * (employer-sponsored defined-contribution plans); false for IRA,
     * HSA, taxable, cash, and pension accounts.
     */
    public static boolean matchAllowedFor(AccountType accountType) {
        return MATCH_ALLOWED_TYPES.contains(accountType);
    }
}
