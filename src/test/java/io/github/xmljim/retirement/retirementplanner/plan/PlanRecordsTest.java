/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
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

    private static final LocalDate RETIREMENT_2040 = LocalDate.of(2040, 1, 1);
    private static final BigDecimal RETURN_RATE = new BigDecimal("0.07");
    private static final BigDecimal CASH_RATE = new BigDecimal("0.04");
    private static final Assumptions DEFAULT_ASSUMPTIONS = new Assumptions(RETURN_RATE, CASH_RATE);

    @Test
    @DisplayName("Plan rejects null household")
    void planRejectsNullHousehold() {
        List<Person> persons = List.of(Person.of(LocalDate.of(1975, 6, 15), RETIREMENT_2040));
        assertThatThrownBy(() -> Plan.of(1L, null, persons, DEFAULT_ASSUMPTIONS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Plan rejects null person list")
    void planRejectsNullPersons() {
        Household household = Household.of(FilingStatus.SINGLE, "VA");
        assertThatThrownBy(() -> Plan.of(1L, household, null, DEFAULT_ASSUMPTIONS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Plan rejects empty person list")
    void planRejectsEmptyPersons() {
        Household household = Household.of(FilingStatus.SINGLE, "VA");
        List<Person> persons = List.of();
        assertThatThrownBy(() -> Plan.of(1L, household, persons, DEFAULT_ASSUMPTIONS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Plan rejects more than two persons")
    void planRejectsThreePersons() {
        Household household = Household.of(FilingStatus.SINGLE, "VA");
        List<Person> persons = List.of(
                Person.of(LocalDate.of(1970, 1, 1), RETIREMENT_2040),
                Person.of(LocalDate.of(1972, 1, 1), RETIREMENT_2040),
                Person.of(LocalDate.of(1974, 1, 1), RETIREMENT_2040));
        assertThatThrownBy(() -> Plan.of(1L, household, persons, DEFAULT_ASSUMPTIONS))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Plan defensively copies the persons list")
    void planCopiesPersons() {
        Household household = Household.of(FilingStatus.SINGLE, "VA");
        Plan plan = Plan.of(
                1L, household, List.of(Person.of(LocalDate.of(1975, 1, 1), RETIREMENT_2040)), DEFAULT_ASSUMPTIONS);
        assertThatThrownBy(() -> plan.persons().add(Person.of(LocalDate.of(2000, 1, 1), RETIREMENT_2040)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Plan rejects null assumptions")
    void planRejectsNullAssumptions() {
        Household household = Household.of(FilingStatus.SINGLE, "VA");
        List<Person> persons = List.of(Person.of(LocalDate.of(1975, 6, 15), RETIREMENT_2040));
        assertThatThrownBy(() -> Plan.of(1L, household, persons, null)).isInstanceOf(NullPointerException.class);
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
        assertThatThrownBy(() -> Person.of(null, RETIREMENT_2040)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Person rejects null retirement date")
    void personRejectsNullRetirementDate() {
        assertThatThrownBy(() -> Person.of(LocalDate.of(1975, 6, 15), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Person rejects retirement date not after dob")
    void personRejectsRetirementBeforeDob() {
        LocalDate dob = LocalDate.of(1975, 6, 15);
        assertThatThrownBy(() -> Person.of(dob, dob)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Person.of(dob, dob.minusDays(1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("ID records carry their value")
    void idsCarryValue() {
        assertThat(new PlanId(7L).value()).isEqualTo(7L);
        assertThat(new HouseholdId(8L).value()).isEqualTo(8L);
        assertThat(new PersonId(9L).value()).isEqualTo(9L);
        assertThat(new SalaryProfileId(10L).value()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Assumptions carries the supplied rates")
    void assumptionsCarriesRates() {
        BigDecimal alternateCashRate = new BigDecimal("0.045");
        Assumptions a = new Assumptions(RETURN_RATE, alternateCashRate);
        assertThat(a.preRetirementReturnRate()).isEqualByComparingTo(RETURN_RATE);
        assertThat(a.cashInterestRate()).isEqualByComparingTo(alternateCashRate);
    }

    @Test
    @DisplayName("Assumptions rejects null preRetirementReturnRate")
    void assumptionsRejectsNullReturnRate() {
        assertThatThrownBy(() -> new Assumptions(null, CASH_RATE)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Assumptions rejects null cashInterestRate")
    void assumptionsRejectsNullCashInterestRate() {
        assertThatThrownBy(() -> new Assumptions(RETURN_RATE, null)).isInstanceOf(NullPointerException.class);
    }
}
