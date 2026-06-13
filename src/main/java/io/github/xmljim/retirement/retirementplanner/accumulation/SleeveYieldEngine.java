/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.accumulation;

import java.time.YearMonth;

import io.github.xmljim.retirement.retirementplanner.plan.Assumptions;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountSleeve;
import io.github.xmljim.retirement.retirementplanner.plan.account.SleeveYieldPolicy;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Computes the per-month yield accrual for one {@link AccountSleeve}
 * (S-2.7, FR-2.2, FR-5.1).
 *
 * <p>Phase-agnostic: the engine applies yield given a sleeve's
 * {@link SleeveYieldPolicy} and the plan's {@link Assumptions}; it
 * does not know whether the household is accumulating, bridging, or
 * drawing down. The orchestrator (S-2.8) decides when to call this.
 *
 * <p>Compounding convention: monthly nominal rate is
 * {@code (1 + annual)^(1/12) - 1} (geometric). Sheet2's simple
 * {@code annual / 12} model is reproduced behind the S-2.10 fixture,
 * not in this engine.
 *
 * <p>Policy mapping:
 * <ul>
 *   <li>{@link SleeveYieldPolicy.FixedRate} &mdash; uses the policy's
 *       {@code annualRate} (e.g. a CD or stable-value fund).</li>
 *   <li>{@link SleeveYieldPolicy.MoneyMarket} &mdash; uses the policy's
 *       {@code currentRate} (a cash sweep / MMF held inside an IRA or
 *       401(k)).</li>
 *   <li>{@link SleeveYieldPolicy.TracksAllocation} &mdash; deterministic
 *       substitute for EPIC-5's stochastic returns. Reads
 *       {@code preRetirementReturnRate} from {@link Assumptions};
 *       EPIC-5 will swap in the per-month draw.</li>
 * </ul>
 *
 * <p>Modulith: hot-path consumer of {@code plan/} types (ADR-008).
 * Direct method-call dependency, not events.
 */
public interface SleeveYieldEngine {

    /**
     * Returns the yield accrual for one month on one sleeve.
     *
     * <p>Result is computed as {@code balance × monthlyRate} where
     * {@code monthlyRate = (1 + annual)^(1/12) - 1}. The accrual is
     * the dollar amount to add to the sleeve's balance for the month;
     * a zero balance or zero rate yields {@link Money#ZERO_USD}.
     *
     * @param sleeve      the sleeve to accrue yield on
     * @param period      the calendar month being accrued (today
     *                    unused by the deterministic policies, but
     *                    kept on the signature so EPIC-5's stochastic
     *                    successor doesn't need a contract change)
     * @param assumptions plan-wide deterministic assumptions; consulted
     *                    for {@link SleeveYieldPolicy.TracksAllocation}
     * @return the dollar accrual for the month
     */
    Money accruePerMonth(AccountSleeve sleeve, YearMonth period, Assumptions assumptions);
}
