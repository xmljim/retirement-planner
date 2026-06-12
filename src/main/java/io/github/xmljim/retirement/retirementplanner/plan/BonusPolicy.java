/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.time.Month;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Annual bonus configuration paid in {@link #payoutMonth()} each year
 * (ADR-003). Sealed: a bonus is either a fixed dollar amount
 * ({@link FixedBonus}) or a percentage of the prevailing salary
 * ({@link PercentOfSalaryBonus}).
 *
 * <p>{@link #payout(Money)} returns the bonus amount, given the
 * salary at the payout date. Implementations decide whether to
 * consult {@code baseSalary}.
 */
public sealed interface BonusPolicy permits FixedBonus, PercentOfSalaryBonus {

    /** The calendar month in which the bonus is paid. */
    Month payoutMonth();

    /**
     * Computes the bonus amount.
     *
     * @param baseSalary the salary at the payout date — used by
     *                   percentage policies; ignored by fixed policies
     */
    Money payout(Money baseSalary);
}
