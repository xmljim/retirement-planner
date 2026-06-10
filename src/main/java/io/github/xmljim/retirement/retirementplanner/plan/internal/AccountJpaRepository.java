/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data interface for {@link AccountEntity}; tenant filtering
 * happens in {@link AccountRepositoryImpl} above this layer by joining
 * through the parent Plan.
 */
interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByIdAndPlanTenantId(Long id, long tenantId);

    List<AccountEntity> findAllByPlanIdAndPlanTenantId(Long planId, long tenantId);

    void deleteByIdAndPlanTenantId(Long id, long tenantId);
}
