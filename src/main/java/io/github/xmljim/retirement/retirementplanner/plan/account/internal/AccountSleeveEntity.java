/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.account.internal;

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
public class AccountSleeveEntity {

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

    public Long getId() {
        return id;
    }

    public AccountEntity getAccount() {
        return account;
    }

    public void setAccount(AccountEntity account) {
        this.account = account;
    }

    public KindType getKindType() {
        return kindType;
    }

    public void setKindType(KindType kindType) {
        this.kindType = kindType;
    }

    public String getKindData() {
        return kindData;
    }

    public void setKindData(String kindData) {
        this.kindData = kindData;
    }

    public YieldType getYieldType() {
        return yieldType;
    }

    public void setYieldType(YieldType yieldType) {
        this.yieldType = yieldType;
    }

    public String getYieldData() {
        return yieldData;
    }

    public void setYieldData(String yieldData) {
        this.yieldData = yieldData;
    }

    public MoneyEmbeddable getBalance() {
        return balance;
    }

    public void setBalance(MoneyEmbeddable balance) {
        this.balance = balance;
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

    public enum KindType {
        CASH,
        ASSET_ALLOCATION,
        FIXED_ALLOCATION
    }

    public enum YieldType {
        FIXED_RATE,
        MONEY_MARKET,
        TRACKS_ALLOCATION
    }
}
