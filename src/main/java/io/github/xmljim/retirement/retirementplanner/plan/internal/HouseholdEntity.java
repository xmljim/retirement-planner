/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.time.Instant;

import io.github.xmljim.retirement.retirementplanner.plan.FilingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "household")
class HouseholdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "plan_id", nullable = false, unique = true)
    private PlanEntity plan;

    @Enumerated(EnumType.STRING)
    @Column(name = "filing_status", nullable = false)
    private FilingStatus filingStatus;

    @Column(nullable = false)
    private String state;

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

    FilingStatus getFilingStatus() {
        return filingStatus;
    }

    void setFilingStatus(FilingStatus filingStatus) {
        this.filingStatus = filingStatus;
    }

    String getState() {
        return state;
    }

    void setState(String state) {
        this.state = state;
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
