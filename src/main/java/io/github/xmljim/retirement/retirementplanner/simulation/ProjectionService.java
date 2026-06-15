/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation;

import java.util.List;

import io.github.xmljim.retirement.retirementplanner.plan.PlanId;

/**
 * Application service for deterministic projections (S-2.8). Loads the
 * {@code Plan} and its {@code Account}s from the active tenant, derives
 * a default per-person {@code SalaryProfile} (zero salary, zero growth)
 * since salary persistence isn't yet wired (S-2.1 defined the value
 * type without persistence), and runs the
 * {@link AccumulationProjector}.
 *
 * <p>The default zero-salary profile means the REST endpoint exercises
 * the yield-only path. Rich salary fixtures are exercised in projector
 * unit tests.
 */
public interface ProjectionService {

    /**
     * Projects month-by-month from this calendar month through the
     * household retirement horizon.
     *
     * @param planId the plan to project, scoped to the active tenant
     * @return one {@link MonthlyProjection} per month
     * @throws io.github.xmljim.retirement.retirementplanner.shared.NotFoundException
     *         if the plan is not visible to the active tenant
     */
    List<MonthlyProjection> deterministic(PlanId planId);
}
