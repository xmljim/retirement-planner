/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable monetary value carrying a {@link BigDecimal} amount and a
 * {@link Currency}. The single representation of money everywhere in
 * this codebase per ADR-007.
 *
 * <p>Internal scale is {@value #INTERNAL_SCALE} with rounding mode
 * {@link RoundingMode#HALF_EVEN}. Amounts at any other scale are
 * normalized to scale 6 by the canonical constructor, which means
 * record equality compares amounts numerically as well as by currency.
 *
 * <p>Cross-currency arithmetic throws {@link IllegalArgumentException}.
 * V1 only uses USD; the {@code Currency} field exists so multi-currency
 * later isn't a refactor (ADR-007).
 *
 * <p>Display rounding (scale 2) is the responsibility of
 * {@link MoneyDisplay}; domain code never rounds for display.
 */
public record Money(BigDecimal amount, Currency currency) {

    /** Internal scale for all monetary arithmetic per ADR-007. */
    public static final int INTERNAL_SCALE = 6;

    /** Rounding mode for all monetary arithmetic per ADR-007. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private static final Currency USD = Currency.getInstance("USD");

    /** Zero dollars. The most-used constant; provided to avoid allocation churn. */
    public static final Money ZERO_USD = new Money(BigDecimal.ZERO, USD);

    /**
     * Canonical constructor. Rejects null inputs and normalizes the
     * amount to {@link #INTERNAL_SCALE} with {@link #ROUNDING}.
     *
     * @throws NullPointerException if {@code amount} or {@code currency} is null
     */
    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(INTERNAL_SCALE, ROUNDING);
    }

    /**
     * Constructs a {@code Money} for the given amount and currency.
     *
     * @param amount   the amount; any scale is normalized to {@link #INTERNAL_SCALE}
     * @param currency the currency
     * @return a new {@code Money}
     */
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /**
     * Constructs a USD {@code Money} from a string literal. Strings are
     * preferred over {@code double} to avoid binary-floating-point loss
     * (ADR-007).
     *
     * @param amount the amount as a decimal string, e.g. {@code "12345.67"}
     * @return a new USD {@code Money}
     */
    public static Money usd(String amount) {
        return of(new BigDecimal(amount), USD);
    }

    /**
     * Returns this plus {@code other}. Currencies must match.
     *
     * @throws IllegalArgumentException if the currencies differ
     */
    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    /**
     * Returns this minus {@code other}. Currencies must match.
     *
     * @throws IllegalArgumentException if the currencies differ
     */
    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    /**
     * Returns this multiplied by a unitless factor (e.g. an inflation
     * multiplier or a contribution percentage). The result is rounded
     * to {@link #INTERNAL_SCALE}.
     */
    public Money times(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor");
        return new Money(amount.multiply(factor), currency);
    }

    /**
     * Returns this divided by a unitless divisor. The result is rounded
     * to {@link #INTERNAL_SCALE}.
     *
     * @throws ArithmeticException if {@code divisor} is zero
     */
    public Money dividedBy(BigDecimal divisor) {
        Objects.requireNonNull(divisor, "divisor");
        return new Money(amount.divide(divisor, INTERNAL_SCALE, ROUNDING), currency);
    }

    /** Returns the additive inverse. */
    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + currency.getCurrencyCode() + " vs " + other.currency.getCurrencyCode());
        }
    }
}
