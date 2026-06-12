/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.account.internal;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.xmljim.retirement.retirementplanner.plan.account.AccountType;
import io.github.xmljim.retirement.retirementplanner.plan.internal.PlanEntity;
import io.github.xmljim.retirement.retirementplanner.plan.person.internal.PersonEntity;

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
public class AccountEntity {

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

    public Long getId() {
        return id;
    }

    public PlanEntity getPlan() {
        return plan;
    }

    public void setPlan(PlanEntity plan) {
        this.plan = plan;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public OwnerType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(OwnerType ownerType) {
        this.ownerType = ownerType;
    }

    public PersonEntity getOwnerPerson() {
        return ownerPerson;
    }

    public void setOwnerPerson(PersonEntity ownerPerson) {
        this.ownerPerson = ownerPerson;
    }

    public List<AccountSleeveEntity> getSleeves() {
        return sleeves;
    }

    public void addSleeve(AccountSleeveEntity sleeve) {
        sleeves.add(sleeve);
        sleeve.setAccount(this);
    }

    public ContributionAmountType getContributionAmountType() {
        return contributionAmountType;
    }

    public void setContributionAmountType(ContributionAmountType contributionAmountType) {
        this.contributionAmountType = contributionAmountType;
    }

    public String getContributionAmountData() {
        return contributionAmountData;
    }

    public void setContributionAmountData(String contributionAmountData) {
        this.contributionAmountData = contributionAmountData;
    }

    public String getEscalationData() {
        return escalationData;
    }

    public void setEscalationData(String escalationData) {
        this.escalationData = escalationData;
    }

    public String getEmployerMatchData() {
        return employerMatchData;
    }

    public void setEmployerMatchData(String employerMatchData) {
        this.employerMatchData = employerMatchData;
    }

    public LocalDate getContributionStartDate() {
        return contributionStartDate;
    }

    public void setContributionStartDate(LocalDate contributionStartDate) {
        this.contributionStartDate = contributionStartDate;
    }

    public LocalDate getContributionEndDate() {
        return contributionEndDate;
    }

    public void setContributionEndDate(LocalDate contributionEndDate) {
        this.contributionEndDate = contributionEndDate;
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

    public enum OwnerType {
        INDIVIDUAL,
        JOINT
    }

    public enum ContributionAmountType {
        PERCENT_OF_SALARY,
        FIXED_DOLLAR
    }
}
