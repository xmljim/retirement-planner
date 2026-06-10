/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

/**
 * Strongly-typed identifier for a {@code SalaryProfile} entity (ADR-002).
 *
 * <p>SalaryProfile is paired 1..1 with a Person; its fields are introduced
 * in a later story (salary timeline modeling).
 */
public record SalaryProfileId(long value) {}
