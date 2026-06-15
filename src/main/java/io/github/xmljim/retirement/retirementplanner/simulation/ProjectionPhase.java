/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation;

/**
 * Phase of life that a {@link MonthlyProjection} month falls in
 * (PRD-001, ADR-002). Bridge and drawdown months land in later epics;
 * S-2.8 only emits {@link #ACCUMULATION}.
 */
public enum ProjectionPhase {
    /** Pre-retirement: contributions accrue and balances grow with yield. */
    ACCUMULATION,

    /** Retired but pre-Social Security: bridge bucket draws cover spending. */
    BRIDGE,

    /** Post-Social Security: full drawdown including RMDs. */
    DRAWDOWN
}
