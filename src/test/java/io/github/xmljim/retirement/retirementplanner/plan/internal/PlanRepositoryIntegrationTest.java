/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.xmljim.retirement.retirementplanner.plan.FilingStatus;
import io.github.xmljim.retirement.retirementplanner.plan.Household;
import io.github.xmljim.retirement.retirementplanner.plan.Person;
import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.shared.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Persistence happy-path coverage for {@link PlanRepositoryImpl}. Runs
 * against a real Postgres via Testcontainers; Flyway applies V1 + V2
 * to the freshly-spun container so we exercise the actual migration.
 *
 * <p>{@link TenantContext} is mocked so individual cases can flip the
 * active tenant without managing security context.
 */
@Testcontainers
@SpringBootTest
class PlanRepositoryIntegrationTest {

    private static final long SOLO_TENANT = TenantContext.SOLO_TENANT_ID;
    private static final String OTHER_TENANT_SLUG = "other";

    @Container
    @ServiceConnection
    @SuppressWarnings("PMD.MutableStaticState") // Testcontainers requires @Container fields to be static
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("retirement_planner")
            .withUsername("retirement")
            .withPassword("retirement");

    @Autowired
    private PlanRepositoryImpl repository;

    @Autowired
    private PlanJpaRepository jpa;

    @MockitoBean
    private TenantContext tenantContext;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanState() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createQuery("DELETE FROM PersonEntity").executeUpdate();
            entityManager.createQuery("DELETE FROM HouseholdEntity").executeUpdate();
            entityManager.createQuery("DELETE FROM PlanEntity").executeUpdate();
            entityManager.createQuery("DELETE FROM SalaryProfileEntity").executeUpdate();
            entityManager
                    .createNativeQuery("INSERT INTO tenants (slug, display_name) "
                            + "SELECT :slug, 'Other' WHERE NOT EXISTS "
                            + "(SELECT 1 FROM tenants WHERE slug = :slug)")
                    .setParameter("slug", OTHER_TENANT_SLUG)
                    .executeUpdate();
        });
    }

    @Test
    @DisplayName("save persists Plan with Household and 1 Person, reload returns full graph")
    void savePersistsPlanWithHouseholdAndPerson() {
        whenTenantIs(SOLO_TENANT);

        Plan saved = repository.save(Plan.of(
                SOLO_TENANT, Household.of(FilingStatus.SINGLE, "VA"), List.of(Person.of(LocalDate.of(1975, 6, 15)))));

        assertThat(saved.id()).isPresent();

        Optional<Plan> reloaded = repository.findById(saved.id().orElseThrow());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.orElseThrow().tenantId()).isEqualTo(SOLO_TENANT);
        assertThat(reloaded.orElseThrow().household().filingStatus()).isEqualTo(FilingStatus.SINGLE);
        assertThat(reloaded.orElseThrow().household().state()).isEqualTo("VA");
        assertThat(reloaded.orElseThrow().persons()).hasSize(1);
        assertThat(reloaded.orElseThrow().persons().get(0).dob()).isEqualTo(LocalDate.of(1975, 6, 15));
        assertThat(reloaded.orElseThrow().persons().get(0).salaryProfileId()).isPresent();
    }

    @Test
    @DisplayName("save persists Plan with two Persons (married couple)")
    void savePersistsTwoPersons() {
        whenTenantIs(SOLO_TENANT);

        Plan saved = repository.save(Plan.of(
                SOLO_TENANT,
                Household.of(FilingStatus.MARRIED_FILING_JOINTLY, "CA"),
                List.of(Person.of(LocalDate.of(1970, 1, 1)), Person.of(LocalDate.of(1972, 5, 20)))));

        Plan reloaded = repository.findById(saved.id().orElseThrow()).orElseThrow();
        assertThat(reloaded.persons()).hasSize(2);
        assertThat(reloaded.persons())
                .extracting(Person::dob)
                .containsExactlyInAnyOrder(LocalDate.of(1970, 1, 1), LocalDate.of(1972, 5, 20));
    }

    @Test
    @DisplayName("findById is scoped to active tenant — other tenants invisible")
    void findByIdIsTenantScoped() {
        long otherTenant = lookupTenantIdBySlug(OTHER_TENANT_SLUG);
        whenTenantIs(otherTenant);

        Plan inOther = repository.save(Plan.of(
                otherTenant, Household.of(FilingStatus.SINGLE, "TX"), List.of(Person.of(LocalDate.of(1980, 3, 3)))));

        whenTenantIs(SOLO_TENANT);
        assertThat(repository.findById(inOther.id().orElseThrow())).isEmpty();
    }

    @Test
    @DisplayName("findAll is scoped to active tenant")
    void findAllIsTenantScoped() {
        whenTenantIs(SOLO_TENANT);
        repository.save(Plan.of(
                SOLO_TENANT, Household.of(FilingStatus.SINGLE, "NY"), List.of(Person.of(LocalDate.of(1980, 1, 1)))));

        long otherTenant = lookupTenantIdBySlug(OTHER_TENANT_SLUG);
        whenTenantIs(otherTenant);
        repository.save(Plan.of(
                otherTenant, Household.of(FilingStatus.SINGLE, "FL"), List.of(Person.of(LocalDate.of(1981, 2, 2)))));

        whenTenantIs(SOLO_TENANT);
        List<Plan> visible = repository.findAll();
        assertThat(visible).extracting(p -> p.household().state()).containsOnly("NY");
    }

    @Test
    @DisplayName("deleteById cascades to Household and Persons")
    void deleteByIdCascades() {
        whenTenantIs(SOLO_TENANT);
        Plan saved = repository.save(Plan.of(
                SOLO_TENANT, Household.of(FilingStatus.SINGLE, "VA"), List.of(Person.of(LocalDate.of(1975, 6, 15)))));
        PlanId id = saved.id().orElseThrow();

        repository.deleteById(id);

        assertThat(repository.findById(id)).isEmpty();
        assertThat(jpa.findById(id.value())).isEmpty();
    }

    @Test
    @DisplayName("deleteById in wrong tenant is a no-op")
    void deleteByIdRefusesCrossTenant() {
        long otherTenant = lookupTenantIdBySlug(OTHER_TENANT_SLUG);
        whenTenantIs(otherTenant);
        Plan inOther = repository.save(Plan.of(
                otherTenant, Household.of(FilingStatus.SINGLE, "TX"), List.of(Person.of(LocalDate.of(1980, 3, 3)))));

        whenTenantIs(SOLO_TENANT);
        repository.deleteById(inOther.id().orElseThrow());

        whenTenantIs(otherTenant);
        assertThat(repository.findById(inOther.id().orElseThrow())).isPresent();
    }

    @Test
    @DisplayName("save refuses to create a Plan whose tenantId differs from the active tenant")
    void saveRefusesTenantMismatch() {
        long otherTenant = lookupTenantIdBySlug(OTHER_TENANT_SLUG);
        whenTenantIs(SOLO_TENANT);
        Plan mismatched = Plan.of(
                otherTenant, Household.of(FilingStatus.SINGLE, "VA"), List.of(Person.of(LocalDate.of(1975, 6, 15))));

        assertThatThrownBy(() -> repository.save(mismatched))
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant mismatch");
    }

    private void whenTenantIs(long tenantId) {
        when(tenantContext.currentTenantId()).thenReturn(tenantId);
    }

    private long lookupTenantIdBySlug(String slug) {
        return transactionTemplate.execute(status -> {
            Number id = (Number) entityManager
                    .createNativeQuery("SELECT id FROM tenants WHERE slug = :slug")
                    .setParameter("slug", slug)
                    .getSingleResult();
            return id.longValue();
        });
    }
}
