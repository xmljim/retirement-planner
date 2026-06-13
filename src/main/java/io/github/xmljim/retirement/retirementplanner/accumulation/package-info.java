/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Accumulation engine: per-month yield application across an account's
 * sleeves and (later, S-2.8) the deterministic projection orchestrator.
 *
 * <p>Public API: {@code SleeveYieldEngine} interface and the value
 * types it consumes/produces. Compounding math lives in
 * {@code internal/}.
 *
 * <p>Consumes: {@code plan} (Account, AccountSleeve, SleeveYieldPolicy,
 * Assumptions). Produces: {@link io.github.xmljim.retirement.retirementplanner.shared.Money}
 * accruals consumed by the projection orchestrator.
 *
 * <p>Phase-agnostic: {@code SleeveYieldEngine} applies yield to a sleeve
 * regardless of accumulation vs. drawdown phase. The deterministic
 * substitute for EPIC-5's stochastic returns reads from
 * {@code Assumptions} until that epic lands.
 *
 * <p>See ADR-002 (domain model), ADR-005 (Monte Carlo &amp; returns),
 * and ADR-008 (module boundaries).
 */
@ApplicationModule(displayName = "Accumulation Engine")
package io.github.xmljim.retirement.retirementplanner.accumulation;

import org.springframework.modulith.ApplicationModule;
