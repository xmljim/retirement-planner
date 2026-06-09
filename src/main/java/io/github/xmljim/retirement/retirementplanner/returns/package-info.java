/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Historical returns dataset access: read-only Parquet datasets
 * bundled with the application, served as primitive {@code double}
 * arrays per ADR-005's dual-precision approach.
 *
 * <p>Public API: {@code ReturnsDataset} interface,
 * {@code BlockBootstrap} sampler.
 *
 * <p>This module is the only sanctioned location, alongside
 * {@code simulation.montecarlo.internal}, where {@code double}
 * primitives may appear (see ADR-007).
 *
 * <p>See ADR-005 (Monte Carlo &amp; returns), ADR-006 (BlobStore for
 * dataset distribution), and ADR-008 (module boundaries).
 */
@ApplicationModule(displayName = "Historical Returns")
package io.github.xmljim.retirement.retirementplanner.returns;

import org.springframework.modulith.ApplicationModule;
