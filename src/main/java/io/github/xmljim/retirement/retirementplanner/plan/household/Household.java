/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.household;

import java.util.Optional;

/**
 * Demographic context for a {@link Plan} (ADR-002).
 *
 * <p>{@code state} is the two-letter US state code used by the tax
 * engine (ADR-004) to resolve state brackets. {@code id} is absent
 * before persistence; the repository populates it on save.
 */
public record Household(Optional<HouseholdId> id, FilingStatus filingStatus, String state) {

    public Household {
        if (filingStatus == null) {
            throw new IllegalArgumentException("filingStatus is required");
        }
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("state is required");
        }
    }

    public static Household of(FilingStatus filingStatus, String state) {
        return new Household(Optional.empty(), filingStatus, state);
    }
}
