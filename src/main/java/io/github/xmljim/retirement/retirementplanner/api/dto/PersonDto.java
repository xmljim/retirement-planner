/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import java.time.LocalDate;

import io.github.xmljim.retirement.retirementplanner.plan.Person;
import io.github.xmljim.retirement.retirementplanner.plan.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.SalaryProfileId;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

/**
 * Person DTO. {@code id} and {@code salaryProfileId} are null on POST
 * and populated on read.
 */
public record PersonDto(
        Long id, Long salaryProfileId, @NotNull @Past LocalDate dob) {

    public static PersonDto from(Person person) {
        return new PersonDto(
                person.id().map(PersonId::value).orElse(null),
                person.salaryProfileId().map(SalaryProfileId::value).orElse(null),
                person.dob());
    }
}
