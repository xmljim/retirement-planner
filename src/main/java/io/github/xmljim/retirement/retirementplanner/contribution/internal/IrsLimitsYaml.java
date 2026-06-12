/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import java.math.BigDecimal;
import java.util.List;

/**
 * Jackson-deserializable mirror of {@code resources/data/irs-limits.yaml}.
 *
 * <p>Distinct from the public {@code IrsLimits} record so the public
 * domain type stays free of Jackson annotations and integer-USD parsing
 * quirks. This class is internal to the contribution module.
 */
record IrsLimitsYaml(Projection projection, List<YearLimits> years) {

    record Projection(BigDecimal contributionLimitGrowthRate) {}

    record YearLimits(
            int year,
            BigDecimal employee401kBase,
            BigDecimal employee401k50PlusCatchup,
            BigDecimal employee401k60PlusCatchup,
            BigDecimal iraBase,
            BigDecimal ira50PlusCatchup,
            BigDecimal hsaSelfOnly,
            BigDecimal hsaFamily,
            BigDecimal hsa55PlusCatchup,
            BigDecimal totalDc,
            BigDecimal secure2_0_603HighEarnerThreshold) {}
}
