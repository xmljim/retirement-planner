/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.github.xmljim.retirement.retirementplanner.plan.SleeveYieldPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

/**
 * Tagged union mirroring the sealed {@link SleeveYieldPolicy} domain
 * type.
 *
 * <p>Wire shapes:
 * <pre>{@code
 *   {"type":"FIXED_RATE","annualRate":"0.045"}
 *   {"type":"MONEY_MARKET"}
 *   {"type":"TRACKS_ALLOCATION"}
 * }</pre>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SleeveYieldPolicyDto.FixedRate.class, name = "FIXED_RATE"),
    @JsonSubTypes.Type(value = SleeveYieldPolicyDto.MoneyMarket.class, name = "MONEY_MARKET"),
    @JsonSubTypes.Type(value = SleeveYieldPolicyDto.TracksAllocation.class, name = "TRACKS_ALLOCATION")
})
@Schema(
        description = "Yield model applied to a sleeve's balance.",
        oneOf = {
            SleeveYieldPolicyDto.FixedRate.class,
            SleeveYieldPolicyDto.MoneyMarket.class,
            SleeveYieldPolicyDto.TracksAllocation.class
        },
        discriminatorProperty = "type")
public sealed interface SleeveYieldPolicyDto {

    static SleeveYieldPolicyDto from(SleeveYieldPolicy policy) {
        return switch (policy) {
            case SleeveYieldPolicy.FixedRate fr -> new FixedRate(fr.annualRate());
            case SleeveYieldPolicy.MoneyMarket _ -> new MoneyMarket();
            case SleeveYieldPolicy.TracksAllocation _ -> new TracksAllocation();
        };
    }

    SleeveYieldPolicy toSleeveYieldPolicy();

    record FixedRate(@NotNull BigDecimal annualRate) implements SleeveYieldPolicyDto {
        @Override
        public SleeveYieldPolicy toSleeveYieldPolicy() {
            return new SleeveYieldPolicy.FixedRate(annualRate);
        }
    }

    record MoneyMarket() implements SleeveYieldPolicyDto {
        @Override
        public SleeveYieldPolicy toSleeveYieldPolicy() {
            return new SleeveYieldPolicy.MoneyMarket();
        }
    }

    record TracksAllocation() implements SleeveYieldPolicyDto {
        @Override
        public SleeveYieldPolicy toSleeveYieldPolicy() {
            return new SleeveYieldPolicy.TracksAllocation();
        }
    }
}
