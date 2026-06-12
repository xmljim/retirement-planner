/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.plan.contribution.internal;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.xmljim.retirement.retirementplanner.plan.account.internal.AccountEntity;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.ContributionAmount;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.ContributionPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.EmployerMatch;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.EscalationPolicy;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.FixedDollar;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.MatchTier;
import io.github.xmljim.retirement.retirementplanner.plan.contribution.PercentOfSalary;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

/**
 * Round-trips a {@link ContributionPolicy} between the public domain
 * record and an {@link AccountEntity}'s discriminator + JSONB columns.
 * Carved out of {@link AccountMapper} to keep that mapper's coupling
 * within PMD thresholds (ADR-009 quality gates).
 *
 * <p>Internal Jackson DTOs ({@link MoneyJson}, {@link EscalationJson},
 * {@link MatchJson}) keep Jackson off the public domain records — same
 * pattern as {@code IrsLimitsYaml}. The mapper bridges.
 */
@Component
public class ContributionPolicyMapper {

    private static final ObjectMapper JSON = new ObjectMapper();

    public Optional<ContributionPolicy> toRecord(AccountEntity entity) {
        if (entity.getContributionAmountType() == null) {
            return Optional.empty();
        }
        ContributionAmount employee =
                switch (entity.getContributionAmountType()) {
                    case PERCENT_OF_SALARY ->
                        new PercentOfSalary(readJson(entity.getContributionAmountData(), BigDecimal.class));
                    case FIXED_DOLLAR -> {
                        MoneyJson m = readJson(entity.getContributionAmountData(), MoneyJson.class);
                        yield new FixedDollar(m.toMoney());
                    }
                };
        Optional<EscalationPolicy> escalation = Optional.ofNullable(entity.getEscalationData())
                .map(s -> readJson(s, EscalationJson.class))
                .map(EscalationJson::toEscalation);
        Optional<EmployerMatch> match = Optional.ofNullable(entity.getEmployerMatchData())
                .map(s -> readJson(s, MatchJson.class))
                .map(MatchJson::toEmployerMatch);
        return Optional.of(new ContributionPolicy(
                employee,
                escalation,
                match,
                Optional.ofNullable(entity.getContributionStartDate()),
                Optional.ofNullable(entity.getContributionEndDate())));
    }

    public void apply(AccountEntity entity, Optional<ContributionPolicy> policy) {
        if (policy.isEmpty()) {
            entity.setContributionAmountType(null);
            entity.setContributionAmountData(null);
            entity.setEscalationData(null);
            entity.setEmployerMatchData(null);
            entity.setContributionStartDate(null);
            entity.setContributionEndDate(null);
            return;
        }
        ContributionPolicy cp = policy.get();
        switch (cp.employee()) {
            case PercentOfSalary p -> {
                entity.setContributionAmountType(AccountEntity.ContributionAmountType.PERCENT_OF_SALARY);
                entity.setContributionAmountData(writeJson(p.pct()));
            }
            case FixedDollar f -> {
                entity.setContributionAmountType(AccountEntity.ContributionAmountType.FIXED_DOLLAR);
                entity.setContributionAmountData(writeJson(MoneyJson.from(f.annualAmount())));
            }
        }
        entity.setEscalationData(
                cp.escalation().map(e -> writeJson(EscalationJson.from(e))).orElse(null));
        entity.setEmployerMatchData(
                cp.match().map(m -> writeJson(MatchJson.from(m))).orElse(null));
        entity.setContributionStartDate(cp.startDate().orElse(null));
        entity.setContributionEndDate(cp.endDate().orElse(null));
    }

    private static String writeJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException("Failed to serialize contribution-policy payload", new IOException(e));
        }
    }

    private static <T> T readJson(String value, Class<T> type) {
        try {
            return JSON.readValue(value, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to deserialize contribution-policy payload", e);
        }
    }

    /** Internal JSON shape for {@link Money}; keeps Jackson off the public record. */
    private record MoneyJson(BigDecimal amount, String currency) {

        static MoneyJson from(Money money) {
            return new MoneyJson(money.amount(), money.currency().getCurrencyCode());
        }

        Money toMoney() {
            return Money.of(amount, Currency.getInstance(currency));
        }
    }

    private record EscalationJson(BigDecimal annualIncrease, BigDecimal cap) {

        static EscalationJson from(EscalationPolicy e) {
            return new EscalationJson(e.annualIncrease(), e.cap());
        }

        EscalationPolicy toEscalation() {
            return new EscalationPolicy(annualIncrease, cap);
        }
    }

    private record MatchTierJson(BigDecimal employeeContribPctUpTo, BigDecimal matchPct) {

        static MatchTierJson from(MatchTier t) {
            return new MatchTierJson(t.employeeContribPctUpTo(), t.matchPct());
        }

        MatchTier toMatchTier() {
            return new MatchTier(employeeContribPctUpTo, matchPct);
        }
    }

    private record MatchJson(List<MatchTierJson> tiers) {

        static MatchJson from(EmployerMatch m) {
            return new MatchJson(m.tiers().stream().map(MatchTierJson::from).toList());
        }

        EmployerMatch toEmployerMatch() {
            return new EmployerMatch(
                    tiers.stream().map(MatchTierJson::toMatchTier).toList());
        }
    }
}
