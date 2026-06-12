/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;
import io.github.xmljim.retirement.retirementplanner.shared.Money;
import io.github.xmljim.retirement.retirementplanner.shared.TimeSeries;

/**
 * Domain wrapper around {@link TimeSeries} of {@link CashFlow} entries
 * with engine-specific filters: by year, account, account-set, or
 * kind-set.
 *
 * <p>This is the contribution engine's primary state-threading
 * mechanism — instead of passing an explicit accumulator, the caller
 * threads a ledger through 12 months and the engine queries it for
 * year-to-date totals when applying §402(g), §408, §223, and §415(c)
 * caps.
 *
 * <p>{@link CashFlow} stores the account reference as a bare
 * {@code long} so {@code shared/} stays a leaf module; the ledger
 * accepts and exposes typed {@link AccountId} at the boundary.
 */
public record CashFlowLedger(TimeSeries<CashFlow> series) {

    public CashFlowLedger {
        Objects.requireNonNull(series, "series");
    }

    /** An empty ledger. */
    public static CashFlowLedger empty() {
        return new CashFlowLedger(TimeSeries.empty());
    }

    /** Returns a new ledger with {@code flow} appended. */
    public CashFlowLedger append(CashFlow flow) {
        return new CashFlowLedger(series.append(flow));
    }

    /** Returns a new ledger with all of {@code flows} appended. */
    public CashFlowLedger appendAll(Collection<CashFlow> flows) {
        return new CashFlowLedger(series.appendAll(flows));
    }

    /** Filters to flows in the given calendar year. */
    public CashFlowLedger forYear(int year) {
        return new CashFlowLedger(series.where(f -> f.period().getYear() == year));
    }

    /** Filters to flows for a single account. */
    public CashFlowLedger forAccount(AccountId accountId) {
        Objects.requireNonNull(accountId, "accountId");
        return new CashFlowLedger(series.where(f -> f.accountId() == accountId.value()));
    }

    /** Filters to flows for any of the given accounts. */
    public CashFlowLedger forAccounts(Collection<AccountId> accountIds) {
        Objects.requireNonNull(accountIds, "accountIds");
        Set<Long> ids = accountIds.stream().map(AccountId::value).collect(Collectors.toUnmodifiableSet());
        return new CashFlowLedger(series.where(f -> ids.contains(f.accountId())));
    }

    /** Filters to flows whose kind is in {@code kinds}. */
    public CashFlowLedger forKinds(Set<CashFlowKind> kinds) {
        Objects.requireNonNull(kinds, "kinds");
        Set<CashFlowKind> snapshot = EnumSet.copyOf(kinds);
        return new CashFlowLedger(series.where(f -> snapshot.contains(f.kind())));
    }

    /** Total of all flows in the (filtered) ledger. */
    public Money total() {
        return series.sumOf(CashFlow::amount);
    }

    /** Number of flows in the (filtered) ledger. */
    public int size() {
        return series.size();
    }

    /** Whether the (filtered) ledger has no flows. */
    public boolean isEmpty() {
        return series.isEmpty();
    }
}
