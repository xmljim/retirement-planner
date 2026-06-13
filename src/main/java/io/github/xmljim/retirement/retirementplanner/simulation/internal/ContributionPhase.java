/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.simulation.internal;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.xmljim.retirement.retirementplanner.contribution.CashFlowLedger;
import io.github.xmljim.retirement.retirementplanner.contribution.ContributionEngine;
import io.github.xmljim.retirement.retirementplanner.contribution.MonthlyContributionResult;
import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.account.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.account.OwnerRef;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfile;
import io.github.xmljim.retirement.retirementplanner.shared.CashFlow;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Per-month contribution phase: walks the plan's persons, asks the
 * {@link ContributionEngine} for each person's flows, threads the
 * {@link CashFlowLedger} for cap enforcement, and aggregates results
 * by account for the projector's yield-and-deposit phase.
 *
 * <p>Extracted from {@code AccumulationProjectorImpl} to keep that
 * class under PMD's CouplingBetweenObjects threshold; this is a pure
 * orchestration helper with no own state once
 * {@link #apply(Plan, Map, CashFlowLedger, Map, YearMonth)} returns.
 */
final class ContributionPhase {

    private final ContributionEngine engine;

    ContributionPhase(ContributionEngine engine) {
        this.engine = engine;
    }

    Result apply(
            Plan plan,
            Map<PersonId, SalaryProfile> salaryProfiles,
            CashFlowLedger ledgerIn,
            Map<AccountId, MutableAccount> state,
            YearMonth period) {
        Accumulator accumulator = new Accumulator(ledgerIn);
        plan.persons().stream()
                .filter(p -> isActive(p, period))
                .filter(p -> p.id().isPresent())
                .filter(p -> salaryProfiles.containsKey(p.id().orElseThrow()))
                .forEach(person -> accumulator.accept(
                        person, salaryProfiles.get(person.id().orElseThrow()), accountsOwnedBy(state, person), period));
        return accumulator.snapshot();
    }

    private static boolean isActive(Person person, YearMonth period) {
        return period.atDay(1).isBefore(person.retirementDate());
    }

    private static List<Account> accountsOwnedBy(Map<AccountId, MutableAccount> state, Person person) {
        PersonId personId = person.id().orElseThrow();
        return state.values().stream()
                .map(MutableAccount::source)
                .filter(a -> ownedBy(a, personId))
                .toList();
    }

    private static boolean ownedBy(Account account, PersonId personId) {
        return switch (account.owner()) {
            case OwnerRef.Individual ind -> ind.personId().equals(personId);
            case OwnerRef.Joint _ -> true;
        };
    }

    record Result(CashFlowLedger ledger, Map<AccountId, Money> byAccount, List<CashFlow> flows) {}

    private final class Accumulator {

        private CashFlowLedger ledger;
        private final Map<AccountId, Money> byAccount = new HashMap<>();
        private final List<CashFlow> monthFlows = new ArrayList<>();

        Accumulator(CashFlowLedger initial) {
            this.ledger = initial;
        }

        void accept(Person person, SalaryProfile profile, List<Account> ownedAccounts, YearMonth period) {
            if (ownedAccounts.isEmpty()) {
                return;
            }
            MonthlyContributionResult result = engine.contributeForMonth(
                    person, ownedAccounts, profile, ledger, period.getYear(), period.getMonth());
            ledger = ledger.appendAll(result.flows());
            result.flows().forEach(flow -> {
                monthFlows.add(flow);
                byAccount.merge(new AccountId(flow.accountId()), flow.amount(), Money::plus);
            });
        }

        Result snapshot() {
            return new Result(ledger, Map.copyOf(byAccount), List.copyOf(monthFlows));
        }
    }
}
