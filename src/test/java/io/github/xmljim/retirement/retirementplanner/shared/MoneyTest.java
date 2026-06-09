/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@SuppressWarnings("PMD.TooManyMethods") // Value-type tests legitimately have many small focused methods.
class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency EUR = Currency.getInstance("EUR");

    // ---------- factories & canonical constructor ----------

    @Test
    void of_creates_money_with_normalized_scale() {
        var m = Money.of(new BigDecimal("12.5"), USD);
        assertThat(m.amount()).isEqualByComparingTo("12.5");
        assertThat(m.amount().scale()).isEqualTo(Money.INTERNAL_SCALE);
        assertThat(m.currency()).isEqualTo(USD);
    }

    @Test
    void usd_constructs_from_string_literal() {
        var m = Money.usd("12345.67");
        assertThat(m.amount()).isEqualByComparingTo("12345.67");
        assertThat(m.amount().scale()).isEqualTo(Money.INTERNAL_SCALE);
        assertThat(m.currency()).isEqualTo(USD);
    }

    @Test
    void zero_usd_constant_is_zero_dollars() {
        assertThat(Money.ZERO_USD.amount()).isEqualByComparingTo("0");
        assertThat(Money.ZERO_USD.amount().scale()).isEqualTo(Money.INTERNAL_SCALE);
        assertThat(Money.ZERO_USD.currency()).isEqualTo(USD);
    }

    @Test
    void canonical_constructor_rejects_null_amount() {
        assertThatThrownBy(() -> new Money(null, USD))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("amount");
    }

    @Test
    void canonical_constructor_rejects_null_currency() {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("currency");
    }

    @Test
    void canonical_constructor_normalizes_higher_scale_with_half_even() {
        // 1.0000005 at scale 6 with HALF_EVEN rounds to 1.000000 (5 with even preceding)
        var m = Money.of(new BigDecimal("1.0000005"), USD);
        assertThat(m.amount()).isEqualByComparingTo("1.000000");
        assertThat(m.amount().scale()).isEqualTo(Money.INTERNAL_SCALE);
    }

    @Test
    void equality_works_after_scale_normalization() {
        // Records use BigDecimal.equals which would fail without canonical
        // normalization: new BigDecimal("1.0").equals(new BigDecimal("1.00")) == false
        var a = Money.of(new BigDecimal("1.0"), USD);
        var b = Money.of(new BigDecimal("1.00"), USD);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void same_amount_different_currency_is_not_equal() {
        var a = Money.of(BigDecimal.ONE, USD);
        var b = Money.of(BigDecimal.ONE, EUR);
        assertThat(a).isNotEqualTo(b);
    }

    // ---------- arithmetic ----------

    @Test
    void plus_adds_within_currency() {
        var sum = Money.usd("10.50").plus(Money.usd("2.25"));
        assertThat(sum.amount()).isEqualByComparingTo("12.75");
    }

    @Test
    void minus_subtracts_within_currency() {
        var diff = Money.usd("10.50").minus(Money.usd("2.25"));
        assertThat(diff.amount()).isEqualByComparingTo("8.25");
    }

    @Test
    void times_multiplies_by_a_unitless_factor() {
        var doubled = Money.usd("12.50").times(BigDecimal.valueOf(2));
        assertThat(doubled.amount()).isEqualByComparingTo("25");
    }

    @Test
    void times_rounds_result_to_internal_scale() {
        // 1.0 * 0.0000001 = 0.00000001 → rounded to scale 6 = 0.000000 (HALF_EVEN, 1 < 5)
        var product = Money.usd("1.0").times(new BigDecimal("0.0000001"));
        assertThat(product.amount()).isEqualByComparingTo("0");
        assertThat(product.amount().scale()).isEqualTo(Money.INTERNAL_SCALE);
    }

    @Test
    void divided_by_divides_with_scale_6_rounding() {
        // 1 / 3 at scale 6 HALF_EVEN = 0.333333
        var third = Money.usd("1").dividedBy(new BigDecimal("3"));
        assertThat(third.amount()).isEqualByComparingTo("0.333333");
        assertThat(third.amount().scale()).isEqualTo(Money.INTERNAL_SCALE);
    }

    @Test
    void divided_by_zero_throws_arithmetic_exception() {
        assertThatThrownBy(() -> Money.usd("1").dividedBy(BigDecimal.ZERO)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void negate_returns_additive_inverse() {
        assertThat(Money.usd("10.00").negate().amount()).isEqualByComparingTo("-10.00");
        assertThat(Money.usd("-7.50").negate().amount()).isEqualByComparingTo("7.50");
        assertThat(Money.ZERO_USD.negate().amount()).isEqualByComparingTo("0");
    }

    // ---------- null-arg defenses ----------

    @Test
    void plus_rejects_null_other() {
        assertThatThrownBy(() -> Money.ZERO_USD.plus(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void times_rejects_null_factor() {
        assertThatThrownBy(() -> Money.ZERO_USD.times(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("factor");
    }

    @Test
    void divided_by_rejects_null_divisor() {
        assertThatThrownBy(() -> Money.ZERO_USD.dividedBy(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("divisor");
    }

    // ---------- cross-currency operations ----------

    @Test
    void plus_throws_on_currency_mismatch() {
        var dollars = Money.of(BigDecimal.ONE, USD);
        var euros = Money.of(BigDecimal.ONE, EUR);
        assertThatThrownBy(() -> dollars.plus(euros))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("EUR");
    }

    @Test
    void minus_throws_on_currency_mismatch() {
        var dollars = Money.of(BigDecimal.ONE, USD);
        var euros = Money.of(BigDecimal.ONE, EUR);
        assertThatThrownBy(() -> dollars.minus(euros))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("USD")
                .hasMessageContaining("EUR");
    }

    // ---------- HALF_EVEN tie-breaking at the internal scale ----------

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        // tie cases at scale 7 → scale 6 with HALF_EVEN: round to nearest even
        "1.0000015, 1.000002", // 5 ties, preceding 1 is odd → round up to 2
        "1.0000025, 1.000002", // 5 ties, preceding 2 is even → round down stays 2
        "1.0000035, 1.000004", // 5 ties, preceding 3 is odd → round up to 4
        "1.0000045, 1.000004", // 5 ties, preceding 4 is even → round down stays 4
        // non-tie cases
        "1.0000011, 1.000001", // < 5 → round down
        "1.0000019, 1.000002", // > 5 → round up
    })
    void half_even_rounding_at_internal_scale_boundary(String input, String expected) {
        assertThat(Money.usd(input).amount()).isEqualByComparingTo(expected);
    }

    // ---------- precision over many additions (NFR-7 / ADR-007) ----------

    @Test
    void summing_ten_cents_a_thousand_times_yields_exactly_one_hundred_dollars() {
        // The classic floating-point trap: 0.1 + 0.1 + ... 1000 times in
        // double would drift; with BigDecimal scale-6 it must be exact.
        var dime = Money.usd("0.10");
        var total = IntStream.range(0, 1000).mapToObj(i -> dime).reduce(Money.ZERO_USD, Money::plus);
        assertThat(total.amount()).isEqualByComparingTo("100");
        assertThat(total).isEqualTo(Money.usd("100"));
    }

    @Test
    void summing_one_third_dollar_three_times_does_not_drift() {
        var third = Money.usd("1").dividedBy(new BigDecimal("3"));
        var sum = third.plus(third).plus(third);
        // 0.333333 + 0.333333 + 0.333333 = 0.999999 (loss is bounded; expected at scale 6)
        assertThat(sum.amount()).isEqualByComparingTo("0.999999");
    }

    @Test
    void large_balance_arithmetic_remains_exact() {
        // Verify that scale-6 internal precision handles realistic balances
        // without precision loss.
        var balance = Money.usd("1234567.890123");
        var withDeposit = balance.plus(Money.usd("0.000001"));
        assertThat(withDeposit.amount()).isEqualByComparingTo("1234567.890124");
    }
}
