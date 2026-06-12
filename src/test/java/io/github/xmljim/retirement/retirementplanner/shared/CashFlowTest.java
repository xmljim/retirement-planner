/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.YearMonth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CashFlowTest {

    private static final YearMonth JAN_2026 = YearMonth.of(2026, 1);

    @Test
    @DisplayName("constructor accepts a valid flow")
    void validFlow() {
        CashFlow flow = new CashFlow(JAN_2026, 42L, CashFlowKind.EMPLOYEE_PRETAX, Money.usd("500"));
        assertThat(flow.period()).isEqualTo(JAN_2026);
        assertThat(flow.accountId()).isEqualTo(42L);
        assertThat(flow.kind()).isEqualTo(CashFlowKind.EMPLOYEE_PRETAX);
        assertThat(flow.amount()).isEqualTo(Money.usd("500"));
    }

    @Test
    @DisplayName("zero amount is allowed")
    void zeroAmountAllowed() {
        CashFlow flow = new CashFlow(JAN_2026, 1L, CashFlowKind.EMPLOYER_MATCH, Money.ZERO_USD);
        assertThat(flow.amount()).isEqualTo(Money.ZERO_USD);
    }

    @Test
    @DisplayName("negative amount rejected")
    void negativeAmountRejected() {
        assertThatThrownBy(() -> new CashFlow(JAN_2026, 1L, CashFlowKind.EMPLOYEE_PRETAX, Money.usd("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    @DisplayName("null period rejected")
    void nullPeriodRejected() {
        assertThatThrownBy(() -> new CashFlow(null, 1L, CashFlowKind.EMPLOYEE_PRETAX, Money.ZERO_USD))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("period");
    }

    @Test
    @DisplayName("null kind rejected")
    void nullKindRejected() {
        assertThatThrownBy(() -> new CashFlow(JAN_2026, 1L, null, Money.ZERO_USD))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("kind");
    }

    @Test
    @DisplayName("null amount rejected")
    void nullAmountRejected() {
        assertThatThrownBy(() -> new CashFlow(JAN_2026, 1L, CashFlowKind.EMPLOYEE_PRETAX, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
    }
}
