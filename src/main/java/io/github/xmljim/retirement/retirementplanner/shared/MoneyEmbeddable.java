/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import java.math.BigDecimal;
import java.util.Currency;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * JPA {@code @Embeddable} adapter for {@link Money}. Maps to two
 * columns on the host table: {@code <prefix>_amount} ({@code NUMERIC(19,6)})
 * and {@code <prefix>_currency} ({@code CHAR(3)}).
 *
 * <p>Public to {@code shared} so any module's JPA entities can embed
 * money values without each module redefining the same adapter. The
 * record-shaped {@link Money} value remains the only currency-aware
 * type the domain handles; this class exists purely so Hibernate has a
 * mutable, no-arg-friendly pair to materialize into.
 *
 * <p>Per ADR-007: amounts are stored at scale {@value Money#INTERNAL_SCALE}.
 * Hibernate's {@code @Embeddable} contract requires a no-arg constructor
 * and getters/setters; entities must override the column names with
 * {@code @AttributeOverrides} when more than one {@code MoneyEmbeddable}
 * lives on the same table.
 */
@Embeddable
public class MoneyEmbeddable {

    @Column(name = "amount", nullable = false, precision = 19, scale = Money.INTERNAL_SCALE)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currencyCode;

    /** Required by JPA. */
    public MoneyEmbeddable() {}

    private MoneyEmbeddable(BigDecimal amount, String currencyCode) {
        this.amount = amount;
        this.currencyCode = currencyCode;
    }

    /** Adapts a {@link Money} into a fresh embeddable. */
    public static MoneyEmbeddable from(Money money) {
        return new MoneyEmbeddable(money.amount(), money.currency().getCurrencyCode());
    }

    /** Reconstructs the {@link Money} value from the persisted columns. */
    public Money toMoney() {
        return Money.of(amount, Currency.getInstance(currencyCode));
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
