/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "contribution_amount_type")
    private ContributionAmountType contributionAmountType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contribution_amount_data")
    private String contributionAmountData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "escalation_data")
    private String escalationData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "employer_match_data")
    private String employerMatchData;

    @Column(name = "contribution_start_date")
    private LocalDate contributionStartDate;

    @Column(name = "contribution_end_date")
    private LocalDate contributionEndDate;

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

    ContributionAmountType getContributionAmountType() {
        return contributionAmountType;
    }

    void setContributionAmountType(ContributionAmountType contributionAmountType) {
        this.contributionAmountType = contributionAmountType;
    }

    String getContributionAmountData() {
        return contributionAmountData;
    }

    void setContributionAmountData(String contributionAmountData) {
        this.contributionAmountData = contributionAmountData;
    }

    String getEscalationData() {
        return escalationData;
    }

    void setEscalationData(String escalationData) {
        this.escalationData = escalationData;
    }

    String getEmployerMatchData() {
        return employerMatchData;
    }

    void setEmployerMatchData(String employerMatchData) {
        this.employerMatchData = employerMatchData;
    }

    LocalDate getContributionStartDate() {
        return contributionStartDate;
    }

    void setContributionStartDate(LocalDate contributionStartDate) {
        this.contributionStartDate = contributionStartDate;
    }

    LocalDate getContributionEndDate() {
        return contributionEndDate;
    }

    void setContributionEndDate(LocalDate contributionEndDate) {
        this.contributionEndDate = contributionEndDate;
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

    enum ContributionAmountType {
        PERCENT_OF_SALARY,
        FIXED_DOLLAR
    }
}
