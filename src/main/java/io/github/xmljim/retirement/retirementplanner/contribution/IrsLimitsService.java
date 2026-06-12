/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution;

/**
 * Provides {@link IrsLimits} for any year covered by the bundled
 * dataset or projectable from it (ADR-003).
 *
 * <p>Years with a published IRS notice in {@code irs-limits.yaml}
 * return {@link IrsLimits.Source#PUBLISHED} records. Years strictly
 * after the latest published year return {@link IrsLimits.Source#PROJECTED}
 * records computed by compounding the configured contribution-limit
 * growth rate on the latest published year. Years before the earliest
 * published year are not supported and throw.
 *
 * <p>Implementations log which years are projected vs published so
 * downstream simulations can flag projected assumptions in their run
 * report.
 */
public interface IrsLimitsService {

    /**
     * Returns the limits for {@code year}.
     *
     * @throws IllegalArgumentException if {@code year} is before the earliest published year in the dataset
     */
    IrsLimits forYear(int year);
}
