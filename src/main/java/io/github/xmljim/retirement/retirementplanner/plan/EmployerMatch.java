/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Tiered employer-match formula on a {@link ContributionPolicy}
 * (ADR-003). Tiers are stored in ascending order of
 * {@link MatchTier#employeeContribPctUpTo()}; the canonical constructor
 * enforces that ordering.
 *
 * <p>The match is &ldquo;against the post-cap employee contribution&rdquo;
 * per ADR-003 §99 — the contribution engine applies §402(g) first and
 * then asks this match for the matched percentage. The §415(c) total-DC
 * cap (applied to employee + employer combined) is the engine's job,
 * not this value type's.
 */
public record EmployerMatch(List<MatchTier> tiers) {

    public EmployerMatch {
        Objects.requireNonNull(tiers, "tiers");
        if (tiers.isEmpty()) {
            throw new IllegalArgumentException("EmployerMatch requires at least one tier");
        }
        List<MatchTier> snapshot = List.copyOf(tiers);
        boolean ascending = IntStream.range(1, snapshot.size())
                .allMatch(i -> snapshot.get(i)
                                .employeeContribPctUpTo()
                                .compareTo(snapshot.get(i - 1).employeeContribPctUpTo())
                        > 0);
        if (!ascending) {
            throw new IllegalArgumentException("tiers must have strictly ascending employeeContribPctUpTo: "
                    + snapshot.stream().map(MatchTier::employeeContribPctUpTo).toList());
        }
        tiers = snapshot;
    }

    /**
     * Returns the employer-match rate (a fraction of salary) earned by
     * an employee contributing {@code employeePct} of salary. The result
     * is the sum across tiers of
     * {@code min(employeePct, tierUpTo) - prevTierUpTo)} times the
     * tier's {@code matchPct}, clamped to the highest tier.
     *
     * <p>For the standard &ldquo;100 % of 3 %, 50 % of next 2 %&rdquo;
     * formula, an employee contributing 6 % earns
     * {@code 0.03 × 1.00 + 0.02 × 0.50 = 0.04} (4 % of salary).
     */
    public BigDecimal matchPct(BigDecimal employeePct) {
        Objects.requireNonNull(employeePct, "employeePct");
        if (employeePct.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return IntStream.range(0, tiers.size())
                .mapToObj(i -> bandContribution(i, employeePct))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal bandContribution(int index, BigDecimal employeePct) {
        MatchTier tier = tiers.get(index);
        BigDecimal lowerBound =
                index == 0 ? BigDecimal.ZERO : tiers.get(index - 1).employeeContribPctUpTo();
        BigDecimal tierTop = tier.employeeContribPctUpTo().min(employeePct);
        BigDecimal band = tierTop.subtract(lowerBound);
        return band.signum() <= 0 ? BigDecimal.ZERO : band.multiply(tier.matchPct());
    }
}
