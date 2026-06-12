/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.internal;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import io.github.xmljim.retirement.retirementplanner.api.PlanOperations;
import io.github.xmljim.retirement.retirementplanner.api.dto.PlanDto;
import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.PlanService;
import io.github.xmljim.retirement.retirementplanner.shared.TenantContext;

/**
 * Plan REST surface. Pure delegation per CLAUDE.md — no business logic
 * lives here; it sits on the {@link PlanService}.
 */
@RestController
class PlanController implements PlanOperations {

    private final PlanService service;
    private final TenantContext tenantContext;

    PlanController(PlanService service, TenantContext tenantContext) {
        this.service = service;
        this.tenantContext = tenantContext;
    }

    @Override
    public ResponseEntity<PlanDto> create(PlanDto plan) {
        Plan created = service.create(plan.toNewPlan(tenantContext.currentTenantId()));
        long newId = created.id().orElseThrow().value();
        return ResponseEntity.created(PlanOperations.locationOf(newId)).body(PlanDto.from(created));
    }

    @Override
    public PlanDto findById(long id) {
        return PlanDto.from(service.findById(new PlanId(id)));
    }

    @Override
    public List<PlanDto> findAll() {
        return service.findAll().stream().map(PlanDto::from).toList();
    }

    @Override
    public PlanDto replace(long id, PlanDto plan) {
        Plan replaced = service.replace(new PlanId(id), plan.toNewPlan(tenantContext.currentTenantId()));
        return PlanDto.from(replaced);
    }

    @Override
    public ResponseEntity<Void> deleteById(long id) {
        service.deleteById(new PlanId(id));
        return ResponseEntity.noContent().build();
    }
}
