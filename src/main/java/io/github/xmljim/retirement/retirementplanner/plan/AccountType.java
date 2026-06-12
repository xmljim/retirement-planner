/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

/**
 * Account category carried by an {@link Account} (ADR-002).
 *
 * <p>Tax treatment is data on the enum, not subclasses — keeps the JPA
 * mapping single-table and the engine free of {@code instanceof} chains.
 * The tax engine (ADR-004) keys on these values to select the correct
 * deferral and withdrawal rules. Stored as TEXT in Postgres with a
 * CHECK constraint matching the enum names.
 */
public enum AccountType {
    /** Traditional 401(k) — pre-tax contributions, ordinary-income on withdrawal. */
    TRADITIONAL_401K,
    /** Roth 401(k) — post-tax contributions, qualified withdrawals tax-free. */
    ROTH_401K,
    /** Traditional 403(b) — public-school / nonprofit analogue of Traditional 401(k). */
    TRADITIONAL_403B,
    /** Roth 403(b) — public-school / nonprofit analogue of Roth 401(k). */
    ROTH_403B,
    /** Traditional IRA — pre-tax (or deductible) contributions, ordinary income on withdrawal. */
    TRADITIONAL_IRA,
    /** Roth IRA — post-tax contributions, qualified withdrawals tax-free. */
    ROTH_IRA,
    /** Health Savings Account — triple-tax-advantaged for qualified medical expenses. */
    HSA,
    /** Taxable brokerage — capital gains on disposition, dividend / interest taxed annually. */
    TAXABLE_BROKERAGE,
    /** Plain cash / checking / savings — interest taxed annually. */
    CASH,
    /** Pension — income stream, not a balance. Modeled as Account with zero sleeves until paid out. */
    PENSION
}
