/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.person;

import java.util.List;
import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.plan.PlanId;

/**
 * Public repository surface for {@link Person} operations within a
 * {@link Plan} (ADR-002).
 *
 * <p>Persons are owned by Plans; reads are scoped to the active tenant
 * by joining through the parent Plan. The 1–2 person constraint on
 * {@link Plan} is enforced here too: {@link #addPerson} refuses when
 * the parent already has two, and {@link #deleteById} refuses to
 * remove the last person in a Plan.
 */
public interface PersonRepository {

    /** Add a new Person to the given Plan. */
    Person addPerson(PlanId planId, Person person);

    /** Update an existing Person within the active tenant. */
    Person update(Person person);

    /** Find a Person by id within the active tenant. */
    Optional<Person> findById(PersonId id);

    /** All Persons belonging to the given Plan, scoped to the active tenant. */
    List<Person> findByPlanId(PlanId planId);

    /** Delete a Person by id within the active tenant. No-op if absent. */
    void deleteById(PersonId id);
}
