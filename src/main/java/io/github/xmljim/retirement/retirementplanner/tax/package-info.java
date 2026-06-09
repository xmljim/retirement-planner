/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Tax engine: federal brackets, state tax with retirement-aware
 * subtractions, RMDs (IRS Uniform Lifetime Table), Roth conversions,
 * taxable Social Security via the IRS provisional-income formula.
 *
 * <p>Public API: {@code TaxEngine} interface, {@code TaxResult},
 * {@code TaxYearInputs} value records.
 *
 * <p>Per ADR-004 the engine is decision-grade, not filing-grade —
 * its job is to inform retirement decisions, not produce returns.
 *
 * <p>See ADR-004 (tax engine) and ADR-008 (module boundaries).
 */
@ApplicationModule(displayName = "Tax Engine")
package io.github.xmljim.retirement.retirementplanner.tax;

import org.springframework.modulith.ApplicationModule;
