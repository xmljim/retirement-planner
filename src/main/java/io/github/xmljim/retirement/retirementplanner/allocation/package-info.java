/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Asset allocation: glide-path policy evaluation, asset class
 * definitions, weight interpolation across reference ages.
 *
 * <p>Public API: {@code AllocationPolicy} interface,
 * {@code AssetClass} enum, {@code GlidePathPoint} value record.
 *
 * <p>See ADR-005 (Monte Carlo &amp; returns) and ADR-008 (module
 * boundaries).
 */
@ApplicationModule(displayName = "Asset Allocation")
package io.github.xmljim.retirement.retirementplanner.allocation;

import org.springframework.modulith.ApplicationModule;
