/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data interface for {@link PersonEntity}; tenant filtering happens above this layer. */
interface PersonJpaRepository extends JpaRepository<PersonEntity, Long> {

    Optional<PersonEntity> findByIdAndPlanTenantId(Long id, long tenantId);

    List<PersonEntity> findAllByPlanIdAndPlanTenantId(Long planId, long tenantId);

    long countByPlanId(Long planId);

    void deleteByIdAndPlanTenantId(Long id, long tenantId);
}
