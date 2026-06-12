/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.api.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.github.xmljim.retirement.retirementplanner.plan.account.OwnerRef;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Tagged union mirroring the sealed {@link OwnerRef} domain type. The
 * {@code type} discriminator selects the variant; payload fields appear
 * inline.
 *
 * <p>Wire shape:
 * <pre>{@code
 *   {"type":"INDIVIDUAL","personId":42}
 *   {"type":"JOINT"}
 * }</pre>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OwnerRefDto.Individual.class, name = "INDIVIDUAL"),
    @JsonSubTypes.Type(value = OwnerRefDto.Joint.class, name = "JOINT")
})
@Schema(
        description = "Account owner. Either INDIVIDUAL with a personId, or JOINT.",
        oneOf = {OwnerRefDto.Individual.class, OwnerRefDto.Joint.class},
        discriminatorProperty = "type")
public sealed interface OwnerRefDto {

    /** Build a DTO from the domain {@link OwnerRef}. */
    static OwnerRefDto from(OwnerRef owner) {
        return switch (owner) {
            case OwnerRef.Individual ind -> new Individual(ind.personId().value());
            case OwnerRef.Joint _ -> new Joint();
        };
    }

    /** Build a domain {@link OwnerRef} from this DTO. */
    OwnerRef toOwnerRef();

    /** Owned by a single Person identified by {@code personId}. */
    record Individual(@NotNull @Positive Long personId) implements OwnerRefDto {
        @Override
        public OwnerRef toOwnerRef() {
            return new OwnerRef.Individual(new PersonId(personId));
        }
    }

    /** Joint household account; no payload. */
    record Joint() implements OwnerRefDto {
        @Override
        public OwnerRef toOwnerRef() {
            return new OwnerRef.Joint();
        }
    }
}
