/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation.internal;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Splits a single account's monthly contribution dollar amount across
 * its sleeves pro-rata by the start-of-month sleeve balance. When all
 * sleeves are zero, the first sleeve receives the full contribution
 * — the only deterministic choice when there's no signal to allocate
 * by.
 *
 * <p>Rounding: each sleeve's share is computed at {@link Money#INTERNAL_SCALE},
 * and the last sleeve absorbs the residual so the sum lands exactly
 * on {@code contribution} regardless of the {@code HALF_EVEN}
 * intermediate rounding.
 */
final class ContributionDistributor {

    private ContributionDistributor() {}

    static void distribute(List<MutableSleeve> sleeves, Money totalStart, Money contribution) {
        if (totalStart.amount().signum() == 0) {
            sleeves.get(0).add(contribution);
            return;
        }
        Money[] remaining = {contribution};
        IntStream.range(0, sleeves.size() - 1).forEach(i -> {
            MutableSleeve sleeve = sleeves.get(i);
            BigDecimal share =
                    sleeve.currentBalance().amount().divide(totalStart.amount(), Money.INTERNAL_SCALE, Money.ROUNDING);
            Money portion = contribution.times(share);
            sleeve.add(portion);
            remaining[0] = remaining[0].minus(portion);
        });
        sleeves.get(sleeves.size() - 1).add(remaining[0]);
    }
}
