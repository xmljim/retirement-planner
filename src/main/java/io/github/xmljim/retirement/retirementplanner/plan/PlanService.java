/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.util.List;

/**
 * Application-level facade for the {@link Plan} aggregate. Controllers
 * delegate here rather than to the repository directly, so future
 * cross-aggregate orchestration (e.g. publishing a {@code PlanCreated}
 * cold-path event in S-6.x) has a place to land.
 *
 * <p>{@link #findById} and {@link #replace} throw
 * {@link io.github.xmljim.retirement.retirementplanner.shared.NotFoundException}
 * when no Plan with the given id is visible in the active tenant.
 */
public interface PlanService {

    /** Create a new Plan in the active tenant. */
    Plan create(Plan plan);

    /** Find a Plan by id, or throw {@code NotFoundException}. */
    Plan findById(PlanId id);

    /** All Plans visible to the active tenant. */
    List<Plan> findAll();

    /**
     * Replace the household scalars of an existing Plan. The 1–2
     * persons list is managed via {@link PersonService}; passing
     * different persons here is ignored.
     */
    Plan replace(PlanId id, Plan replacement);

    /** Delete a Plan by id. No-op if absent. */
    void deleteById(PlanId id);
}
