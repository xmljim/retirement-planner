/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import java.time.YearMonth;

import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;

/**
 * REST view of a single {@link CashFlow} line.
 */
public record CashFlowDto(YearMonth period, long accountId, CashFlowKind kind, MoneyDto amount) {

    public static CashFlowDto from(CashFlow flow) {
        return new CashFlowDto(flow.period(), flow.accountId(), flow.kind(), MoneyDto.from(flow.amount()));
    }
}
