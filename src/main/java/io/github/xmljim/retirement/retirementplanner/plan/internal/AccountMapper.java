/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.xmljim.retirement.retirementplanner.plan.Account;
import io.github.xmljim.retirement.retirementplanner.plan.AccountId;
import io.github.xmljim.retirement.retirementplanner.plan.AccountSleeve;
import io.github.xmljim.retirement.retirementplanner.plan.OwnerRef;
import io.github.xmljim.retirement.retirementplanner.plan.PersonId;
import io.github.xmljim.retirement.retirementplanner.plan.PlanId;
import io.github.xmljim.retirement.retirementplanner.plan.SleeveId;
import io.github.xmljim.retirement.retirementplanner.plan.SleeveKind;
import io.github.xmljim.retirement.retirementplanner.plan.SleeveYieldPolicy;
import io.github.xmljim.retirement.retirementplanner.shared.MoneyEmbeddable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Translates between the public {@link Account} record and the JPA
 * {@link AccountEntity}. Sealed sub-types ({@link SleeveKind},
 * {@link SleeveYieldPolicy}) round-trip through a TEXT discriminator
 * + JSONB payload. Contribution-policy persistence delegates to
 * {@link ContributionPolicyMapper} to keep coupling within PMD limits.
 */
@Component
class AccountMapper {

    private static final TypeReference<Map<String, BigDecimal>> WEIGHTS_TYPE = new TypeReference<>() {};

    private static final ObjectMapper JSON = new ObjectMapper();

    private final PlanJpaRepository planJpa;
    private final ContributionPolicyMapper contributionPolicyMapper;

    @PersistenceContext
    private EntityManager entityManager;

    AccountMapper(PlanJpaRepository planJpa, ContributionPolicyMapper contributionPolicyMapper) {
        this.planJpa = planJpa;
        this.contributionPolicyMapper = contributionPolicyMapper;
    }

    Account toRecord(AccountEntity entity) {
        OwnerRef owner = entity.getOwnerType() == AccountEntity.OwnerType.JOINT
                ? new OwnerRef.Joint()
                : new OwnerRef.Individual(new PersonId(entity.getOwnerPerson().getId()));
        List<AccountSleeve> sleeves =
                entity.getSleeves().stream().map(this::toSleeveRecord).toList();
        return new Account(
                Optional.of(new AccountId(entity.getId())),
                new PlanId(entity.getPlan().getId()),
                entity.getAccountType(),
                owner,
                sleeves,
                contributionPolicyMapper.toRecord(entity));
    }

    AccountEntity toEntity(Account account) {
        AccountEntity entity = new AccountEntity();
        applyAccountScalars(entity, account);
        account.sleeves().forEach(sleeve -> entity.addSleeve(toSleeveEntity(sleeve)));
        return entity;
    }

    void applyAccountScalars(AccountEntity entity, Account account) {
        entity.setPlan(planJpa.getReferenceById(account.planId().value()));
        entity.setAccountType(account.type());
        switch (account.owner()) {
            case OwnerRef.Individual ind -> {
                entity.setOwnerType(AccountEntity.OwnerType.INDIVIDUAL);
                entity.setOwnerPerson(entityManager.getReference(
                        PersonEntity.class, ind.personId().value()));
            }
            case OwnerRef.Joint _ -> {
                entity.setOwnerType(AccountEntity.OwnerType.JOINT);
                entity.setOwnerPerson(null);
            }
        }
        contributionPolicyMapper.apply(entity, account.contributionPolicy());
    }

    private AccountSleeve toSleeveRecord(AccountSleeveEntity entity) {
        SleeveKind kind =
                switch (entity.getKindType()) {
                    case CASH -> new SleeveKind.Cash();
                    case ASSET_ALLOCATION -> new SleeveKind.AssetAllocation();
                    case FIXED_ALLOCATION ->
                        new SleeveKind.FixedAllocation(readJson(entity.getKindData(), WEIGHTS_TYPE));
                };
        SleeveYieldPolicy yieldPolicy =
                switch (entity.getYieldType()) {
                    case FIXED_RATE ->
                        new SleeveYieldPolicy.FixedRate(readJson(entity.getYieldData(), BigDecimal.class));
                    case MONEY_MARKET -> new SleeveYieldPolicy.MoneyMarket();
                    case TRACKS_ALLOCATION -> new SleeveYieldPolicy.TracksAllocation();
                };
        return new AccountSleeve(
                Optional.of(new SleeveId(entity.getId())),
                kind,
                entity.getBalance().toMoney(),
                yieldPolicy);
    }

    AccountSleeveEntity toSleeveEntity(AccountSleeve sleeve) {
        AccountSleeveEntity entity = new AccountSleeveEntity();
        switch (sleeve.kind()) {
            case SleeveKind.Cash _ -> {
                entity.setKindType(AccountSleeveEntity.KindType.CASH);
                entity.setKindData(null);
            }
            case SleeveKind.AssetAllocation _ -> {
                entity.setKindType(AccountSleeveEntity.KindType.ASSET_ALLOCATION);
                entity.setKindData(null);
            }
            case SleeveKind.FixedAllocation fa -> {
                entity.setKindType(AccountSleeveEntity.KindType.FIXED_ALLOCATION);
                entity.setKindData(writeJson(fa.weights()));
            }
        }
        switch (sleeve.yieldPolicy()) {
            case SleeveYieldPolicy.FixedRate fr -> {
                entity.setYieldType(AccountSleeveEntity.YieldType.FIXED_RATE);
                entity.setYieldData(writeJson(fr.annualRate()));
            }
            case SleeveYieldPolicy.MoneyMarket _ -> {
                entity.setYieldType(AccountSleeveEntity.YieldType.MONEY_MARKET);
                entity.setYieldData(null);
            }
            case SleeveYieldPolicy.TracksAllocation _ -> {
                entity.setYieldType(AccountSleeveEntity.YieldType.TRACKS_ALLOCATION);
                entity.setYieldData(null);
            }
        }
        entity.setBalance(MoneyEmbeddable.from(sleeve.balance()));
        return entity;
    }

    private static String writeJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to serialize sleeve payload", new IOException(e));
        }
    }

    private static <T> T readJson(String value, TypeReference<T> type) {
        try {
            return JSON.readValue(value, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to deserialize sleeve payload", e);
        }
    }

    private static <T> T readJson(String value, Class<T> type) {
        try {
            return JSON.readValue(value, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to deserialize sleeve payload", e);
        }
    }
}
