/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.github.xmljim.retirement.retirementplanner.plan.ContributionAmount;
import io.github.xmljim.retirement.retirementplanner.plan.FixedDollar;
import io.github.xmljim.retirement.retirementplanner.plan.PercentOfSalary;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Tagged union mirroring the sealed {@link ContributionAmount} domain
 * type.
 *
 * <p>Wire shapes:
 * <pre>{@code
 *   {"type":"PERCENT_OF_SALARY","pct":"0.05"}
 *   {"type":"FIXED_DOLLAR","annualAmount":{"amount":"23000.00","currency":"USD"}}
 * }</pre>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ContributionAmountDto.PercentOfSalaryDto.class, name = "PERCENT_OF_SALARY"),
    @JsonSubTypes.Type(value = ContributionAmountDto.FixedDollarDto.class, name = "FIXED_DOLLAR")
})
@Schema(
        description = "Employee contribution amount on a ContributionPolicy.",
        oneOf = {ContributionAmountDto.PercentOfSalaryDto.class, ContributionAmountDto.FixedDollarDto.class},
        discriminatorProperty = "type")
public sealed interface ContributionAmountDto {

    static ContributionAmountDto from(ContributionAmount amount) {
        return switch (amount) {
            case PercentOfSalary p -> new PercentOfSalaryDto(p.pct());
            case FixedDollar f -> new FixedDollarDto(MoneyDto.from(f.annualAmount()));
        };
    }

    ContributionAmount toContributionAmount();

    record PercentOfSalaryDto(@NotNull BigDecimal pct) implements ContributionAmountDto {
        @Override
        public ContributionAmount toContributionAmount() {
            return new PercentOfSalary(pct);
        }
    }

    record FixedDollarDto(@NotNull @Valid MoneyDto annualAmount) implements ContributionAmountDto {
        @Override
        public ContributionAmount toContributionAmount() {
            return new FixedDollar(annualAmount.toMoney());
        }
    }
}
