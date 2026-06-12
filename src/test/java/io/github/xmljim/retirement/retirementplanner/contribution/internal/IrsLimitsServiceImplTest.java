/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.contribution.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.core.io.ClassPathResource;

import io.github.xmljim.retirement.retirementplanner.contribution.IrsLimits;
import io.github.xmljim.retirement.retirementplanner.shared.Money;

@TestInstance(Lifecycle.PER_CLASS)
class IrsLimitsServiceImplTest {

    private IrsLimitsServiceImpl service;

    @BeforeAll
    void setUp() {
        service = new IrsLimitsServiceImpl(new ClassPathResource("data/irs-limits.yaml"));
    }

    @Test
    @DisplayName("forYear returns published values for 2024")
    void forYearPublished2024() {
        IrsLimits limits = service.forYear(2024);
        assertThat(limits.source()).isEqualTo(IrsLimits.Source.PUBLISHED);
        assertThat(limits.year()).isEqualTo(2024);
        assertThat(limits.employee401kBase()).isEqualTo(Money.usd("23000"));
        assertThat(limits.totalDc()).isEqualTo(Money.usd("69000"));
    }

    @Test
    @DisplayName("forYear returns SECURE 2.0 §109 super-catch-up rising in 2025")
    void forYearPublished2025SuperCatchup() {
        IrsLimits limits = service.forYear(2025);
        assertThat(limits.employee401k60PlusCatchup()).isEqualTo(Money.usd("11250"));
    }

    @Test
    @DisplayName("forYear returns published values for 2026")
    void forYearPublished2026() {
        IrsLimits limits = service.forYear(2026);
        assertThat(limits.source()).isEqualTo(IrsLimits.Source.PUBLISHED);
        assertThat(limits.employee401kBase()).isEqualTo(Money.usd("24500"));
        assertThat(limits.secure2_0_603HighEarnerThreshold()).isEqualTo(Money.usd("150000"));
    }

    @Test
    @DisplayName("forYear projects forward by the configured growth rate")
    void forYearProjected2027() {
        IrsLimits limits = service.forYear(2027);
        assertThat(limits.source()).isEqualTo(IrsLimits.Source.PROJECTED);
        assertThat(limits.year()).isEqualTo(2027);
        // 24500 * 1.025 = 25112.50
        assertThat(limits.employee401kBase()).isEqualTo(Money.usd("25112.50"));
    }

    @Test
    @DisplayName("forYear compounds the growth rate over multi-year gaps")
    void forYearProjectedMultiYear() {
        IrsLimits limits = service.forYear(2030);
        assertThat(limits.source()).isEqualTo(IrsLimits.Source.PROJECTED);
        BigDecimal multiplier = new BigDecimal("1.025").pow(4);
        BigDecimal expected = new BigDecimal("24500").multiply(multiplier);
        assertThat(limits.employee401kBase()).isEqualTo(Money.of(expected, Currency.getInstance("USD")));
    }

    @Test
    @DisplayName("forYear caches projected results")
    void forYearProjectionIsStable() {
        IrsLimits first = service.forYear(2030);
        IrsLimits second = service.forYear(2030);
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("forYear rejects years before the earliest published year")
    void forYearRejectsPreEarliest() {
        assertThatThrownBy(() -> service.forYear(2023)).isInstanceOf(IllegalArgumentException.class);
    }
}
