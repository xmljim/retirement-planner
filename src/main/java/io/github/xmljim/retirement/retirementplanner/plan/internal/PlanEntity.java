/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.github.xmljim.retirement.retirementplanner.plan.household.internal.HouseholdEntity;
import io.github.xmljim.retirement.retirementplanner.plan.person.internal.PersonEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "plan")
public class PlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private long tenantId;

    @OneToOne(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    private HouseholdEntity household;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<PersonEntity> persons = new ArrayList<>();

    @Column(name = "pre_retirement_return_rate", nullable = false, precision = 7, scale = 6)
    private BigDecimal preRetirementReturnRate;

    @Column(name = "cash_interest_rate", nullable = false, precision = 7, scale = 6)
    private BigDecimal cashInterestRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public long getTenantId() {
        return tenantId;
    }

    public void setTenantId(long tenantId) {
        this.tenantId = tenantId;
    }

    public HouseholdEntity getHousehold() {
        return household;
    }

    public void setHousehold(HouseholdEntity household) {
        this.household = household;
        if (household != null) {
            household.setPlan(this);
        }
    }

    public List<PersonEntity> getPersons() {
        return persons;
    }

    public void addPerson(PersonEntity person) {
        persons.add(person);
        person.setPlan(this);
    }

    public BigDecimal getPreRetirementReturnRate() {
        return preRetirementReturnRate;
    }

    public void setPreRetirementReturnRate(BigDecimal preRetirementReturnRate) {
        this.preRetirementReturnRate = preRetirementReturnRate;
    }

    public BigDecimal getCashInterestRate() {
        return cashInterestRate;
    }

    public void setCashInterestRate(BigDecimal cashInterestRate) {
        this.cashInterestRate = cashInterestRate;
    }

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }
}
