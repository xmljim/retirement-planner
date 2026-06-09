/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Simulation orchestrator: month-by-month projection across
 * accumulation, bridge, and drawdown phases. Includes the Monte
 * Carlo engine.
 *
 * <p>Hot-path module: per ADR-008 simulation calls into other
 * modules via direct method calls on injected interfaces, never
 * via the event bus. The {@code ApplicationEventPublisher} is
 * banned here (enforced by ArchUnit).
 *
 * <p>The Monte Carlo inner loop (in
 * {@code simulation.montecarlo.internal}) is the bounded exception
 * to the {@code Money}-only contract — primitive {@code double} is
 * allowed there per ADR-005 and ADR-007 only.
 *
 * <p>Public API: {@code SimulationService},
 * {@code MonteCarloService}, {@code MonthlyProjection},
 * {@code SimulationResult}.
 *
 * <p>See ADR-005 (Monte Carlo), ADR-008 (module boundaries),
 * ADR-007 (precision), and CLAUDE.md (hot-path / cold-path rule).
 */
@ApplicationModule(displayName = "Simulation Orchestrator")
package io.github.xmljim.retirement.retirementplanner.simulation;

import org.springframework.modulith.ApplicationModule;
