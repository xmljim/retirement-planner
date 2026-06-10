/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.util.List;
import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.plan.Household;
import io.github.xmljim.retirement.retirementplanner.plan.HouseholdId;
import io.github.xmljim.retirement.retirementplanner.plan.Person;
import io.github.xmljim.retirement.retirementplanner.plan.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.SalaryProfileId;

final class PlanMapper {

    private PlanMapper() {}

    static Plan toRecord(PlanEntity entity) {
        Household household = new Household(
                Optional.of(new HouseholdId(entity.getHousehold().getId())),
                entity.getHousehold().getFilingStatus(),
                entity.getHousehold().getState());
        List<Person> persons = entity.getPersons().stream()
                .map(p -> new Person(
                        Optional.of(new PersonId(p.getId())),
                        Optional.of(new SalaryProfileId(p.getSalaryProfile().getId())),
                        p.getDob()))
                .toList();
        return new Plan(Optional.of(new PlanId(entity.getId())), entity.getTenantId(), household, persons);
    }

    static PlanEntity toEntity(Plan plan) {
        PlanEntity entity = new PlanEntity();
        entity.setTenantId(plan.tenantId());

        HouseholdEntity household = new HouseholdEntity();
        household.setFilingStatus(plan.household().filingStatus());
        household.setState(plan.household().state());
        entity.setHousehold(household);

        plan.persons().forEach(person -> {
            PersonEntity pe = new PersonEntity();
            pe.setDob(person.dob());
            pe.setSalaryProfile(new SalaryProfileEntity());
            entity.addPerson(pe);
        });

        return entity;
    }
}
