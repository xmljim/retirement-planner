/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.contribution;

/**
 * Employee-side amount on a {@link ContributionPolicy} (ADR-003).
 *
 * <p>Sealed: an employee either contributes a percentage of their
 * prevailing salary ({@link PercentOfSalary}) or a fixed annual dollar
 * amount ({@link FixedDollar}). Annual is the fixed-dollar unit because
 * IRS limits ({@code §402(g)}, {@code §223}, IRA caps) are framed
 * annually; the contribution engine divides at compute time.
 */
public sealed interface ContributionAmount permits PercentOfSalary, FixedDollar {}
