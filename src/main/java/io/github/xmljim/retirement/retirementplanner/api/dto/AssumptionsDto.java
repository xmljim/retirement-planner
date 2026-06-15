/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import java.math.BigDecimal;

import io.github.xmljim.retirement.retirementplanner.plan.Assumptions;

import jakarta.validation.constraints.NotNull;

/**
 * Plan-wide deterministic projection inputs (S-2.8, FR-5.1). Rates
 * are decimal fractions (0.07 for 7%), not percentages.
 */
public record AssumptionsDto(
        @NotNull BigDecimal preRetirementReturnRate,
        @NotNull BigDecimal cashInterestRate) {

    public static AssumptionsDto from(Assumptions assumptions) {
        return new AssumptionsDto(assumptions.preRetirementReturnRate(), assumptions.cashInterestRate());
    }

    public Assumptions toRecord() {
        return new Assumptions(preRetirementReturnRate, cashInterestRate);
    }
}
