/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.account;

import java.util.List;
import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.plan.PlanId;

/**
 * Public repository surface for the {@link Account} aggregate (ADR-002).
 *
 * <p>All read methods are scoped to the active tenant resolved from
 * {@code TenantContext}, joined through the parent Plan; callers cannot
 * read across tenants. {@link #save(Account)} verifies the parent Plan
 * belongs to the active tenant and refuses to migrate an existing
 * Account to a different Plan.
 */
public interface AccountRepository {

    /** Persist (insert or update) an Account. The parent Plan must belong to the active tenant. */
    Account save(Account account);

    /** Find an Account by id within the active tenant. */
    Optional<Account> findById(AccountId id);

    /** All Accounts belonging to the given Plan, scoped to the active tenant. */
    List<Account> findByPlanId(PlanId planId);

    /** Delete an Account by id within the active tenant. No-op if absent or in another tenant. */
    void deleteById(AccountId id);
}
