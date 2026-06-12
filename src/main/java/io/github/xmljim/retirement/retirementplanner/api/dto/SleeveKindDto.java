/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.github.xmljim.retirement.retirementplanner.plan.SleeveKind;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Tagged union mirroring the sealed {@link SleeveKind} domain type.
 *
 * <p>Wire shapes:
 * <pre>{@code
 *   {"type":"CASH"}
 *   {"type":"ASSET_ALLOCATION"}
 *   {"type":"FIXED_ALLOCATION","weights":{"EQUITY":"0.65","BOND":"0.35"}}
 * }</pre>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SleeveKindDto.Cash.class, name = "CASH"),
    @JsonSubTypes.Type(value = SleeveKindDto.AssetAllocation.class, name = "ASSET_ALLOCATION"),
    @JsonSubTypes.Type(value = SleeveKindDto.FixedAllocation.class, name = "FIXED_ALLOCATION")
})
@Schema(
        description = "Sleeve allocation behavior.",
        oneOf = {SleeveKindDto.Cash.class, SleeveKindDto.AssetAllocation.class, SleeveKindDto.FixedAllocation.class},
        discriminatorProperty = "type")
public sealed interface SleeveKindDto {

    static SleeveKindDto from(SleeveKind kind) {
        return switch (kind) {
            case SleeveKind.Cash _ -> new Cash();
            case SleeveKind.AssetAllocation _ -> new AssetAllocation();
            case SleeveKind.FixedAllocation fa -> new FixedAllocation(fa.weights());
        };
    }

    SleeveKind toSleeveKind();

    record Cash() implements SleeveKindDto {
        @Override
        public SleeveKind toSleeveKind() {
            return new SleeveKind.Cash();
        }
    }

    record AssetAllocation() implements SleeveKindDto {
        @Override
        public SleeveKind toSleeveKind() {
            return new SleeveKind.AssetAllocation();
        }
    }

    record FixedAllocation(@NotNull @NotEmpty Map<String, BigDecimal> weights) implements SleeveKindDto {
        @Override
        public SleeveKind toSleeveKind() {
            return new SleeveKind.FixedAllocation(weights);
        }
    }
}
