/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.github.xmljim.retirement.retirementplanner.plan.AccountType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "account")
class AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false)
    private OwnerType ownerType;

    @ManyToOne
    @JoinColumn(name = "owner_person_id")
    private PersonEntity ownerPerson;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<AccountSleeveEntity> sleeves = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    Long getId() {
        return id;
    }

    PlanEntity getPlan() {
        return plan;
    }

    void setPlan(PlanEntity plan) {
        this.plan = plan;
    }

    AccountType getAccountType() {
        return accountType;
    }

    void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    OwnerType getOwnerType() {
        return ownerType;
    }

    void setOwnerType(OwnerType ownerType) {
        this.ownerType = ownerType;
    }

    PersonEntity getOwnerPerson() {
        return ownerPerson;
    }

    void setOwnerPerson(PersonEntity ownerPerson) {
        this.ownerPerson = ownerPerson;
    }

    List<AccountSleeveEntity> getSleeves() {
        return sleeves;
    }

    void addSleeve(AccountSleeveEntity sleeve) {
        sleeves.add(sleeve);
        sleeve.setAccount(this);
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    enum OwnerType {
        INDIVIDUAL,
        JOINT
    }
}
