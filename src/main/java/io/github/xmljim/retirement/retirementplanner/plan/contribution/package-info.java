/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Contribution-policy value types attached to an {@code Account}: the
 * sealed {@code ContributionAmount} ({@code PercentOfSalary} |
 * {@code FixedDollar}), {@code EscalationPolicy}, {@code MatchTier} +
 * {@code EmployerMatch}, and the composing {@code ContributionPolicy}.
 *
 * <p>Distinct from the top-level {@code contribution/} module — that's
 * the engine; this is the value-types it consumes (ADR-002 §domain,
 * ADR-003 §policy shape).
 *
 * <p>Exposed as a {@link org.springframework.modulith.NamedInterface}
 * so the contribution engine and the REST DTOs can import these.
 */
@NamedInterface("contribution")
package io.github.xmljim.retirement.retirementplanner.plan.contribution;

import org.springframework.modulith.NamedInterface;
