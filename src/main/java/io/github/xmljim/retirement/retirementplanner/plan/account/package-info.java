/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Account aggregate within the Plan module: {@code Account},
 * {@code AccountSleeve}, {@code AccountType}, owner discriminators,
 * sleeve kind / yield policies. Persistence lives in {@code internal/}.
 *
 * <p>Exposed as a {@link org.springframework.modulith.NamedInterface}
 * so other modules ({@code api/}, {@code contribution/},
 * {@code simulation/}) can import these types — see ADR-008.
 */
@NamedInterface("account")
package io.github.xmljim.retirement.retirementplanner.plan.account;

import org.springframework.modulith.NamedInterface;
