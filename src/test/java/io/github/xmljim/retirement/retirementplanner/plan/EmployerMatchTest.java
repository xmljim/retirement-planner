/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmployerMatchTest {

    private static final BigDecimal THREE_PCT = new BigDecimal("0.03");
    private static final BigDecimal FIVE_PCT = new BigDecimal("0.05");
    private static final BigDecimal HUNDRED_PCT = new BigDecimal("1.00");
    private static final BigDecimal HALF = new BigDecimal("0.50");

    @Test
    @DisplayName("single tier: employee under cap → match scales with employee pct")
    void singleTierUnderCap() {
        EmployerMatch match = new EmployerMatch(List.of(new MatchTier(THREE_PCT, HUNDRED_PCT)));
        assertThat(match.matchPct(new BigDecimal("0.02"))).isEqualByComparingTo("0.0200");
    }

    @Test
    @DisplayName("single tier: employee at cap → match equals tier ceiling × match rate")
    void singleTierAtCap() {
        EmployerMatch match = new EmployerMatch(List.of(new MatchTier(THREE_PCT, HUNDRED_PCT)));
        assertThat(match.matchPct(THREE_PCT)).isEqualByComparingTo("0.0300");
    }

    @Test
    @DisplayName("single tier: employee over cap → match clamps at the tier ceiling")
    void singleTierOverCap() {
        EmployerMatch match = new EmployerMatch(List.of(new MatchTier(THREE_PCT, HUNDRED_PCT)));
        assertThat(match.matchPct(new BigDecimal("0.10"))).isEqualByComparingTo("0.0300");
    }

    @Test
    @DisplayName("two tiers: employee in first tier")
    void twoTiersFirst() {
        EmployerMatch match =
                new EmployerMatch(List.of(new MatchTier(THREE_PCT, HUNDRED_PCT), new MatchTier(FIVE_PCT, HALF)));
        assertThat(match.matchPct(new BigDecimal("0.02"))).isEqualByComparingTo("0.02");
    }

    @Test
    @DisplayName("two tiers: employee at first-tier cap → no second-tier earnings")
    void twoTiersAtFirstTierCap() {
        EmployerMatch match =
                new EmployerMatch(List.of(new MatchTier(THREE_PCT, HUNDRED_PCT), new MatchTier(FIVE_PCT, HALF)));
        assertThat(match.matchPct(THREE_PCT)).isEqualByComparingTo("0.03");
    }

    @Test
    @DisplayName("two tiers: employee mid-second-tier → first tier full, second pro-rata")
    void twoTiersMidSecondTier() {
        EmployerMatch match =
                new EmployerMatch(List.of(new MatchTier(THREE_PCT, HUNDRED_PCT), new MatchTier(FIVE_PCT, HALF)));
        // 3% × 100% + 1% × 50% = 0.035
        assertThat(match.matchPct(new BigDecimal("0.04"))).isEqualByComparingTo("0.0350");
    }

    @Test
    @DisplayName("two tiers: employee at top tier cap → both tiers fully earned")
    void twoTiersAtTopCap() {
        EmployerMatch match =
                new EmployerMatch(List.of(new MatchTier(THREE_PCT, HUNDRED_PCT), new MatchTier(FIVE_PCT, HALF)));
        // 3% + 2% × 50% = 0.04
        assertThat(match.matchPct(FIVE_PCT)).isEqualByComparingTo("0.04");
    }

    @Test
    @DisplayName("two tiers: employee over top tier cap → match clamps at top tier")
    void twoTiersOverTopCap() {
        EmployerMatch match =
                new EmployerMatch(List.of(new MatchTier(THREE_PCT, HUNDRED_PCT), new MatchTier(FIVE_PCT, HALF)));
        assertThat(match.matchPct(new BigDecimal("0.10"))).isEqualByComparingTo("0.04");
    }

    @Test
    @DisplayName("zero or negative employee pct → zero match")
    void nonPositiveEmployeePct() {
        EmployerMatch match = new EmployerMatch(List.of(new MatchTier(THREE_PCT, HUNDRED_PCT)));
        assertThat(match.matchPct(BigDecimal.ZERO)).isEqualByComparingTo("0");
        assertThat(match.matchPct(new BigDecimal("-0.01"))).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("EmployerMatch rejects empty tier list")
    void rejectsEmpty() {
        assertThatThrownBy(() -> new EmployerMatch(List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("EmployerMatch rejects non-ascending tiers")
    void rejectsNonAscending() {
        assertThatThrownBy(() -> new EmployerMatch(
                        List.of(new MatchTier(FIVE_PCT, HALF), new MatchTier(THREE_PCT, HUNDRED_PCT))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("EmployerMatch rejects duplicate tier ceilings")
    void rejectsDuplicateCeilings() {
        assertThatThrownBy(() -> new EmployerMatch(
                        List.of(new MatchTier(THREE_PCT, HUNDRED_PCT), new MatchTier(THREE_PCT, HALF))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("EmployerMatch rejects null tiers")
    void rejectsNullTiers() {
        assertThatThrownBy(() -> new EmployerMatch(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("MatchTier rejects non-positive ceiling and negative match rate")
    void matchTierRejectsBadInputs() {
        assertThatThrownBy(() -> new MatchTier(BigDecimal.ZERO, HUNDRED_PCT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MatchTier(new BigDecimal("-0.01"), HUNDRED_PCT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MatchTier(THREE_PCT, new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
