/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * Salary timeline value types — {@code SalaryProfile} and its
 * supporting records ({@code SalaryOverride}, sealed
 * {@code BonusPolicy} with {@code FixedBonus} /
 * {@code PercentOfSalaryBonus}).
 *
 * <p>Exposed as a {@link org.springframework.modulith.NamedInterface}
 * so the contribution engine can read salary at a given month.
 */
@NamedInterface("salary")
package io.github.xmljim.retirement.retirementplanner.plan.salary;

import org.springframework.modulith.NamedInterface;
