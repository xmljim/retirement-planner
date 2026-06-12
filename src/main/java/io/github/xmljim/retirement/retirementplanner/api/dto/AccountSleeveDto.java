/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import io.github.xmljim.retirement.retirementplanner.plan.AccountSleeve;
import io.github.xmljim.retirement.retirementplanner.plan.SleeveId;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Account sleeve DTO. {@code id} is null on POST/PUT (server-assigned).
 */
public record AccountSleeveDto(
        Long id,
        @NotNull @Valid SleeveKindDto kind,
        @NotNull @Valid MoneyDto balance,
        @NotNull @Valid SleeveYieldPolicyDto yieldPolicy) {

    public static AccountSleeveDto from(AccountSleeve sleeve) {
        return new AccountSleeveDto(
                sleeve.id().map(SleeveId::value).orElse(null),
                SleeveKindDto.from(sleeve.kind()),
                MoneyDto.from(sleeve.balance()),
                SleeveYieldPolicyDto.from(sleeve.yieldPolicy()));
    }

    public AccountSleeve toAccountSleeve() {
        return AccountSleeve.of(kind.toSleeveKind(), balance.toMoney(), yieldPolicy.toSleeveYieldPolicy());
    }
}
