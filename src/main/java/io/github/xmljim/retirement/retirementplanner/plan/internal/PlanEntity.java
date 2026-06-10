/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "plan")
class PlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private long tenantId;

    @OneToOne(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    private HouseholdEntity household;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PersonEntity> persons = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    Long getId() {
        return id;
    }

    long getTenantId() {
        return tenantId;
    }

    void setTenantId(long tenantId) {
        this.tenantId = tenantId;
    }

    HouseholdEntity getHousehold() {
        return household;
    }

    void setHousehold(HouseholdEntity household) {
        this.household = household;
        if (household != null) {
            household.setPlan(this);
        }
    }

    List<PersonEntity> getPersons() {
        return persons;
    }

    void addPerson(PersonEntity person) {
        persons.add(person);
        person.setPlan(this);
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @jakarta.persistence.PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
