/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.PlanRepository;
import io.github.xmljim.retirement.retirementplanner.shared.TenantContext;

/**
 * Tenant-scoped implementation of {@link PlanRepository}. Reads and
 * writes are filtered by the {@link TenantContext}'s current tenant id;
 * cross-tenant reads are not reachable from this surface.
 */
@Repository
public class PlanRepositoryImpl implements PlanRepository {

    private final PlanJpaRepository jpa;
    private final TenantContext tenantContext;

    PlanRepositoryImpl(PlanJpaRepository jpa, TenantContext tenantContext) {
        this.jpa = jpa;
        this.tenantContext = tenantContext;
    }

    @Override
    @Transactional
    public Plan save(Plan plan) {
        long activeTenant = tenantContext.currentTenantId();
        PlanEntity entity = plan.id()
                .map(id -> loadForUpdate(id, activeTenant, plan))
                .orElseGet(() -> {
                    if (plan.tenantId() != activeTenant) {
                        throw new IllegalArgumentException("Plan tenant mismatch: tried to save in tenant "
                                + activeTenant + " but plan declares " + plan.tenantId());
                    }
                    return PlanMapper.toEntity(plan);
                });
        PlanEntity saved = jpa.save(entity);
        return PlanMapper.toRecord(saved);
    }

    private PlanEntity loadForUpdate(PlanId id, long activeTenant, Plan plan) {
        PlanEntity existing = jpa.findByIdAndTenantId(id.value(), activeTenant)
                .orElseThrow(() ->
                        new IllegalArgumentException("Plan " + id.value() + " not found for tenant " + activeTenant));
        existing.getHousehold().setFilingStatus(plan.household().filingStatus());
        existing.getHousehold().setState(plan.household().state());
        return existing;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Plan> findById(PlanId id) {
        return jpa.findByIdAndTenantId(id.value(), tenantContext.currentTenantId())
                .map(PlanMapper::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan> findAll() {
        return jpa.findAllByTenantId(tenantContext.currentTenantId()).stream()
                .map(PlanMapper::toRecord)
                .toList();
    }

    @Override
    @Transactional
    public void deleteById(PlanId id) {
        jpa.deleteByIdAndTenantId(id.value(), tenantContext.currentTenantId());
    }
}
