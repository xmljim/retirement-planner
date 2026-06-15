/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation.internal;

import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.plan.account.AccountSleeve;
import io.github.xmljim.retirement.retirementplanner.plan.account.SleeveId;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Mutable per-sleeve state. The projector applies yield and
 * contributions to {@link #currentBalance()} in place and reconstructs
 * an immutable {@link AccountSleeve} on demand for the yield engine.
 */
final class MutableSleeve {

    private final Optional<SleeveId> id;
    private final AccountSleeve template;
    private Money runningBalance;

    private MutableSleeve(AccountSleeve source) {
        this.id = source.id();
        this.template = source;
        this.runningBalance = source.balance();
    }

    static MutableSleeve of(AccountSleeve source) {
        return new MutableSleeve(source);
    }

    Money currentBalance() {
        return runningBalance;
    }

    void add(Money delta) {
        runningBalance = runningBalance.plus(delta);
    }

    AccountSleeve toRecord() {
        return new AccountSleeve(id, template.kind(), runningBalance, template.yieldPolicy());
    }
}
