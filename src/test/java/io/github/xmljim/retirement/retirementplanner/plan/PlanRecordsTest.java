/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.retirementplanner.plan.household.FilingStatus;
import io.github.xmljim.retirement.retirementplanner.plan.household.Household;
import io.github.xmljim.retirement.retirementplanner.plan.household.HouseholdId;
import io.github.xmljim.retirement.retirementplanner.plan.person.Person;
import io.github.xmljim.retirement.retirementplanner.plan.person.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.salary.SalaryProfileId;

class PlanRecordsTest {

    @Test
    @DisplayName("Plan rejects null household")
    void planRejectsNullHousehold() {
        List<Person> persons = List.of(Person.of(LocalDate.of(1975, 6, 15)));
        assertThatThrownBy(() -> Plan.of(1L, null, persons)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Plan rejects null person list")
    void planRejectsNullPersons() {
        Household household = Household.of(FilingStatus.SINGLE, "VA");
        assertThatThrownBy(() -> Plan.of(1L, household, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Plan rejects empty person list")
    void planRejectsEmptyPersons() {
        Household household = Household.of(FilingStatus.SINGLE, "VA");
        List<Person> persons = List.of();
        assertThatThrownBy(() -> Plan.of(1L, household, persons)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Plan rejects more than two persons")
    void planRejectsThreePersons() {
        Household household = Household.of(FilingStatus.SINGLE, "VA");
        List<Person> persons = List.of(
                Person.of(LocalDate.of(1970, 1, 1)),
                Person.of(LocalDate.of(1972, 1, 1)),
                Person.of(LocalDate.of(1974, 1, 1)));
        assertThatThrownBy(() -> Plan.of(1L, household, persons)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Plan defensively copies the persons list")
    void planCopiesPersons() {
        Household household = Household.of(FilingStatus.SINGLE, "VA");
        Plan plan = Plan.of(1L, household, List.of(Person.of(LocalDate.of(1975, 1, 1))));
        assertThatThrownBy(() -> plan.persons().add(Person.of(LocalDate.of(2000, 1, 1))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Household rejects null filing status")
    void householdRejectsNullFilingStatus() {
        assertThatThrownBy(() -> Household.of(null, "VA")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Household rejects null state")
    void householdRejectsNullState() {
        assertThatThrownBy(() -> Household.of(FilingStatus.SINGLE, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Household rejects blank state")
    void householdRejectsBlankState() {
        assertThatThrownBy(() -> Household.of(FilingStatus.SINGLE, "  ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Person rejects null DOB")
    void personRejectsNullDob() {
        assertThatThrownBy(() -> Person.of(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ID records carry their value")
    void idsCarryValue() {
        assertThat(new PlanId(7L).value()).isEqualTo(7L);
        assertThat(new HouseholdId(8L).value()).isEqualTo(8L);
        assertThat(new PersonId(9L).value()).isEqualTo(9L);
        assertThat(new SalaryProfileId(10L).value()).isEqualTo(10L);
    }
}
