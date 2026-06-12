/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.household;

/**
 * Federal tax filing status carried by a {@code Household}.
 *
 * <p>Stored as TEXT in Postgres with a CHECK constraint matching the
 * names below (see {@code V2__plan_household_person.sql}). The Java
 * surface is a plain enum; downstream tax-engine code (ADR-004) keys
 * on these values to select bracket tables.
 */
public enum FilingStatus {
    SINGLE,
    MARRIED_FILING_JOINTLY,
    MARRIED_FILING_SEPARATELY,
    HEAD_OF_HOUSEHOLD,
    QUALIFYING_SURVIVING_SPOUSE
}
