/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

class CashFlowLedgerTest {

    private static final AccountId ACCT_1 = new AccountId(1L);
    private static final AccountId ACCT_2 = new AccountId(2L);
    private static final String ONE_HUNDRED = "100";
    private static final String TWO_HUNDRED = "200";
    private static final YearMonth JAN_2026 = YearMonth.of(2026, 1);
    private static final YearMonth DEC_2025 = YearMonth.of(2025, 12);

    @Test
    @DisplayName("empty ledger has zero total")
    void emptyLedger() {
        CashFlowLedger ledger = CashFlowLedger.empty();
        assertThat(ledger.isEmpty()).isTrue();
        assertThat(ledger.total()).isEqualTo(Money.ZERO_USD);
    }

    @Test
    @DisplayName("forYear filters across calendar years")
    void forYearFilters() {
        CashFlowLedger ledger = CashFlowLedger.empty()
                .append(flow(DEC_2025, 1L, CashFlowKind.EMPLOYEE_PRETAX, ONE_HUNDRED))
                .append(flow(JAN_2026, 1L, CashFlowKind.EMPLOYEE_PRETAX, TWO_HUNDRED))
                .append(flow(YearMonth.of(2026, 6), 1L, CashFlowKind.EMPLOYEE_PRETAX, "300"));
        assertThat(ledger.forYear(2026).total()).isEqualTo(Money.usd("500"));
        assertThat(ledger.forYear(2025).total()).isEqualTo(Money.usd(ONE_HUNDRED));
    }

    @Test
    @DisplayName("forAccount filters to a single account")
    void forAccountFilters() {
        CashFlowLedger ledger = CashFlowLedger.empty()
                .append(flow(JAN_2026, 1L, CashFlowKind.EMPLOYEE_PRETAX, ONE_HUNDRED))
                .append(flow(JAN_2026, 2L, CashFlowKind.EMPLOYEE_PRETAX, TWO_HUNDRED));
        assertThat(ledger.forAccount(ACCT_1).total()).isEqualTo(Money.usd(ONE_HUNDRED));
        assertThat(ledger.forAccount(ACCT_2).total()).isEqualTo(Money.usd(TWO_HUNDRED));
    }

    @Test
    @DisplayName("forAccounts filters to a set of accounts")
    void forAccountsFilters() {
        AccountId acct3 = new AccountId(3L);
        CashFlowLedger ledger = CashFlowLedger.empty()
                .append(flow(JAN_2026, 1L, CashFlowKind.EMPLOYEE_PRETAX, ONE_HUNDRED))
                .append(flow(JAN_2026, 2L, CashFlowKind.EMPLOYEE_PRETAX, TWO_HUNDRED))
                .append(flow(JAN_2026, 3L, CashFlowKind.EMPLOYEE_PRETAX, "400"));
        assertThat(ledger.forAccounts(List.of(ACCT_1, acct3)).total()).isEqualTo(Money.usd("500"));
    }

    @Test
    @DisplayName("forKinds filters to a kind set")
    void forKindsFilters() {
        CashFlowLedger ledger = CashFlowLedger.empty()
                .append(flow(JAN_2026, 1L, CashFlowKind.EMPLOYEE_PRETAX, ONE_HUNDRED))
                .append(flow(JAN_2026, 1L, CashFlowKind.EMPLOYER_MATCH, "50"));
        assertThat(ledger.forKinds(EnumSet.of(CashFlowKind.EMPLOYER_MATCH)).total())
                .isEqualTo(Money.usd("50"));
    }

    @Test
    @DisplayName("filters compose")
    void filtersCompose() {
        CashFlowLedger ledger = CashFlowLedger.empty()
                .append(flow(DEC_2025, 1L, CashFlowKind.EMPLOYEE_PRETAX, ONE_HUNDRED))
                .append(flow(JAN_2026, 1L, CashFlowKind.EMPLOYEE_PRETAX, TWO_HUNDRED))
                .append(flow(JAN_2026, 1L, CashFlowKind.EMPLOYER_MATCH, "75"))
                .append(flow(JAN_2026, 2L, CashFlowKind.EMPLOYEE_PRETAX, "300"));
        Money ytdEmployeeAcct1 = ledger.forYear(2026)
                .forAccount(ACCT_1)
                .forKinds(EnumSet.of(CashFlowKind.EMPLOYEE_PRETAX))
                .total();
        assertThat(ytdEmployeeAcct1).isEqualTo(Money.usd(TWO_HUNDRED));
    }

    @Test
    @DisplayName("appendAll adds many flows in order")
    void appendAll() {
        CashFlowLedger ledger = CashFlowLedger.empty()
                .appendAll(List.of(
                        flow(JAN_2026, 1L, CashFlowKind.EMPLOYEE_PRETAX, "1"),
                        flow(YearMonth.of(2026, 2), 1L, CashFlowKind.EMPLOYEE_PRETAX, "2")));
        assertThat(ledger.size()).isEqualTo(2);
        assertThat(ledger.total()).isEqualTo(Money.usd("3"));
    }

    private static CashFlow flow(YearMonth period, long accountId, CashFlowKind kind, String amount) {
        return new CashFlow(period, accountId, kind, Money.usd(amount));
    }
}
