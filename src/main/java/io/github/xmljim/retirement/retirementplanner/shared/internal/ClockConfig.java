/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared.internal;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single source of {@link Clock} for time-dependent services. Tests
 * override with a fixed {@code Clock} via {@code @MockitoBean} to
 * make projection horizons deterministic.
 */
@Configuration
class ClockConfig {

    @Bean
    Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
