/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.util.List;

import org.springframework.stereotype.Service;

import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.PlanRepository;
import io.github.xmljim.retirement.retirementplanner.plan.PlanService;
import io.github.xmljim.retirement.retirementplanner.shared.NotFoundException;

@Service
class PlanServiceImpl implements PlanService {

    private final PlanRepository repository;

    PlanServiceImpl(PlanRepository repository) {
        this.repository = repository;
    }

    @Override
    public Plan create(Plan plan) {
        return repository.save(plan);
    }

    @Override
    public Plan findById(PlanId id) {
        return repository.findById(id).orElseThrow(() -> notFound(id));
    }

    @Override
    public List<Plan> findAll() {
        return repository.findAll();
    }

    @Override
    public Plan replace(PlanId id, Plan replacement) {
        Plan existing = repository.findById(id).orElseThrow(() -> notFound(id));
        Plan merged = new Plan(
                existing.id(),
                existing.tenantId(),
                replacement.household(),
                existing.persons(),
                replacement.assumptions());
        return repository.save(merged);
    }

    @Override
    public void deleteById(PlanId id) {
        repository.deleteById(id);
    }

    private static NotFoundException notFound(PlanId id) {
        return new NotFoundException("Plan " + id.value() + " not found");
    }
}
