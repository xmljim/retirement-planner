/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import java.util.List;
import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.household.Household;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Plan DTO. {@code id} and {@code tenantId} are null on POST; the server
 * stamps the active tenant on creation.
 */
public record PlanDto(
        Long id,
        Long tenantId,
        @NotNull @Valid HouseholdDto household,
        @NotNull @NotEmpty @Size(max = 2) @Valid List<PersonDto> persons,
        @NotNull @Valid AssumptionsDto assumptions) {

    public static PlanDto from(Plan plan) {
        return new PlanDto(
                plan.id().map(PlanId::value).orElse(null),
                plan.tenantId(),
                HouseholdDto.from(plan.household()),
                plan.persons().stream().map(PersonDto::from).toList(),
                AssumptionsDto.from(plan.assumptions()));
    }

    /**
     * Convert this DTO into a domain {@link Plan} for the given active
     * tenant. Any {@code id}/{@code tenantId} on the DTO is ignored —
     * the server is the source of truth.
     */
    public Plan toNewPlan(long activeTenant) {
        Household domainHousehold = Household.of(household.filingStatus(), household.state());
        List<Person> domainPersons = persons.stream()
                .map(p -> Person.of(p.dob(), p.retirementDate()))
                .toList();
        return new Plan(Optional.empty(), activeTenant, domainHousehold, domainPersons, assumptions.toRecord());
    }
}
