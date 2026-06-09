/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MoneyDisplayTest {

    @Test
    void display_scale_constant_is_two() {
        assertThat(MoneyDisplay.DISPLAY_SCALE).isEqualTo(2);
    }

    @Test
    void rounds_internal_scale_six_to_display_scale_two() {
        var m = Money.usd("12.345678");
        assertThat(MoneyDisplay.toDisplay(m)).isEqualByComparingTo("12.35");
        assertThat(MoneyDisplay.toDisplay(m).scale()).isEqualTo(2);
    }

    @Test
    void preserves_amounts_already_at_display_scale() {
        assertThat(MoneyDisplay.toDisplay(Money.usd("12.34"))).isEqualByComparingTo("12.34");
    }

    @Test
    void zero_renders_as_zero_at_scale_two() {
        assertThat(MoneyDisplay.toDisplay(Money.ZERO_USD)).isEqualByComparingTo("0.00");
        assertThat(MoneyDisplay.toDisplay(Money.ZERO_USD).scale()).isEqualTo(2);
    }

    @Test
    void negative_amounts_round_with_half_even() {
        // -12.345 → tie at the cents boundary; HALF_EVEN picks the even neighbor
        assertThat(MoneyDisplay.toDisplay(Money.usd("-12.345"))).isEqualByComparingTo("-12.34");
    }

    // HALF_EVEN tie-breaking at the cents boundary (scale 6 → scale 2).
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "12.345, 12.34", // 5 ties, preceding 4 is even → stays 4
        "12.355, 12.36", // 5 ties, preceding 5 is odd  → up to 6
        "12.341, 12.34", // < 5
        "12.349, 12.35", // > 5
        "12.350001, 12.35", // not a tie — > 5
        "0.005, 0.00", // 5 ties, preceding 0 is even → stays 0
        "0.015, 0.02", // 5 ties, preceding 1 is odd → up to 2
    })
    void half_even_rounding_at_display_scale_boundary(String input, String expected) {
        assertThat(MoneyDisplay.toDisplay(Money.usd(input))).isEqualByComparingTo(expected);
    }
}
