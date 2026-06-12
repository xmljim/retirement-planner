/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import java.math.BigDecimal;
import java.util.Currency;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Wire representation of {@link Money} as
 * {@code {"amount":"12345.67","currency":"USD"}} per ADR-007. The amount
 * is a JSON string — never a number — so JS clients don't lose precision
 * to {@code IEEE-754} on parse.
 */
public record MoneyDto(
        @NotNull @Pattern(regexp = "-?\\d+(\\.\\d+)?", message = "amount must be a decimal string")
        String amount,

        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "currency must be an ISO-4217 alphabetic code")
        String currency) {

    /** Build a DTO from the domain {@link Money}, rendering the amount at internal scale (6). */
    public static MoneyDto from(Money money) {
        return new MoneyDto(money.amount().toPlainString(), money.currency().getCurrencyCode());
    }

    /** Build a domain {@link Money} from this DTO. */
    public Money toMoney() {
        return Money.of(new BigDecimal(amount), Currency.getInstance(currency));
    }
}
