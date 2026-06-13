/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;

/**
 * One month of a deterministic accumulation projection (S-2.8,
 * FR-7.1, FR-7.2).
 *
 * <p>Holds the per-account end-of-month balances and the cash flows
 * that occurred during the month. Cash flows are kept flat; consumers
 * (REST DTO, CSV exporter) aggregate by account when needed.
 */
public record MonthlyProjection(
        YearMonth period, ProjectionPhase phase, List<AccountBalance> accountBalances, List<CashFlow> cashFlows) {

    public MonthlyProjection {
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(accountBalances, "accountBalances");
        Objects.requireNonNull(cashFlows, "cashFlows");
        accountBalances = List.copyOf(accountBalances);
        cashFlows = List.copyOf(cashFlows);
    }
}
