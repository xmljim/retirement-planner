/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Cross-cutting value types used by multiple modules.
 *
 * <p>Currently reserved for: {@code Money}, {@code CashFlow},
 * {@code BlobStore} interface, related primitives that don't naturally
 * belong to any single domain module.
 *
 * <p>The bar for adding a type here is high: it must be needed by
 * three or more modules and have no domain-meaningful home elsewhere.
 * When in doubt, place it in the consuming module.
 *
 * <p>See ADR-007 (Money), ADR-006 (BlobStore), ADR-008 (module
 * boundaries; rationale for {@code shared}).
 */
@ApplicationModule(displayName = "Shared Value Types")
package io.github.xmljim.retirement.retirementplanner.shared;

import org.springframework.modulith.ApplicationModule;
