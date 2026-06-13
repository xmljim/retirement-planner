/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution;

import java.util.List;
import java.util.Objects;

import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;

/**
 * Return value of
 * {@link ContributionEngine#contributeForMonth}: the cash flows
 * generated this month plus any structured warnings raised while
 * computing them (ADR-003, "Engine Output Contract").
 *
 * <p>Warnings exist because §603 / §604 routing can silently truncate a
 * user's intended contribution (e.g. a high-earner whose plan has no
 * Roth designated account cannot receive the §603-mandated catch-up).
 * Surfacing those decisions back to the API and frontend is part of the
 * engine's contract; downstream UI must aggregate flows by account/month
 * and display warnings adjacent to the contribution display.
 */
public record MonthlyContributionResult(List<CashFlow> flows, List<EngineWarning> warnings) {

    public MonthlyContributionResult {
        Objects.requireNonNull(flows, "flows");
        Objects.requireNonNull(warnings, "warnings");
        flows = List.copyOf(flows);
        warnings = List.copyOf(warnings);
    }

    /** A result with the supplied flows and no warnings. */
    public static MonthlyContributionResult ofFlows(List<CashFlow> flows) {
        return new MonthlyContributionResult(flows, List.of());
    }
}
