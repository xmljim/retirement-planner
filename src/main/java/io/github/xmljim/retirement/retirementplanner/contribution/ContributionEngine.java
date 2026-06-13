/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution;

import java.time.Month;
import java.util.List;

import io.github.xmljim.retirement.retirementplanner.plan.account.Account;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfile;

/**
 * Computes the per-month employee + employer contributions for one
 * {@link Person} across all of their contributing accounts (ADR-003,
 * FR-2.5/FR-2.6).
 *
 * <p>Per-person scope: §402(g) caps elective deferrals across all
 * 401(k)/403(b) accounts a person owns; §408 caps Trad+Roth IRA
 * combined. Computing one person at a time lets the engine enforce
 * those cross-account caps in a single ledger query.
 *
 * <p>State threading: callers thread a {@link CashFlowLedger} through
 * 12 months. The engine queries the ledger for year-to-date totals
 * when applying caps; AC's &ldquo;year-boundary state resets at year
 * start&rdquo; is satisfied implicitly because {@code forYear(year)}
 * returns nothing on the first call of a fresh year.
 *
 * <p>Out of scope (deferred to later stories):
 * <ul>
 *   <li>SECURE 2.0 §604 employer-match Roth election — S-2.6</li>
 *   <li>HSA family-pool spousal sharing — tech-debt</li>
 *   <li>Per-employer §415(c) grouping (currently grouped per
 *       (owner, plan-family) within a Plan) — tech-debt</li>
 * </ul>
 *
 * <p>Modulith: hot-path consumer of {@code plan/} types (ADR-008).
 * Direct method-call dependency, not events.
 */
public interface ContributionEngine {

    /**
     * Computes the cash flows for one {@link Person} in one calendar
     * month across the supplied {@code accounts}, plus any
     * {@link EngineWarning}s raised while applying §603 / §604 routing
     * rules.
     *
     * @param person          whose accounts are being contributed to
     * @param accounts        the person's accounts (filter to those the
     *                        person owns at the call site; the engine
     *                        does not consult the repository)
     * @param salaryProfile   salary timeline for {@code person}
     * @param ledger          running ledger of prior flows; used for
     *                        year-to-date cap enforcement
     * @param year            the calendar year
     * @param month           the calendar month
     * @return result containing this month's flows (callers append to
     *         the ledger themselves) and any warnings to surface
     */
    MonthlyContributionResult contributeForMonth(
            Person person,
            List<Account> accounts,
            SalaryProfile salaryProfile,
            CashFlowLedger ledger,
            int year,
            Month month);
}
