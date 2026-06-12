/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import java.util.List;
import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.plan.Account;
import io.github.xmljim.retirement.retirementplanner.plan.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.AccountType;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Account DTO. {@code id} is null on POST/PUT (server-assigned).
 * {@code planId} on the DTO is ignored on POST/PUT — the path
 * {@code /plans/{planId}/accounts} is the source of truth.
 *
 * <p>{@code contributionPolicy} is optional: omit or send {@code null}
 * for accounts that have no funding stream (Roth IRA, taxable, etc.).
 */
public record AccountDto(
        Long id,
        Long planId,
        @NotNull AccountType type,
        @NotNull @Valid OwnerRefDto owner,
        @NotNull @NotEmpty @Valid List<AccountSleeveDto> sleeves,
        @Valid ContributionPolicyDto contributionPolicy) {

    public static AccountDto from(Account account) {
        return new AccountDto(
                account.id().map(AccountId::value).orElse(null),
                account.planId().value(),
                account.type(),
                OwnerRefDto.from(account.owner()),
                account.sleeves().stream().map(AccountSleeveDto::from).toList(),
                account.contributionPolicy().map(ContributionPolicyDto::from).orElse(null));
    }

    /**
     * Convert this DTO into a domain {@link Account} parented to the
     * given plan id. The DTO's own {@code planId} field is ignored.
     */
    public Account toNewAccount(PlanId parentPlanId) {
        return new Account(
                Optional.empty(),
                parentPlanId,
                type,
                owner.toOwnerRef(),
                sleeves.stream().map(AccountSleeveDto::toAccountSleeve).toList(),
                Optional.ofNullable(contributionPolicy).map(ContributionPolicyDto::toContributionPolicy));
    }
}
