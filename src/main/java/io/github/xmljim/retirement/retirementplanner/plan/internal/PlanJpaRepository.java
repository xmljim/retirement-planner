/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data interface for {@link PlanEntity}; tenant filtering happens above this layer. */
interface PlanJpaRepository extends JpaRepository<PlanEntity, Long> {

    Optional<PlanEntity> findByIdAndTenantId(Long id, long tenantId);

    List<PlanEntity> findAllByTenantId(long tenantId);

    void deleteByIdAndTenantId(Long id, long tenantId);
}
