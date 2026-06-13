/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation.internal;

import java.time.YearMonth;
import java.util.Comparator;

import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;

/**
 * Resolves the projection horizon for a {@link Plan} — the latest
 * person retirement date, expressed as a {@link YearMonth}. The
 * household horizon is always {@code max(person.retirementDate)} so
 * spouses retiring on different schedules each end contributing on
 * their own date while yield continues for both up to the household
 * horizon.
 */
final class ProjectionHorizon {

    private ProjectionHorizon() {}

    static YearMonth of(Plan plan) {
        return plan.persons().stream()
                .map(Person::retirementDate)
                .max(Comparator.naturalOrder())
                .map(d -> YearMonth.of(d.getYear(), d.getMonth()))
                .orElseThrow();
    }
}
