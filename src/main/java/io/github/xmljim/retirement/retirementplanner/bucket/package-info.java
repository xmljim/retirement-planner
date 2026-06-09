/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Bucket engine: priority-ordered draws, adaptive spending policy
 * evaluation, lifecycle sweeps, mid-plan reallocation.
 *
 * <p>The {@code Bucket}, {@code FundingPolicy}, {@code SpendingPolicy},
 * and {@code LifecyclePolicy} sealed interfaces themselves live in
 * {@code plan/} (they are part of the Plan aggregate's invariants).
 * This module owns the engine code that consumes them.
 *
 * <p>Public API: {@code BucketEngine} interface plus
 * {@code SpendingDecision} / {@code BucketState} value records.
 *
 * <p>See ADR-002 (bucket abstraction) and ADR-008 (module boundaries).
 */
@ApplicationModule(displayName = "Bucket Engine")
package io.github.xmljim.retirement.retirementplanner.bucket;

import org.springframework.modulith.ApplicationModule;
