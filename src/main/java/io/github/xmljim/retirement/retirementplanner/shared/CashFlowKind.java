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
    EMPLOYER_MATCH_ROTH,
    /**
     * Taxable portion of a Roth conversion (Traditional → Roth). The
     * amount is added to ordinary income for the conversion year.
     * Reserved for EPIC-3 (tax engine).
     */
    ROTH_CONVERSION_TAXABLE,
    /**
     * Withdrawal taxed at ordinary income rates — pre-tax 401(k)/403(b),
     * Traditional IRA, RMDs, and the taxable portion of Social Security.
     * Reserved for EPIC-4 / EPIC-5 (drawdown).
     */
    WITHDRAWAL_ORDINARY,
    /**
     * Qualified-distribution withdrawal — Roth principal/earnings post
     * 5-year rule, qualified HSA medical, etc. Tax-free at withdrawal.
     * Reserved for EPIC-4 / EPIC-5 (drawdown).
     */
    WITHDRAWAL_QUALIFIED,
    /**
     * Cash drawn from a goal-oriented {@code Bucket} (Bridge, Travel,
     * Bucket-list, Healthcare, Legacy). The bucket engine emits these
     * as it spends down its earmarked balance; tax treatment follows
     * the source account's withdrawal kind. Reserved for EPIC-4 (bucket
     * engine).
     */
    BUCKET_DRAW
}
