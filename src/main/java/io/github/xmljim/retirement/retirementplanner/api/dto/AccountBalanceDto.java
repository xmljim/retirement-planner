/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import io.github.xmljim.retirement.retirementplanner.simulation.AccountBalance;

/**
 * REST view of an end-of-month account balance line within a
 * {@link MonthlyProjectionDto}.
 */
public record AccountBalanceDto(long accountId, MoneyDto endingBalance) {

    public static AccountBalanceDto from(AccountBalance balance) {
        return new AccountBalanceDto(balance.accountId().value(), MoneyDto.from(balance.endingBalance()));
    }
}
