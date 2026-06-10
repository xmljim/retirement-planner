/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

import io.github.xmljim.retirement.retirementplanner.plan.Account;
import io.github.xmljim.retirement.retirementplanner.plan.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.AccountSleeve;
import io.github.xmljim.retirement.retirementplanner.plan.AccountType;
import io.github.xmljim.retirement.retirementplanner.plan.FilingStatus;
import io.github.xmljim.retirement.retirementplanner.plan.Household;
import io.github.xmljim.retirement.retirementplanner.plan.OwnerRef;
import io.github.xmljim.retirement.retirementplanner.plan.Person;
import io.github.xmljim.retirement.retirementplanner.plan.Plan;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.SleeveKind;
import io.github.xmljim.retirement.retirementplanner.plan.SleeveYieldPolicy;
import io.github.xmljim.retirement.retirementplanner.shared.Money;
import io.github.xmljim.retirement.retirementplanner.shared.TenantContext;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Persistence happy-path coverage for {@link AccountRepositoryImpl}.
 * Runs against a real Postgres via Testcontainers so we exercise the
 * V3 migration, the JSONB sleeve payloads, and the sealed-kind
 * round-trip end-to-end.
 */
// Integration test legitimately couples to the full Account/Plan/Sleeve/Money surface plus Spring + Testcontainers
// bootstrap.
@SuppressWarnings("PMD.ExcessiveImports")
@Testcontainers
@SpringBootTest
class AccountRepositoryIntegrationTest {

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
    private AccountRepositoryImpl repository;

    @Autowired
    private AccountJpaRepository accountJpa;

    @Autowired
    private PlanRepositoryImpl planRepository;

