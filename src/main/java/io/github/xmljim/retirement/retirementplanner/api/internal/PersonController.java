/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import io.github.xmljim.retirement.retirementplanner.api.PersonOperations;
import io.github.xmljim.retirement.retirementplanner.api.dto.PersonDto;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonService;

@RestController
class PersonController implements PersonOperations {

    private final PersonService service;

    PersonController(PersonService service) {
        this.service = service;
    }

    @Override
    public List<PersonDto> findByPlanId(long planId) {
        return service.findByPlanId(new PlanId(planId)).stream()
                .map(PersonDto::from)
                .toList();
    }

    @Override
    public ResponseEntity<PersonDto> add(long planId, PersonDto person) {
        Person created = service.add(new PlanId(planId), Person.of(person.dob(), person.retirementDate()));
        long newId = created.id().orElseThrow().value();
        return ResponseEntity.created(PersonOperations.locationOf(newId)).body(PersonDto.from(created));
    }

    @Override
    public PersonDto findById(long id) {
        return PersonDto.from(service.findById(new PersonId(id)));
    }

    @Override
    public PersonDto replace(long id, PersonDto person) {
        Person replacement = new Person(Optional.empty(), Optional.empty(), person.dob(), person.retirementDate());
        return PersonDto.from(service.replace(new PersonId(id), replacement));
    }

    @Override
    public ResponseEntity<Void> deleteById(long id) {
        service.deleteById(new PersonId(id));
        return ResponseEntity.noContent().build();
    }
}
