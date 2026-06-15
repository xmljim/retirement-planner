/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import java.time.YearMonth;
import java.util.List;

import io.github.xmljim.retirement.retirementplanner.simulation.MonthlyProjection;
import io.github.xmljim.retirement.retirementplanner.simulation.ProjectionPhase;

/**
 * REST view of one {@link MonthlyProjection}.
 */
public record MonthlyProjectionDto(
        YearMonth period, ProjectionPhase phase, List<AccountBalanceDto> accountBalances, List<CashFlowDto> cashFlows) {

    public static MonthlyProjectionDto from(MonthlyProjection projection) {
        return new MonthlyProjectionDto(
                projection.period(),
                projection.phase(),
                projection.accountBalances().stream()
                        .map(AccountBalanceDto::from)
                        .toList(),
                projection.cashFlows().stream().map(CashFlowDto::from).toList());
    }
}
