/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.person.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.internal.PlanEntity;
import io.github.xmljim.retirement.retirementplanner.plan.internal.PlanJpaRepository;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonRepository;
import io.github.xmljim.retirement.retirementplanner.plan.salary.internal.SalaryProfileEntity;
import io.github.xmljim.retirement.retirementplanner.shared.TenantContext;

/**
 * Tenant-scoped implementation of {@link PersonRepository}. Tenant
 * filtering is enforced by joining through the parent Plan; the 1–2
 * person constraint on a Plan (ADR-002) is enforced here on add and
 * delete.
 */
@Repository
class PersonRepositoryImpl implements PersonRepository {

    private final PersonJpaRepository jpa;
    private final PlanJpaRepository planJpa;
    private final TenantContext tenantContext;

    PersonRepositoryImpl(PersonJpaRepository jpa, PlanJpaRepository planJpa, TenantContext tenantContext) {
        this.jpa = jpa;
        this.planJpa = planJpa;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional
    public Person addPerson(PlanId planId, Person person) {
        long activeTenant = tenantContext.currentTenantId();
        PlanEntity parent = planJpa.findByIdAndTenantId(planId.value(), activeTenant)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Plan " + planId.value() + " not found for tenant " + activeTenant));
        if (parent.getPersons().size() >= 2) {
            throw new IllegalArgumentException("Plan " + planId.value() + " already has the maximum of 2 persons");
        }
        PersonEntity entity = new PersonEntity();
        entity.setDob(person.dob());
        entity.setSalaryProfile(new SalaryProfileEntity());
        parent.addPerson(entity);
        PersonEntity saved = jpa.save(entity);
        return PersonMappers.toRecord(saved);
    }

    @Override
    @Transactional
    public Person update(Person person) {
        long activeTenant = tenantContext.currentTenantId();
        PersonId id = person.id().orElseThrow(() -> new IllegalArgumentException("Person id is required for update"));
        PersonEntity existing = jpa.findByIdAndPlanTenantId(id.value(), activeTenant)
                .orElseThrow(() ->
                        new IllegalArgumentException("Person " + id.value() + " not found for tenant " + activeTenant));
        existing.setDob(person.dob());
        return PersonMappers.toRecord(jpa.save(existing));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Person> findById(PersonId id) {
        return jpa.findByIdAndPlanTenantId(id.value(), tenantContext.currentTenantId())
                .map(PersonMappers::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Person> findByPlanId(PlanId planId) {
        return jpa.findAllByPlanIdAndPlanTenantId(planId.value(), tenantContext.currentTenantId()).stream()
                .map(PersonMappers::toRecord)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(PersonId id) {
        long activeTenant = tenantContext.currentTenantId();
        Optional<PersonEntity> existing = jpa.findByIdAndPlanTenantId(id.value(), activeTenant);
        if (existing.isEmpty()) {
            return;
        }
        Long planId = existing.orElseThrow().getPlan().getId();
        if (jpa.countByPlanId(planId) <= 1) {
            throw new IllegalArgumentException("Cannot delete the last Person from Plan " + planId);
        }
        jpa.deleteByIdAndPlanTenantId(id.value(), activeTenant);
    }
}
