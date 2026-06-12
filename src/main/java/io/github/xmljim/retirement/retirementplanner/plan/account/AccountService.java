/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.util.List;

/**
 * Application-level facade for the {@link Account} aggregate. Throws
 * {@link io.github.xmljim.retirement.retirementplanner.shared.NotFoundException}
 * when an Account is not visible in the active tenant.
 */
public interface AccountService {

    /** Create a new Account under the given Plan. */
    Account create(PlanId planId, Account account);

    /** Find an Account by id, or throw {@code NotFoundException}. */
    Account findById(AccountId id);

    /** All Accounts belonging to the given Plan. */
    List<Account> findByPlanId(PlanId planId);

    /** All Sleeves on the given Account. */
    List<AccountSleeve> findSleevesByAccountId(AccountId id);

    /** Replace an existing Account in place (sleeves replaced wholesale). */
    Account replace(AccountId id, Account replacement);

    /** Delete an Account by id. No-op if absent. */
    void deleteById(AccountId id);
}
