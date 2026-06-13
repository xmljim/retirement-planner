/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution;

import java.time.Month;
import java.util.Objects;

/**
 * Structured warning emitted by the {@link ContributionEngine} when the
 * user's contribution policy could not be honored verbatim — for example,
 * a high-earner whose plan has no Roth designated component to receive
 * the §603-mandated catch-up portion.
 *
 * <p>Warnings are a first-class engine output, not log lines: they are
 * returned in {@link MonthlyContributionResult} alongside cash flows so
 * the API and frontend can surface them to the user (ADR-003, "Engine
 * Output Contract").
 *
 * @param kind      stable enum identifying the rule that fired
 * @param accountId account whose contribution stream was affected
 * @param year      contribution year
 * @param month     contribution month
 * @param detail    human-readable detail with specifics (amount disallowed, etc.)
 */
public record EngineWarning(WarningKind kind, long accountId, int year, Month month, String detail) {

    public EngineWarning {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(month, "month");
        Objects.requireNonNull(detail, "detail");
    }

    /**
     * Stable identifier for an {@link EngineWarning}. Adding new values
     * is backward-compatible; renaming is a breaking change. Frontend
     * i18n keys map to enum names.
     */
    public enum WarningKind {
        /**
         * SECURE 2.0 §603 high-earner catch-up could not be routed to a
         * Roth designated account because none exists for this employer
         * plan; the catch-up portion was disallowed.
         */
        SECTION_603_NO_ROTH_DESTINATION
    }
}
