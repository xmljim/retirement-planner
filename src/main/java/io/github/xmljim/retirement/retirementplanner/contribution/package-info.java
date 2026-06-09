/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Accumulation engine: monthly contributions, IRS limit hierarchy,
 * employer match, SECURE 2.0 §603 / §604 routing.
 *
 * <p>Public API: {@code ContributionEngine} interface and the value
 * records it returns. Internal limit-loading and matching logic lives
 * in {@code internal/}.
 *
 * <p>Consumes: {@code plan} (Account, Person, ContributionPolicy).
 * Produces: {@code CashFlow} sequences consumed by the simulation
 * orchestrator.
 *
 * <p>See ADR-003 (accumulation phase &amp; contribution model) and
 * ADR-008 (module boundaries).
 */
@ApplicationModule(displayName = "Contribution Engine")
package io.github.xmljim.retirement.retirementplanner.contribution;

import org.springframework.modulith.ApplicationModule;
