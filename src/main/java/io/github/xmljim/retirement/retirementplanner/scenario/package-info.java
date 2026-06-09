/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Scenario management: save, clone, compare; Parquet snapshots for
 * immutable history; run result caching by canonicalized inputs hash.
 *
 * <p>This module hosts the project's only Modulith {@code @ApplicationModuleListener}
 * use sites — cold-path workflow events: {@code ScenarioSavedEvent},
 * {@code RunCompletedEvent}, {@code ScenarioDeletedEvent}.
 *
 * <p>Public API: {@code ScenarioService}, {@code RunRepository},
 * scenario / run DTOs.
 *
 * <p>See ADR-006 (scenario persistence &amp; versioning) and ADR-008
 * (module boundaries; cold-path event usage).
 */
@ApplicationModule(displayName = "Scenario Management")
package io.github.xmljim.retirement.retirementplanner.scenario;

import org.springframework.modulith.ApplicationModule;
