/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.account;

import java.util.Objects;
import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * A slice of an {@link Account} by asset class or behavior (ADR-002).
 *
 * <p>A Traditional IRA may hold a {@link SleeveKind.Cash} sleeve
 * yielding the money-market rate alongside an
 * {@link SleeveKind.AssetAllocation} sleeve subject to glide-path
 * returns. Tax treatment is per-account, not per-sleeve — sleeves
 * inherit their host account's tax rules.
 *
 * <p>{@code id} is absent before persistence; the repository populates
 * it on save.
 */
public record AccountSleeve(Optional<SleeveId> id, SleeveKind kind, Money balance, SleeveYieldPolicy yieldPolicy) {

    public AccountSleeve {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(balance, "balance");
        Objects.requireNonNull(yieldPolicy, "yieldPolicy");
    }

    /** Convenience constructor for an unpersisted sleeve. */
    public static AccountSleeve of(SleeveKind kind, Money balance, SleeveYieldPolicy yieldPolicy) {
        return new AccountSleeve(Optional.empty(), kind, balance, yieldPolicy);
    }
}
