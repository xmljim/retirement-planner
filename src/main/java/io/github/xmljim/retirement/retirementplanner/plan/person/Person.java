/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.person;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfileId;

/**
 * One spouse / member of a {@link Plan}'s {@link Household} (ADR-002).
 *
 * <p>{@code id} and {@code salaryProfileId} are absent on a freshly
 * constructed Person that has not yet been persisted; the repository
 * populates them on save. {@code dob} and {@code retirementDate} are
 * required.
 *
 * <p>{@code retirementDate} is per-person — each spouse retires on
 * their own schedule. The accumulation projector (S-2.8) stops
 * generating contribution flows for a person at this date and uses
 * the latest date across the Plan's persons as the projection horizon.
 *
 * <p>Salary timeline modeling lives behind {@link SalaryProfileId} and
 * is a later story; the reference is present so the Person↔SalaryProfile
 * FK is wired from day one (per ADR-002).
 */
public record Person(
        Optional<PersonId> id, Optional<SalaryProfileId> salaryProfileId, LocalDate dob, LocalDate retirementDate) {

    public Person {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(salaryProfileId, "salaryProfileId");
        if (dob == null) {
            throw new IllegalArgumentException("dob is required");
        }
        if (retirementDate == null) {
            throw new IllegalArgumentException("retirementDate is required");
        }
        if (!retirementDate.isAfter(dob)) {
            throw new IllegalArgumentException("retirementDate must be after dob");
        }
    }

    /** Convenience constructor for an unpersisted Person. */
    public static Person of(LocalDate dob, LocalDate retirementDate) {
        return new Person(Optional.empty(), Optional.empty(), dob, retirementDate);
    }
}
