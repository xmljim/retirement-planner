/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.time.Month;
import java.util.Objects;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Lump-sum {@link BonusPolicy} that pays a fixed dollar amount in
 * {@link #payoutMonth()} each year, regardless of salary (ADR-003).
 */
public record FixedBonus(Money amount, Month payoutMonth) implements BonusPolicy {

    public FixedBonus {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(payoutMonth, "payoutMonth");
    }

    @Override
    public Money payout(Money baseSalary) {
        return amount;
    }
}
