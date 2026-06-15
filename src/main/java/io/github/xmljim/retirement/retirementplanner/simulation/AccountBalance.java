/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation;

import java.util.Objects;

import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * End-of-month balance for one {@link Account} within a
 * {@link MonthlyProjection}. Aggregates across the account's sleeves
 * — sleeve-level breakdown isn't part of the projection contract
 * today; consumers can re-derive it from the cash-flow ledger and
 * sleeve yield engine if needed.
 */
public record AccountBalance(AccountId accountId, Money endingBalance) {

    public AccountBalance {
        Objects.requireNonNull(accountId, "accountId");
        Objects.requireNonNull(endingBalance, "endingBalance");
    }
}
