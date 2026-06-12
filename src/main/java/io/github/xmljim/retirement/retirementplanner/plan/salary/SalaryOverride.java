/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.salary;

import java.time.LocalDate;
import java.util.Objects;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Discrete salary change effective on {@code effectiveDate} — e.g. a
 * promotion, job change, or other known step (ADR-003).
 *
 * <p>An override anchors the salary at {@code newSalary} on its
 * {@link #effectiveDate}; subsequent annual growth resumes from that
 * point per the owning {@link SalaryProfile}'s growth schedule.
 */
public record SalaryOverride(LocalDate effectiveDate, Money newSalary) {

    public SalaryOverride {
        Objects.requireNonNull(effectiveDate, "effectiveDate");
        Objects.requireNonNull(newSalary, "newSalary");
    }
}
