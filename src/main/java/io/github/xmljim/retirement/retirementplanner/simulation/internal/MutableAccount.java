/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.shared.Money;
import io.github.xmljim.retirement.retirementplanner.simulation.AccountBalance;

/**
 * Mutable per-account state held by the projector while it walks
 * months. Wraps the immutable {@link Account} record and the list of
 * {@link MutableSleeve}s so contributions and yield can compose
 * efficiently in-place during a long projection.
 *
 * <p>Package-private intentionally — leaks internal mutation;
 * exposing it would invert the {@code Account} record's immutability
 * guarantee.
 */
final class MutableAccount {

    private final Account sourceAccount;
    private final List<MutableSleeve> mutableSleeves;

    private MutableAccount(Account sourceAccount, List<MutableSleeve> mutableSleeves) {
        this.sourceAccount = sourceAccount;
        this.mutableSleeves = mutableSleeves;
    }

    static MutableAccount of(Account account) {
        List<MutableSleeve> sleeves =
                account.sleeves().stream().map(MutableSleeve::of).toList();
        return new MutableAccount(account, new ArrayList<>(sleeves));
    }

    static Map<AccountId, MutableAccount> initialState(List<Account> accounts) {
        Map<AccountId, MutableAccount> state = new HashMap<>();
        accounts.forEach(account -> {
            AccountId id = account.id()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "AccumulationProjector requires persisted accounts (id present)"));
            state.put(id, of(account));
        });
        return state;
    }

    static List<AccountBalance> snapshotBalances(Map<AccountId, MutableAccount> state) {
        return state.entrySet().stream()
                .map(e -> new AccountBalance(e.getKey(), e.getValue().endingBalance()))
                .sorted(Comparator.comparingLong(b -> b.accountId().value()))
                .toList();
    }

    Account source() {
        return sourceAccount;
    }

    List<MutableSleeve> sleeves() {
        return mutableSleeves;
    }

    Money endingBalance() {
        return mutableSleeves.stream().map(MutableSleeve::currentBalance).reduce(Money.ZERO_USD, Money::plus);
    }
}
