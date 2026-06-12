/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import io.github.xmljim.retirement.retirementplanner.plan.FilingStatus;
import io.github.xmljim.retirement.retirementplanner.plan.Household;
import io.github.xmljim.retirement.retirementplanner.plan.HouseholdId;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Household DTO. {@code id} is null on POST and populated on read.
 */
public record HouseholdDto(
        Long id,
        @NotNull FilingStatus filingStatus,

        @NotNull @Pattern(regexp = "[A-Z]{2}", message = "state must be a two-letter US state code")
        String state) {

    public static HouseholdDto from(Household household) {
        return new HouseholdDto(
                household.id().map(HouseholdId::value).orElse(null), household.filingStatus(), household.state());
    }
}
