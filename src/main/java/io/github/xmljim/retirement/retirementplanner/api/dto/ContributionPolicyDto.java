/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.plan.contribution.ContributionPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.EmployerMatch;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.EscalationPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.MatchTier;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * REST shape of a {@link ContributionPolicy}. Optional sub-fields are
 * carried as nullable JSON properties; the conversion to/from the
 * domain wraps them in {@link java.util.Optional}.
 *
 * <p>{@link EscalationPolicy} and {@link EmployerMatch} are plain
 * records (no discriminator); {@link ContributionAmountDto} is the
 * tagged union for the sealed {@code ContributionAmount}.
 */
public record ContributionPolicyDto(
        @NotNull @Valid ContributionAmountDto employee,
        @Valid EscalationPolicyDto escalation,
        @Valid EmployerMatchDto match,
        LocalDate startDate,
        LocalDate endDate) {

    public static ContributionPolicyDto from(ContributionPolicy policy) {
        return new ContributionPolicyDto(
                ContributionAmountDto.from(policy.employee()),
                policy.escalation().map(EscalationPolicyDto::from).orElse(null),
                policy.match().map(EmployerMatchDto::from).orElse(null),
                policy.startDate().orElse(null),
                policy.endDate().orElse(null));
    }

    public ContributionPolicy toContributionPolicy() {
        return new ContributionPolicy(
                employee.toContributionAmount(),
                Optional.ofNullable(escalation).map(EscalationPolicyDto::toEscalationPolicy),
                Optional.ofNullable(match).map(EmployerMatchDto::toEmployerMatch),
                Optional.ofNullable(startDate),
                Optional.ofNullable(endDate));
    }

    public record EscalationPolicyDto(
            @NotNull BigDecimal annualIncrease, @NotNull BigDecimal cap) {

        public static EscalationPolicyDto from(EscalationPolicy policy) {
            return new EscalationPolicyDto(policy.annualIncrease(), policy.cap());
        }

        public EscalationPolicy toEscalationPolicy() {
            return new EscalationPolicy(annualIncrease, cap);
        }
    }

    public record EmployerMatchDto(@NotNull @Valid List<MatchTierDto> tiers) {

        public static EmployerMatchDto from(EmployerMatch match) {
            return new EmployerMatchDto(
                    match.tiers().stream().map(MatchTierDto::from).toList());
        }

        public EmployerMatch toEmployerMatch() {
            return new EmployerMatch(
                    tiers.stream().map(MatchTierDto::toMatchTier).toList());
        }
    }

    public record MatchTierDto(
            @NotNull BigDecimal employeeContribPctUpTo,
            @NotNull BigDecimal matchPct) {

        public static MatchTierDto from(MatchTier tier) {
            return new MatchTierDto(tier.employeeContribPctUpTo(), tier.matchPct());
        }

        public MatchTier toMatchTier() {
            return new MatchTier(employeeContribPctUpTo, matchPct);
        }
    }
}
