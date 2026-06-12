/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

class ContributionPolicyTest {

    private static final BigDecimal FIVE_PCT = new BigDecimal("0.05");
    private static final BigDecimal SIX_PCT = new BigDecimal("0.06");
    private static final BigDecimal HUNDRED_PCT = new BigDecimal("1.00");

    @Test
    @DisplayName("PercentOfSalary rejects negative pct")
    void percentRejectsNegative() {
        assertThatThrownBy(() -> new PercentOfSalary(new BigDecimal("-0.01")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("FixedDollar rejects negative annual amount")
    void fixedRejectsNegative() {
        assertThatThrownBy(() -> new FixedDollar(Money.usd("-100.00"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("EscalationPolicy rejects negative annualIncrease or cap")
    void escalationRejectsNegative() {
        assertThatThrownBy(() -> new EscalationPolicy(new BigDecimal("-0.01"), new BigDecimal("0.15")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EscalationPolicy(new BigDecimal("0.01"), new BigDecimal("-0.15")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ContributionPolicy.of returns a policy with no escalation, match, or dates")
    void ofConvenience() {
        ContributionPolicy policy = ContributionPolicy.of(new PercentOfSalary(FIVE_PCT));
        assertThat(policy.employee()).isInstanceOf(PercentOfSalary.class);
        assertThat(policy.escalation()).isEmpty();
        assertThat(policy.match()).isEmpty();
        assertThat(policy.startDate()).isEmpty();
        assertThat(policy.endDate()).isEmpty();
    }

    @Test
    @DisplayName("ContributionPolicy rejects endDate before startDate")
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> new ContributionPolicy(
                        new PercentOfSalary(FIVE_PCT),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(LocalDate.of(2026, 6, 1)),
                        Optional.of(LocalDate.of(2026, 1, 1))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("matchAllowedFor: 401(k) and 403(b) variants only")
    void matchAllowedFor() {
        assertThat(ContributionPolicy.matchAllowedFor(AccountType.TRADITIONAL_401K))
                .isTrue();
        assertThat(ContributionPolicy.matchAllowedFor(AccountType.ROTH_401K)).isTrue();
        assertThat(ContributionPolicy.matchAllowedFor(AccountType.TRADITIONAL_403B))
                .isTrue();
        assertThat(ContributionPolicy.matchAllowedFor(AccountType.ROTH_403B)).isTrue();

        assertThat(ContributionPolicy.matchAllowedFor(AccountType.TRADITIONAL_IRA))
                .isFalse();
        assertThat(ContributionPolicy.matchAllowedFor(AccountType.ROTH_IRA)).isFalse();
        assertThat(ContributionPolicy.matchAllowedFor(AccountType.HSA)).isFalse();
        assertThat(ContributionPolicy.matchAllowedFor(AccountType.TAXABLE_BROKERAGE))
                .isFalse();
        assertThat(ContributionPolicy.matchAllowedFor(AccountType.CASH)).isFalse();
        assertThat(ContributionPolicy.matchAllowedFor(AccountType.PENSION)).isFalse();
    }

    @Test
    @DisplayName("Account with employer match on a non-401(k)/403(b) is rejected")
    void accountRejectsMatchOnIra() {
        ContributionPolicy withMatch = new ContributionPolicy(
                new PercentOfSalary(FIVE_PCT),
                Optional.empty(),
                Optional.of(new EmployerMatch(List.of(new MatchTier(SIX_PCT, HUNDRED_PCT)))),
                Optional.empty(),
                Optional.empty());
        AccountSleeve sleeve = AccountSleeve.of(
                new SleeveKind.AssetAllocation(), Money.usd("10000.00"), new SleeveYieldPolicy.TracksAllocation());
        assertThatThrownBy(() -> Account.of(
                        new PlanId(1L), AccountType.TRADITIONAL_IRA, new OwnerRef.Joint(), List.of(sleeve), withMatch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EmployerMatch");
    }

    @Test
    @DisplayName("Account with employer match on a 401(k) is accepted")
    void accountAcceptsMatchOn401k() {
        ContributionPolicy withMatch = new ContributionPolicy(
                new PercentOfSalary(FIVE_PCT),
                Optional.empty(),
                Optional.of(new EmployerMatch(List.of(new MatchTier(SIX_PCT, HUNDRED_PCT)))),
                Optional.empty(),
                Optional.empty());
        AccountSleeve sleeve = AccountSleeve.of(
                new SleeveKind.AssetAllocation(), Money.usd("10000.00"), new SleeveYieldPolicy.TracksAllocation());
        Account account = Account.of(
                new PlanId(1L), AccountType.TRADITIONAL_401K, new OwnerRef.Joint(), List.of(sleeve), withMatch);
        assertThat(account.contributionPolicy()).isPresent();
    }

    @Test
    @DisplayName("Account without contribution policy still works (Optional.empty)")
    void accountWithoutPolicy() {
        Account account = Account.withDefaultSleeve(
                new PlanId(1L), AccountType.ROTH_IRA, new OwnerRef.Joint(), Money.usd("1000.00"));
        assertThat(account.contributionPolicy()).isEmpty();
    }
}
