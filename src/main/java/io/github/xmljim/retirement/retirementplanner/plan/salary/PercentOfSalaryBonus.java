/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.math.BigDecimal;
import java.time.Month;
import java.util.Objects;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * {@link BonusPolicy} that pays a percentage of the prevailing salary
 * in {@link #payoutMonth()} each year (ADR-003). The rate is a
 * decimal — {@code 0.10} means 10 %.
 */
public record PercentOfSalaryBonus(BigDecimal pct, Month payoutMonth) implements BonusPolicy {

    public PercentOfSalaryBonus {
        Objects.requireNonNull(pct, "pct");
        Objects.requireNonNull(payoutMonth, "payoutMonth");
    }

    @Override
    public Money payout(Money baseSalary) {
        Objects.requireNonNull(baseSalary, "baseSalary");
        return baseSalary.times(pct);
    }
}
