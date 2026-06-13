/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.contribution;

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
 *
 * <p>{@link #asRoth()} carries the SECURE 2.0 §604 election (S-2.6): when
 * true, matched contributions are emitted as
 * {@link io.github.xmljim.retirement.retirementplanner.shared.CashFlowKind#EMPLOYER_MATCH_ROTH}
 * rather than {@code EMPLOYER_MATCH}, and the tax engine (ADR-004 /
 * EPIC-3) treats the match as a current-year addition to W-2 wages.
 * Defaults to {@code false} (the historical pre-tax behavior); use
 * {@link #of(List)} when no §604 election is in effect. §604 is
 * orthogonal to §603 high-earner Roth catch-up routing.
 */
public record EmployerMatch(List<MatchTier> tiers, boolean asRoth) {

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

    /** Convenience constructor for the default (pre-tax) match — no §604 election. */
    public static EmployerMatch of(List<MatchTier> tiers) {
        return new EmployerMatch(tiers, false);
    }

    /** Convenience constructor for a §604-elected (Roth-treated) match. */
    public static EmployerMatch ofRoth(List<MatchTier> tiers) {
        return new EmployerMatch(tiers, true);
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
