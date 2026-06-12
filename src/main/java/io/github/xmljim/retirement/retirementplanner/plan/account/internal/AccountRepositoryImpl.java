/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.account.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountRepository;
import io.github.xmljim.retirement.retirementplanner.plan.internal.PlanEntity;
import io.github.xmljim.retirement.retirementplanner.plan.internal.PlanJpaRepository;
import io.github.xmljim.retirement.retirementplanner.shared.TenantContext;

/**
 * Tenant-scoped implementation of {@link AccountRepository}. Tenant
 * filtering is enforced by joining through the parent Plan; a Plan can
 * never live in two tenants, so an Account inherits its parent's
 * tenant transitively.
 */
@Repository
class AccountRepositoryImpl implements AccountRepository {

    private final AccountJpaRepository jpa;
    private final PlanJpaRepository planJpa;
    private final AccountMapper mapper;
    private final TenantContext tenantContext;

    AccountRepositoryImpl(
            AccountJpaRepository jpa, PlanJpaRepository planJpa, AccountMapper mapper, TenantContext tenantContext) {
        this.jpa = jpa;
        this.planJpa = planJpa;
        this.mapper = mapper;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional
    public Account save(Account account) {
        long activeTenant = tenantContext.currentTenantId();
        requireParentInActiveTenant(account.planId(), activeTenant);
        AccountEntity entity = account.id()
                .map(id -> loadForUpdate(id, activeTenant, account))
                .orElseGet(() -> mapper.toEntity(account));
        AccountEntity saved = jpa.save(entity);
        return mapper.toRecord(saved);
    }

    private void requireParentInActiveTenant(PlanId planId, long activeTenant) {
        PlanEntity parent = planJpa.findById(planId.value())
                .orElseThrow(() -> new IllegalArgumentException("Plan " + planId.value() + " not found"));
        if (parent.getTenantId() != activeTenant) {
            throw new IllegalArgumentException("Plan tenant mismatch: tried to save Account in tenant " + activeTenant
                    + " but parent Plan belongs to tenant " + parent.getTenantId());
        }
    }

    private AccountEntity loadForUpdate(AccountId id, long activeTenant, Account account) {
        AccountEntity existing = jpa.findByIdAndPlanTenantId(id.value(), activeTenant)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account " + id.value() + " not found for tenant " + activeTenant));
        if (existing.getPlan().getId() != account.planId().value()) {
            throw new IllegalArgumentException("Account " + id.value() + " cannot be reparented to a different Plan");
        }
        mapper.applyAccountScalars(existing, account);
        existing.getSleeves().clear();
        account.sleeves().forEach(sleeve -> existing.addSleeve(mapper.toSleeveEntity(sleeve)));
        return existing;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Account> findById(AccountId id) {
        return jpa.findByIdAndPlanTenantId(id.value(), tenantContext.currentTenantId())
                .map(mapper::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> findByPlanId(PlanId planId) {
        return jpa.findAllByPlanIdAndPlanTenantId(planId.value(), tenantContext.currentTenantId()).stream()
                .map(mapper::toRecord)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(AccountId id) {
        jpa.deleteByIdAndPlanTenantId(id.value(), tenantContext.currentTenantId());
    }
}
