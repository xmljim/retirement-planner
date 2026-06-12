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
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

class AccountRecordsTest {

    private static final PlanId PLAN = new PlanId(1L);
    private static final OwnerRef.Joint JOINT = new OwnerRef.Joint();

    @Test
    @DisplayName("Account rejects empty sleeve list")
    void accountRejectsEmptySleeves() {
        assertThatThrownBy(() -> Account.of(PLAN, AccountType.ROTH_IRA, JOINT, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Account rejects null sleeve list")
    void accountRejectsNullSleeves() {
        assertThatThrownBy(() -> Account.of(PLAN, AccountType.ROTH_IRA, JOINT, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Account rejects null planId")
    void accountRejectsNullPlanId() {
        AccountSleeve sleeve = defaultSleeve();
        assertThatThrownBy(() -> Account.of(null, AccountType.ROTH_IRA, JOINT, List.of(sleeve)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Account.withDefaultSleeve creates an AssetAllocation sleeve at the full balance")
    void accountWithDefaultSleeve() {
        Money balance = Money.usd("100000.00");
        Account account = Account.withDefaultSleeve(PLAN, AccountType.TRADITIONAL_IRA, JOINT, balance);

        assertThat(account.id()).isEmpty();
        assertThat(account.type()).isEqualTo(AccountType.TRADITIONAL_IRA);
        assertThat(account.sleeves()).hasSize(1);
        AccountSleeve sleeve = account.sleeves().get(0);
        assertThat(sleeve.kind()).isInstanceOf(SleeveKind.AssetAllocation.class);
        assertThat(sleeve.yieldPolicy()).isInstanceOf(SleeveYieldPolicy.TracksAllocation.class);
        assertThat(sleeve.balance()).isEqualTo(balance);
    }

    @Test
    @DisplayName("Account defensively copies the sleeve list")
    void accountCopiesSleeves() {
        Account account = Account.withDefaultSleeve(PLAN, AccountType.ROTH_IRA, JOINT, Money.usd("1.00"));
        assertThatThrownBy(() -> account.sleeves().add(defaultSleeve()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("AccountSleeve rejects null kind / balance / yield")
    void accountSleeveRejectsNulls() {
        Money one = Money.usd("1.00");
        SleeveKind cash = new SleeveKind.Cash();
        SleeveYieldPolicy mm = new SleeveYieldPolicy.MoneyMarket();
        assertThatThrownBy(() -> AccountSleeve.of(null, one, mm)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccountSleeve.of(cash, null, mm)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> AccountSleeve.of(cash, one, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("OwnerRef.Individual rejects null personId")
    void ownerIndividualRejectsNullPersonId() {
        assertThatThrownBy(() -> new OwnerRef.Individual(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("SleeveKind.FixedAllocation rejects empty / null weights")
    void fixedAllocationRejectsEmptyWeights() {
        assertThatThrownBy(() -> new SleeveKind.FixedAllocation(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SleeveKind.FixedAllocation(Map.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SleeveKind.FixedAllocation defensively copies weights")
    void fixedAllocationCopiesWeights() {
        SleeveKind.FixedAllocation fa =
                new SleeveKind.FixedAllocation(Map.of("EQUITY", new BigDecimal("0.6"), "BOND", new BigDecimal("0.4")));
        assertThatThrownBy(() -> fa.weights().put("CASH", BigDecimal.ZERO))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("SleeveYieldPolicy.FixedRate rejects null annual rate")
    void fixedRateRejectsNullRate() {
        assertThatThrownBy(() -> new SleeveYieldPolicy.FixedRate(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("ID records carry their value")
    void idsCarryValue() {
        assertThat(new AccountId(11L).value()).isEqualTo(11L);
        assertThat(new SleeveId(12L).value()).isEqualTo(12L);
    }

    private AccountSleeve defaultSleeve() {
        return AccountSleeve.of(
                new SleeveKind.AssetAllocation(), Money.usd("1.00"), new SleeveYieldPolicy.TracksAllocation());
    }
}
