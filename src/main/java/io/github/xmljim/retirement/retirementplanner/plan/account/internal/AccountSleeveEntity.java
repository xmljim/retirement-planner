/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.xmljim.retirement.retirementplanner.shared.MoneyEmbeddable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_sleeve")
class AccountSleeveEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind_type", nullable = false)
    private KindType kindType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "kind_data")
    private String kindData;

    @Enumerated(EnumType.STRING)
    @Column(name = "yield_type", nullable = false)
    private YieldType yieldType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "yield_data")
    private String yieldData;

    @Embedded
    @AttributeOverride(
            name = "amount",
            column = @Column(name = "balance_amount", nullable = false, precision = 19, scale = 6))
    @AttributeOverride(name = "currencyCode", column = @Column(name = "balance_currency", nullable = false, length = 3))
    private MoneyEmbeddable balance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    Long getId() {
        return id;
    }

    AccountEntity getAccount() {
        return account;
    }

    void setAccount(AccountEntity account) {
        this.account = account;
    }

    KindType getKindType() {
        return kindType;
    }

    void setKindType(KindType kindType) {
        this.kindType = kindType;
    }

    String getKindData() {
        return kindData;
    }

    void setKindData(String kindData) {
        this.kindData = kindData;
    }

    YieldType getYieldType() {
        return yieldType;
    }

    void setYieldType(YieldType yieldType) {
        this.yieldType = yieldType;
    }

    String getYieldData() {
        return yieldData;
    }

    void setYieldData(String yieldData) {
        this.yieldData = yieldData;
    }

    MoneyEmbeddable getBalance() {
        return balance;
    }

    void setBalance(MoneyEmbeddable balance) {
        this.balance = balance;
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

    enum KindType {
        CASH,
        ASSET_ALLOCATION,
        FIXED_ALLOCATION
    }

    enum YieldType {
        FIXED_RATE,
        MONEY_MARKET,
        TRACKS_ALLOCATION
    }
}
