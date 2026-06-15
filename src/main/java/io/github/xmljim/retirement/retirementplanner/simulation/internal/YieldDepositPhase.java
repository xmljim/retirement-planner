/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation.internal;

import java.time.YearMonth;
import java.util.Map;

import io.github.xmljim.retirement.retirementplanner.accumulation.SleeveYieldEngine;
import io.github.xmljim.retirement.retirementplanner.plan.Assumptions;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Per-month yield + deposit phase: applies start-of-month yield to
 * each sleeve, then distributes any contributions from the
 * {@link ContributionPhase} pro-rata via {@link ContributionDistributor}.
 *
 * <p>Yield-on-contribution timing: start-of-month balance receives
 * yield, contributions land afterward. The convention is documented
 * on {@code AccumulationProjector}; this class is the deterministic
 * implementation.
 */
final class YieldDepositPhase {

    private final SleeveYieldEngine engine;

    YieldDepositPhase(SleeveYieldEngine engine) {
        this.engine = engine;
    }

    void apply(
            Assumptions assumptions,
            Map<AccountId, MutableAccount> state,
            Map<AccountId, Money> contributionsByAccount,
            YearMonth period) {
        state.forEach(
                (accountId, mutable) -> applyOne(assumptions, mutable, contributionsByAccount, accountId, period));
    }

    private void applyOne(
            Assumptions assumptions,
            MutableAccount mutable,
            Map<AccountId, Money> contributionsByAccount,
            AccountId accountId,
            YearMonth period) {
        Money totalStart = mutable.endingBalance();
        mutable.sleeves().forEach(sleeve -> sleeve.add(engine.accruePerMonth(sleeve.toRecord(), period, assumptions)));
        Money contribution = contributionsByAccount.getOrDefault(accountId, Money.ZERO_USD);
        if (contribution.amount().signum() > 0) {
            ContributionDistributor.distribute(mutable.sleeves(), totalStart, contribution);
        }
    }
}
