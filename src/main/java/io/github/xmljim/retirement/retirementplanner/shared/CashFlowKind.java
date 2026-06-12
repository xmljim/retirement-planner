/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

/**
 * Discriminator for {@link CashFlow} entries (ADR-003 §"Engine
 * Behavior", point 7 — &ldquo;separate lines for employee, employer
 * match, and any after-tax — useful for downstream tax basis
 * tracking&rdquo;).
 *
 * <p>The contribution engine emits flows tagged with these kinds; the
 * tax engine and downstream reporting key off them. Slot reservations
 * cover SECURE 2.0 §603 / §604 routing (S-2.5 / S-2.6) so adding
 * Roth-catch-up and Roth-match behaviors does not require widening
 * this enum.
 */
public enum CashFlowKind {
    /** Employee elective deferral to a Traditional 401(k) / 403(b). Pre-tax. */
    EMPLOYEE_PRETAX,
    /** Employee elective deferral to a Roth 401(k) / 403(b) / Roth IRA. Post-tax. */
    EMPLOYEE_ROTH,
    /** Employee contribution to a Traditional IRA. */
    EMPLOYEE_TRADITIONAL_IRA,
    /** Employee contribution to a Health Savings Account (§223). */
    EMPLOYEE_HSA,
    /** Employee contribution from after-tax cash (taxable brokerage / cash). */
    EMPLOYEE_AFTER_TAX,
    /** Employer match — pre-tax (default; matches historical behavior). */
    EMPLOYER_MATCH,
    /**
     * Employee elective-deferral catch-up routed to Roth under SECURE 2.0
     * §603 (high-earner mandatory Roth catch-up). Reserved for S-2.5.
     */
    EMPLOYEE_ROTH_CATCHUP,
    /**
     * Employer match routed to Roth under SECURE 2.0 §604 (employee
     * elects Roth treatment of match). Reserved for S-2.6.
     */
    EMPLOYER_MATCH_ROTH
}
