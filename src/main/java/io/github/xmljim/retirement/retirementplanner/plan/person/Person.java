/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.person;

import java.time.LocalDate;
import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfileId;

/**
 * One spouse / member of a {@link Plan}'s {@link Household} (ADR-002).
 *
 * <p>{@code id} and {@code salaryProfileId} are absent on a freshly
 * constructed Person that has not yet been persisted; the repository
 * populates them on save. {@code dob} is required.
 *
 * <p>Salary timeline modeling lives behind {@link SalaryProfileId} and
 * is a later story; the reference is present so the Person↔SalaryProfile
 * FK is wired from day one (per ADR-002).
 */
public record Person(Optional<PersonId> id, Optional<SalaryProfileId> salaryProfileId, LocalDate dob) {

    public Person {
        if (dob == null) {
            throw new IllegalArgumentException("dob is required");
        }
    }

    /** Convenience constructor for an unpersisted Person. */
    public static Person of(LocalDate dob) {
        return new Person(Optional.empty(), Optional.empty(), dob);
    }
}
