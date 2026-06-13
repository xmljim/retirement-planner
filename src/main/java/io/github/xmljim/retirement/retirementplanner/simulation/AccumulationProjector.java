/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfile;

/**
 * Composes {@code ContributionEngine} + {@code SleeveYieldEngine} to
 * produce a month-by-month deterministic accumulation projection for
 * a {@link Plan} (S-2.8, FR-7.1, FR-7.2, NFR-10).
 *
 * <p>Per ADR-008 this is a hot-path orchestrator: every month spans
 * direct synchronous calls into {@code contribution/} and
 * {@code accumulation/}.
 *
 * <h2>Phase</h2>
 * S-2.8 emits {@link ProjectionPhase#ACCUMULATION} only. Bridge and
 * drawdown phases land in EPIC-4 / EPIC-5; the
 * {@link MonthlyProjection#phase()} field is on the contract today so
 * a future projector can extend the same record without breaking
 * consumers.
 *
 * <h2>Horizon</h2>
 * The projection runs from {@code startMonth} (inclusive) through the
 * latest {@code retirementDate} across {@link Plan#persons()} —
 * spouses retiring on different schedules each see contribution flows
 * end at their own date, while yield continues for both up to the
 * household horizon.
 *
 * <h2>Yield-on-contribution timing</h2>
 * For each month: yield is applied to the start-of-month balance,
 * then contributions are added. Contributions therefore do not earn
 * intra-month yield. This is the simplest deterministic convention
 * and stays within the ≤1% Sheet2 tolerance (Sheet2 itself uses an
 * annual single-rate compound). EPIC-5's stochastic successor may
 * adopt a different convention.
 */
public interface AccumulationProjector {

    /**
     * Projects month-by-month from {@code startMonth} to the latest
     * person retirement date in the plan.
     *
     * @param plan            plan with persons + assumptions
     * @param accounts        the plan's accounts (caller loads — the
     *                        projector does not consult repositories)
     * @param salaryProfiles  per-person salary profile, keyed by
     *                        persisted {@link PersonId}; persons
     *                        without a profile generate no
     *                        contributions but still see yield on
     *                        existing balances
     * @param startMonth      first month of the projection (inclusive)
     * @return one {@link MonthlyProjection} per month from
     *         {@code startMonth} through the household retirement horizon
     */
    List<MonthlyProjection> project(
            Plan plan, List<Account> accounts, Map<PersonId, SalaryProfile> salaryProfiles, YearMonth startMonth);
}
