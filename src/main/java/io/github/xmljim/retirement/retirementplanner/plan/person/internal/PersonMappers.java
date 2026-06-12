/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.plan.Person;
import io.github.xmljim.retirement.retirementplanner.plan.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.SalaryProfileId;

final class PersonMappers {

    private PersonMappers() {}

    static Person toRecord(PersonEntity entity) {
        return new Person(
                Optional.of(new PersonId(entity.getId())),
                Optional.of(new SalaryProfileId(entity.getSalaryProfile().getId())),
                entity.getDob());
    }
}
