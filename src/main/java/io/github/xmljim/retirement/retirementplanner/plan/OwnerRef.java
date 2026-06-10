/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import java.util.Objects;

/**
 * Identifies who owns an {@link Account} — a single {@link Person} or
 * a joint household (ADR-002).
 *
 * <p>Tax treatment differs for joint accounts in some states (community
 * property, RMD aggregation, beneficiary rules); the explicit
 * {@code Joint} variant keeps that distinction visible without a
 * sentinel-{@code null} pattern.
 */
public sealed interface OwnerRef {

    /** Account owned solely by the referenced {@code Person}. */
    record Individual(PersonId personId) implements OwnerRef {
        public Individual {
            Objects.requireNonNull(personId, "personId");
        }
    }

    /** Joint household account (community property / spousal). */
    record Joint() implements OwnerRef {}
}
