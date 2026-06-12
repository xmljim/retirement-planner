/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * An account held within a {@link Plan} (ADR-002). Aggregate root for
 * its {@link AccountSleeve} children.
 *
 * <p>{@link AccountType} carries the tax treatment as data — a single
 * entity per ADR-002 rather than a class hierarchy. Owner is a
 * {@link Person} ({@code Individual}) or {@code Joint}. A new account
 * defaults to a single {@link SleeveKind.AssetAllocation} sleeve
 * holding the full balance; see {@link #withDefaultSleeve}.
 *
 * <p>{@code id} is absent before persistence; the repository populates
 * it on save. {@code planId} is the parent Plan reference and is
 * required — accounts don't exist outside a Plan.
 *
 * <p>{@link #contributionPolicy()} is {@link Optional} per ADR-003: a
 * Roth IRA or taxable brokerage may have no employer-sponsored funding
 * stream, while a 401(k) typically does. When a policy carries an
 * {@link EmployerMatch}, the canonical constructor enforces that the
 * account is a 401(k) or 403(b) variant via
 * {@link ContributionPolicy#matchAllowedFor(AccountType)}.
 */
public record Account(
        Optional<AccountId> id,
        PlanId planId,
        AccountType type,
        OwnerRef owner,
        List<AccountSleeve> sleeves,
        Optional<ContributionPolicy> contributionPolicy) {

    public Account {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(planId, "planId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(contributionPolicy, "contributionPolicy");
        if (sleeves == null || sleeves.isEmpty()) {
            throw new IllegalArgumentException("Account requires at least one sleeve");
        }
        contributionPolicy.flatMap(ContributionPolicy::match).ifPresent(_ -> {
            if (!ContributionPolicy.matchAllowedFor(type)) {
                throw new IllegalArgumentException(
                        "EmployerMatch is only valid on 401(k) / 403(b) accounts; was: " + type);
            }
        });
        sleeves = List.copyOf(sleeves);
    }

    /** Convenience constructor for an unpersisted account without a contribution policy. */
    public static Account of(PlanId planId, AccountType type, OwnerRef owner, List<AccountSleeve> sleeves) {
        return new Account(Optional.empty(), planId, type, owner, sleeves, Optional.empty());
    }

    /** Convenience constructor for an unpersisted account with a contribution policy. */
    public static Account of(
            PlanId planId,
            AccountType type,
            OwnerRef owner,
            List<AccountSleeve> sleeves,
            ContributionPolicy contributionPolicy) {
        return new Account(Optional.empty(), planId, type, owner, sleeves, Optional.of(contributionPolicy));
    }

    /**
     * Constructs an unpersisted account with the AC-mandated default:
     * one {@link SleeveKind.AssetAllocation} sleeve holding the full
     * balance, yielding via {@link SleeveYieldPolicy.TracksAllocation}.
     */
    public static Account withDefaultSleeve(PlanId planId, AccountType type, OwnerRef owner, Money balance) {
        AccountSleeve sleeve =
                AccountSleeve.of(new SleeveKind.AssetAllocation(), balance, new SleeveYieldPolicy.TracksAllocation());
        return of(planId, type, owner, List.of(sleeve));
    }
}
