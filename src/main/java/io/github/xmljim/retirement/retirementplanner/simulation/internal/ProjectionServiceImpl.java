/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation.internal;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.PlanService;
import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountRepository;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfile;
import io.github.xmljim.retirement.retirementplanner.shared.Money;
import io.github.xmljim.retirement.retirementplanner.simulation.AccumulationProjector;
import io.github.xmljim.retirement.retirementplanner.simulation.MonthlyProjection;
import io.github.xmljim.retirement.retirementplanner.simulation.ProjectionService;

/**
 * Default {@link ProjectionService} (S-2.8).
 *
 * <p>{@link Clock} is injected so tests can advance time deterministically;
 * the production bean uses the default {@code Clock.systemDefaultZone()}.
 */
@Service
class ProjectionServiceImpl implements ProjectionService {

    private final PlanService planService;
    private final AccountRepository accountRepository;
    private final AccumulationProjector projector;
    private final Clock clock;

    ProjectionServiceImpl(
            PlanService planService,
            AccountRepository accountRepository,
            AccumulationProjector projector,
            Clock clock) {
        this.planService = planService;
        this.accountRepository = accountRepository;
        this.projector = projector;
        this.clock = clock;
    }

    @Override
    public List<MonthlyProjection> deterministic(PlanId planId) {
        Plan plan = planService.findById(planId);
        List<Account> accounts = accountRepository.findByPlanId(planId);
        YearMonth startMonth = YearMonth.now(clock);
        Map<PersonId, SalaryProfile> profiles = defaultSalaryProfiles(plan, startMonth.atDay(1));
        return projector.project(plan, accounts, profiles, startMonth);
    }

    private static Map<PersonId, SalaryProfile> defaultSalaryProfiles(Plan plan, LocalDate startDate) {
        SalaryProfile zeroProfile = zeroSalaryProfile(startDate);
        return plan.persons().stream()
                .map(Person::id)
                .flatMap(Optional::stream)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(id -> id, _ -> zeroProfile));
    }

    private static SalaryProfile zeroSalaryProfile(LocalDate baseDate) {
        return new SalaryProfile(
                Optional.empty(),
                Money.ZERO_USD,
                baseDate,
                BigDecimal.ZERO,
                java.time.Month.JANUARY,
                List.of(),
                Optional.empty(),
                Optional.empty());
    }
}
