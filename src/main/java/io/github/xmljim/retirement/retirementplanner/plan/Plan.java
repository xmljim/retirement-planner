/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.plan.household.Household;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;

/**
 * Top-level aggregate root for retirement planning (ADR-002).
 *
 * <p>Owns one {@link Household} and one or two {@link Person}s. Future
 * fields (Accounts, Buckets, AssetAllocationPolicy) land in subsequent
 * stories.
 *
 * <p>{@link Assumptions} is required from S-2.8 onward — the
 * deterministic accumulation projector reads pre-retirement return
 * rate and cash interest rate from here. Scenario-level overrides
 * (EPIC-6) layer on top without re-typing per request.
 *
 * <p>{@code tenantId} carries multi-tenancy from day one — solo mode
 * uses the {@code "solo"} tenant seeded in {@code V1__init.sql}.
 * {@code id} is absent before persistence; the repository populates it
 * on save.
 */
public record Plan(
        Optional<PlanId> id, long tenantId, Household household, List<Person> persons, Assumptions assumptions) {

    public Plan {
        Objects.requireNonNull(id, "id");
        if (household == null) {
            throw new IllegalArgumentException("household is required");
        }
        if (persons == null || persons.isEmpty() || persons.size() > 2) {
            throw new IllegalArgumentException("persons must contain 1 or 2 entries");
        }
        Objects.requireNonNull(assumptions, "assumptions");
        persons = List.copyOf(persons);
    }

    public static Plan of(long tenantId, Household household, List<Person> persons, Assumptions assumptions) {
        return new Plan(Optional.empty(), tenantId, household, persons, assumptions);
    }
}
