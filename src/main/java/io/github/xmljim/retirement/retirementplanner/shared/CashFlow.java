/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import java.time.YearMonth;
import java.util.Objects;

/**
 * One audit-grade line item in the cash-flow ledger (ADR-003 §"Engine
 * Behavior", point 7).
 *
 * <p>Cross-cutting per ADR-008: produced by {@code contribution/},
 * consumed by {@code tax/}, {@code simulation/}, and {@code api/}.
 * Lives in {@code shared/} to avoid forcing every consumer to depend
 * on {@code contribution/}.
 *
 * <p>{@code accountId} is held as a {@code long} (the bare
 * {@code AccountId.value()}) rather than the strongly-typed
 * {@code AccountId} record so that {@code shared/} stays a leaf
 * module — {@code shared} cannot depend on {@code plan} per the
 * Modulith layering. Domain wrappers in consuming modules
 * (e.g. {@code CashFlowLedger}) re-attach the typed identifier when
 * filtering.
 *
 * @param period    the calendar month this flow occurs in
 * @param accountId the {@code AccountId.value()} of the affected account
 * @param kind      what the flow represents (employee deferral, match, …)
 * @param amount    the flow amount; non-negative
 */
public record CashFlow(YearMonth period, long accountId, CashFlowKind kind, Money amount) {

    public CashFlow {
        Objects.requireNonNull(period, "period");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(amount, "amount");
        if (amount.amount().signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative: " + amount);
        }
    }
}
