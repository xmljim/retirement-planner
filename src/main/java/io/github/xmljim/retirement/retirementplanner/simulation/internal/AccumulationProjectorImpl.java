/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation.internal;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import io.github.xmljim.retirement.retirementplanner.accumulation.SleeveYieldEngine;
import io.github.xmljim.retirement.retirementplanner.contribution.CashFlowLedger;
import io.github.xmljim.retirement.retirementplanner.contribution.ContributionEngine;
import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfile;
import io.github.xmljim.retirement.retirementplanner.simulation.AccumulationProjector;
import io.github.xmljim.retirement.retirementplanner.simulation.MonthlyProjection;
import io.github.xmljim.retirement.retirementplanner.simulation.ProjectionPhase;

/**
 * Default {@link AccumulationProjector} implementation (S-2.8).
 *
 * <p>Composes two pure phases each month from {@code startMonth}
 * through the household horizon (latest person retirement date):
 * <ol>
 *   <li>{@link ContributionPhase}: walks the plan's persons and asks
 *       the contribution engine for each person's flows; threads the
 *       running {@link CashFlowLedger} for cap enforcement; aggregates
 *       results by account.</li>
 *   <li>{@link YieldDepositPhase}: applies start-of-month yield to
 *       every sleeve, then distributes the month's contributions
 *       pro-rata via {@link ContributionDistributor}.</li>
 * </ol>
 *
 * <p>"Actively contributing" means {@code today.isBefore(retirementDate)}
 * for the person; on the month of retirement and beyond, no
 * contribution flows are generated for that person, but yield
 * continues to compound on existing balances. Joint accounts
 * contribute as long as either spouse is still active — for §603 / §604
 * purposes the contribution engine's per-person scope is preserved by
 * making each person's monthly call independently against the joint
 * accounts they have access to.
 */
@Service
class AccumulationProjectorImpl implements AccumulationProjector {

    private final ContributionPhase contributionPhase;
    private final YieldDepositPhase yieldDepositPhase;

    AccumulationProjectorImpl(ContributionEngine contributionEngine, SleeveYieldEngine sleeveYieldEngine) {
        this.contributionPhase = new ContributionPhase(contributionEngine);
        this.yieldDepositPhase = new YieldDepositPhase(sleeveYieldEngine);
    }

    @Override
    public List<MonthlyProjection> project(
            Plan plan, List<Account> accounts, Map<PersonId, SalaryProfile> salaryProfiles, YearMonth startMonth) {
        YearMonth endMonth = ProjectionHorizon.of(plan);
        if (endMonth.isBefore(startMonth)) {
            return List.of();
        }
        Map<AccountId, MutableAccount> state = MutableAccount.initialState(accounts);
        List<MonthlyProjection> projections = new ArrayList<>();
        CashFlowLedger ledger = CashFlowLedger.empty();
        YearMonth period = startMonth;
        while (!period.isAfter(endMonth)) {
            ContributionPhase.Result phase = contributionPhase.apply(plan, salaryProfiles, ledger, state, period);
            yieldDepositPhase.apply(plan.assumptions(), state, phase.byAccount(), period);
            projections.add(new MonthlyProjection(
                    period, ProjectionPhase.ACCUMULATION, MutableAccount.snapshotBalances(state), phase.flows()));
            ledger = phase.ledger();
            period = period.plusMonths(1);
        }
        return List.copyOf(projections);
    }
}
