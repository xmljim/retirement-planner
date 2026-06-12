/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Household value types — {@code Household}, {@code HouseholdId},
 * {@code FilingStatus}. Persistence lives in {@code internal/}.
 *
 * <p>Exposed as a {@link org.springframework.modulith.NamedInterface}
 * so other modules can reference filing status and household-scoped
 * scalars from outside {@code plan/}.
 */
@NamedInterface("household")
package io.github.xmljim.retirement.retirementplanner.plan.household;

import org.springframework.modulith.NamedInterface;
