/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import java.math.BigDecimal;

/**
 * Boundary helper that rounds a {@link Money}'s amount to the display
 * scale of 2 dollars (cents) per ADR-007 §Display Scale. Locale-aware
 * formatting (currency symbol, grouping, negative sign placement) is a
 * view-layer concern and lives elsewhere; this helper produces the
 * unambiguous numeric value that the view layer formats.
 *
 * <p>Domain code does not round for display. Calls to this method
 * should appear only at DTO mapping or report rendering boundaries.
 */
public final class MoneyDisplay {

    /** Display scale per ADR-007. */
    public static final int DISPLAY_SCALE = 2;

    private MoneyDisplay() {}

    /**
     * Returns the amount rounded to {@link #DISPLAY_SCALE} using
     * {@link Money#ROUNDING}.
     */
    public static BigDecimal toDisplay(Money money) {
        return money.amount().setScale(DISPLAY_SCALE, Money.ROUNDING);
    }
}
