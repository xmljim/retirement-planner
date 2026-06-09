/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Plan aggregate root and supporting domain types.
 *
 * <p>Owns: {@code Plan}, {@code Household}, {@code Person},
 * {@code Account}, {@code AccountSleeve}, {@code Bucket} sealed
 * interface and concrete types, {@code FundingPolicy},
 * {@code SpendingPolicy}, {@code LifecyclePolicy},
 * {@code RolloverEvent}, {@code BucketReallocationEvent},
 * {@code RothConversion}, {@code Assumptions},
 * {@code AssetAllocationPolicy}.
 *
 * <p>Bucket value types live here even though the bucket engine
 * lives in {@code bucket/}; per ADR-002 the engine operates on the
 * Plan's invariants but does not own them.
 *
 * <p>See ADR-002 (domain model), ADR-007 (Money / precision), and
 * ADR-008 (module boundaries).
 */
@ApplicationModule(displayName = "Plan")
package io.github.xmljim.retirement.retirementplanner.plan;

import org.springframework.modulith.ApplicationModule;
