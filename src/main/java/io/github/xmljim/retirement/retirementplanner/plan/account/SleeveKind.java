/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Discriminates an {@link AccountSleeve}'s allocation behavior (ADR-002).
 *
 * <p>The default for any new account is a single
 * {@link AssetAllocation} sleeve holding the full balance — the
 * glide-path policy from {@code allocation/} (ADR-005) governs its
 * weights. Power users can split an account into a {@link Cash} sleeve
 * (real cash earning yield) plus equity, or pin a sleeve to a static
 * {@link FixedAllocation} that ignores the glide path.
 */
public sealed interface SleeveKind {

    /** Cash held inside a wrapper account; yields per the sleeve's yield policy. */
    record Cash() implements SleeveKind {}

    /** Glide-path-managed allocation — weights resolved by the allocation policy at runtime. */
    record AssetAllocation() implements SleeveKind {}

    /**
     * Static allocation that opts out of the glide path. Weights are
     * keyed by asset-class identifier (string until {@code allocation/}
     * defines an enum); values must sum to 1.0 within rounding.
     */
    record FixedAllocation(Map<String, BigDecimal> weights) implements SleeveKind {

        public FixedAllocation {
            Objects.requireNonNull(weights, "weights");
            if (weights.isEmpty()) {
                throw new IllegalArgumentException("FixedAllocation requires at least one weight");
            }
            weights = Map.copyOf(weights);
        }
    }
}
