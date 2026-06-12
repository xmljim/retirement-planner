/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.util.List;

/**
 * Application-level facade for {@link Person} CRUD inside a
 * {@link Plan}. Throws {@link io.github.xmljim.retirement.retirementplanner.shared.NotFoundException}
 * when a Person or its parent Plan is not visible in the active tenant.
 */
public interface PersonService {

    /** Add a new Person to the given Plan. */
    Person add(PlanId planId, Person person);

    /** Find a Person by id, or throw {@code NotFoundException}. */
    Person findById(PersonId id);

    /** All Persons belonging to the given Plan. */
    List<Person> findByPlanId(PlanId planId);

    /** Replace the scalars of an existing Person. */
    Person replace(PersonId id, Person replacement);

    /** Delete a Person by id. No-op if absent. */
    void deleteById(PersonId id);
}