    @MockitoBean
    private TenantContext tenantContext;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanState() {
        transactionTemplate.executeWithoutResult(status -> {
            entityManager.createQuery("DELETE FROM AccountSleeveEntity").executeUpdate();
            entityManager.createQuery("DELETE FROM AccountEntity").executeUpdate();
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
    @DisplayName("save persists Account with multi-sleeve graph and full round-trip")
    void savePersistsAccountWithMultipleSleeves() {
        whenTenantIs(SOLO_TENANT);
        Plan plan = newPlan(SOLO_TENANT, FilingStatus.SINGLE, "VA");
        PlanId planId = plan.id().orElseThrow();

        AccountSleeve cashSleeve = AccountSleeve.of(
                new SleeveKind.Cash(), Money.usd("12500.00"), new SleeveYieldPolicy.FixedRate(new BigDecimal("0.045")));
        AccountSleeve equitySleeve = AccountSleeve.of(
                new SleeveKind.AssetAllocation(), Money.usd("87500.00"), new SleeveYieldPolicy.TracksAllocation());

        Account saved = repository.save(Account.of(
                planId,
                AccountType.TRADITIONAL_IRA,
                new OwnerRef.Individual(plan.persons().get(0).id().orElseThrow()),
                List.of(cashSleeve, equitySleeve)));

        assertThat(saved.id()).isPresent();
        Account reloaded = repository.findById(saved.id().orElseThrow()).orElseThrow();
        assertThat(reloaded.type()).isEqualTo(AccountType.TRADITIONAL_IRA);
        assertThat(reloaded.owner()).isInstanceOf(OwnerRef.Individual.class);
        assertThat(reloaded.sleeves()).hasSize(2);
        assertThat(reloaded.sleeves())
                .extracting(AccountSleeve::balance)
                .containsExactlyInAnyOrder(Money.usd("12500.00"), Money.usd("87500.00"));
    }

    @Test
    @DisplayName("save persists JOINT-owned Account; owner_person_id null in DB")
    void savePersistsJointAccount() {
        whenTenantIs(SOLO_TENANT);
        Plan plan = newPlan(SOLO_TENANT, FilingStatus.MARRIED_FILING_JOINTLY, "CA");

        Account saved = repository.save(Account.withDefaultSleeve(
                plan.id().orElseThrow(), AccountType.TAXABLE_BROKERAGE, new OwnerRef.Joint(), Money.usd("250000.00")));

        Account reloaded = repository.findById(saved.id().orElseThrow()).orElseThrow();
        assertThat(reloaded.owner()).isInstanceOf(OwnerRef.Joint.class);
    }

    @Test
    @DisplayName("FixedAllocation and FixedRate round-trip through JSONB")
    void sealedKindsRoundTripThroughJsonb() {
        whenTenantIs(SOLO_TENANT);
        Plan plan = newPlan(SOLO_TENANT, FilingStatus.SINGLE, "TX");

        Map<String, BigDecimal> weights = Map.of(
                "EQUITY", new BigDecimal("0.65"), "BOND", new BigDecimal("0.30"), "CASH", new BigDecimal("0.05"));
        AccountSleeve fixed = AccountSleeve.of(
                new SleeveKind.FixedAllocation(weights),
                Money.usd("50000.00"),
                new SleeveYieldPolicy.FixedRate(new BigDecimal("0.0375")));

        Account saved = repository.save(Account.of(
                plan.id().orElseThrow(),
                AccountType.HSA,
                new OwnerRef.Individual(plan.persons().get(0).id().orElseThrow()),
                List.of(fixed)));

        AccountSleeve reloadedSleeve = repository
                .findById(saved.id().orElseThrow())
                .orElseThrow()
                .sleeves()
                .get(0);
        assertThat(reloadedSleeve.kind()).isInstanceOf(SleeveKind.FixedAllocation.class);
        SleeveKind.FixedAllocation fa = (SleeveKind.FixedAllocation) reloadedSleeve.kind();
        assertThat(fa.weights())
                .containsEntry("EQUITY", new BigDecimal("0.65"))
                .containsEntry("BOND", new BigDecimal("0.30"))
                .containsEntry("CASH", new BigDecimal("0.05"));
        assertThat(reloadedSleeve.yieldPolicy()).isInstanceOf(SleeveYieldPolicy.FixedRate.class);
        assertThat(((SleeveYieldPolicy.FixedRate) reloadedSleeve.yieldPolicy()).annualRate())
                .isEqualByComparingTo(new BigDecimal("0.0375"));
    }

    @Test
    @DisplayName("findById is scoped to active tenant — other tenants invisible")
    void findByIdIsTenantScoped() {
        long otherTenant = lookupTenantIdBySlug(OTHER_TENANT_SLUG);
        whenTenantIs(otherTenant);
        Plan otherPlan = newPlan(otherTenant, FilingStatus.SINGLE, "FL");
        Account inOther = repository.save(Account.withDefaultSleeve(
                otherPlan.id().orElseThrow(), AccountType.ROTH_IRA, new OwnerRef.Joint(), Money.usd("1000.00")));

        whenTenantIs(SOLO_TENANT);
        assertThat(repository.findById(inOther.id().orElseThrow())).isEmpty();
    }

    @Test
    @DisplayName("findByPlanId is scoped to active tenant")
    void findByPlanIdIsTenantScoped() {
        whenTenantIs(SOLO_TENANT);
        Plan soloPlan = newPlan(SOLO_TENANT, FilingStatus.SINGLE, "NY");
        repository.save(Account.withDefaultSleeve(
                soloPlan.id().orElseThrow(), AccountType.CASH, new OwnerRef.Joint(), Money.usd("5000.00")));

        long otherTenant = lookupTenantIdBySlug(OTHER_TENANT_SLUG);
        whenTenantIs(otherTenant);
        // In other-tenant context, querying solo's plan id returns nothing.
        assertThat(repository.findByPlanId(soloPlan.id().orElseThrow())).isEmpty();

        whenTenantIs(SOLO_TENANT);
        assertThat(repository.findByPlanId(soloPlan.id().orElseThrow())).hasSize(1);
    }

    @Test
    @DisplayName("deleting an Account cascades to its sleeves")
    void deleteAccountCascadesSleeves() {
        whenTenantIs(SOLO_TENANT);
        Plan plan = newPlan(SOLO_TENANT, FilingStatus.SINGLE, "VA");
        Account saved = repository.save(Account.of(
                plan.id().orElseThrow(),
                AccountType.TRADITIONAL_401K,
                new OwnerRef.Individual(plan.persons().get(0).id().orElseThrow()),
                List.of(
                        AccountSleeve.of(
                                new SleeveKind.Cash(), Money.usd("100.00"), new SleeveYieldPolicy.MoneyMarket()),
                        AccountSleeve.of(
                                new SleeveKind.AssetAllocation(),
                                Money.usd("900.00"),
                                new SleeveYieldPolicy.TracksAllocation()))));
        AccountId id = saved.id().orElseThrow();

        repository.deleteById(id);

        assertThat(accountJpa.findById(id.value())).isEmpty();
        Number remainingSleeves = transactionTemplate.execute(status -> (Number) entityManager
                .createNativeQuery("SELECT count(*) FROM account_sleeve")
                .getSingleResult());
        assertThat(remainingSleeves.longValue()).isZero();
    }

    @Test
    @DisplayName("deleting a Plan cascades through Accounts to Sleeves")
    void deletePlanCascadesAccountsAndSleeves() {
        whenTenantIs(SOLO_TENANT);
        Plan plan = newPlan(SOLO_TENANT, FilingStatus.SINGLE, "VA");
        repository.save(Account.withDefaultSleeve(
                plan.id().orElseThrow(), AccountType.ROTH_IRA, new OwnerRef.Joint(), Money.usd("100.00")));

        planRepository.deleteById(plan.id().orElseThrow());

        Number accountCount = transactionTemplate.execute(status -> (Number)
                entityManager.createNativeQuery("SELECT count(*) FROM account").getSingleResult());
        Number sleeveCount = transactionTemplate.execute(status -> (Number) entityManager
                .createNativeQuery("SELECT count(*) FROM account_sleeve")
                .getSingleResult());
        assertThat(accountCount.longValue()).isZero();
        assertThat(sleeveCount.longValue()).isZero();
    }

    @Test
    @DisplayName("save refuses an Account whose parent Plan belongs to another tenant")
    void saveRefusesCrossTenantParent() {
        long otherTenant = lookupTenantIdBySlug(OTHER_TENANT_SLUG);
        whenTenantIs(otherTenant);
        Plan otherPlan = newPlan(otherTenant, FilingStatus.SINGLE, "FL");

        whenTenantIs(SOLO_TENANT);
        Account mismatched = Account.withDefaultSleeve(
                otherPlan.id().orElseThrow(), AccountType.ROTH_IRA, new OwnerRef.Joint(), Money.usd("1.00"));

        assertThatThrownBy(() -> repository.save(mismatched))
                .rootCause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenant mismatch");
    }

    private Plan newPlan(long tenantId, FilingStatus filing, String state) {
        return planRepository.save(
                Plan.of(tenantId, Household.of(filing, state), List.of(Person.of(LocalDate.of(1975, 6, 15)))));
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
