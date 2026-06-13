/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.accumulation.internal;

import java.math.BigDecimal;
import java.time.YearMonth;

import org.springframework.stereotype.Component;

import io.github.xmljim.retirement.retirementplanner.accumulation.SleeveYieldEngine;
import io.github.xmljim.retirement.retirementplanner.plan.Assumptions;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountSleeve;
import io.github.xmljim.retirement.retirementplanner.plan.account.SleeveYieldPolicy;
import io.github.xmljim.retirement.retirementplanner.shared.CompoundRate;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Geometric monthly compounding implementation of
 * {@link SleeveYieldEngine}. The annual-to-monthly conversion lives in
 * {@link CompoundRate}; this class is the policy-routing layer that
 * picks the right annual rate for each {@link SleeveYieldPolicy} and
 * applies it to the sleeve's balance.
 */
@Component
class SleeveYieldEngineImpl implements SleeveYieldEngine {

    @Override
    public Money accruePerMonth(AccountSleeve sleeve, YearMonth period, Assumptions assumptions) {
        BigDecimal annualRate = annualRateFor(sleeve.yieldPolicy(), assumptions);
        if (sleeve.balance().amount().signum() == 0 || annualRate.signum() == 0) {
            return Money.ZERO_USD;
        }
        BigDecimal monthlyRate = CompoundRate.monthlyFromAnnual(annualRate);
        return sleeve.balance().times(monthlyRate);
    }

    private static BigDecimal annualRateFor(SleeveYieldPolicy policy, Assumptions assumptions) {
        return switch (policy) {
            case SleeveYieldPolicy.FixedRate fr -> fr.annualRate();
            case SleeveYieldPolicy.MoneyMarket mm -> mm.currentRate();
            case SleeveYieldPolicy.TracksAllocation _ -> assumptions.preRetirementReturnRate();
        };
    }
}
