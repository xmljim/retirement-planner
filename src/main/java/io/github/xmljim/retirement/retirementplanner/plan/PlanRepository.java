/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.util.List;
import java.util.Optional;

/**
 * Public repository surface for the {@link Plan} aggregate (ADR-002).
 *
 * <p>All read methods are scoped to the active tenant resolved from
 * {@code TenantContext}; callers cannot accidentally read across
 * tenants. {@link #save(Plan)} stamps the tenant id on creation and
 * refuses to migrate an existing Plan to a different tenant.
 */
public interface PlanRepository {

    /** Persist (insert or update) a Plan in the active tenant. */
    Plan save(Plan plan);

    /** Find a Plan by id within the active tenant. */
    Optional<Plan> findById(PlanId id);

    /** All Plans visible to the active tenant. */
    List<Plan> findAll();

    /** Delete a Plan by id within the active tenant. No-op if absent. */
    void deleteById(PlanId id);
}
