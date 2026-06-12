/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.person.internal;

import java.util.List;

import org.springframework.stereotype.Service;

import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonRepository;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonService;
import io.github.xmljim.retirement.retirementplanner.shared.NotFoundException;

@Service
class PersonServiceImpl implements PersonService {

    private final PersonRepository repository;

    PersonServiceImpl(PersonRepository repository) {
        this.repository = repository;
    }

    @Override
    public Person add(PlanId planId, Person person) {
        return repository.addPerson(planId, person);
    }

    @Override
    public Person findById(PersonId id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    @Override
    public List<Person> findByPlanId(PlanId planId) {
        return repository.findByPlanId(planId);
    }

    @Override
    public Person replace(PersonId id, Person replacement) {
        Person existing = repository.findById(id).orElseThrow(() -> notFound(id));
        Person merged = new Person(existing.id(), existing.salaryProfileId(), replacement.dob());
        return repository.update(merged);
    }

    @Override
    public void deleteById(PersonId id) {
        repository.deleteById(id);
    }

    private static NotFoundException notFound(PersonId id) {
        return new NotFoundException("Person " + id.value() + " not found");
    }
}
