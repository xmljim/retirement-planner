/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

/**
 * CORS configuration bound from {@code app.cors.*}.
 *
 * <p>An explicit allowlist is the project's defense-in-depth against
 * browser-mediated token replay (e.g. a leaked bearer token used from
 * an attacker-controlled origin). The same allowlist drives the
 * {@code Origin}/{@code Referer} check on state-changing methods.
 *
 * <p>{@code local} ships {@code http://localhost:5173} for the React
 * dev server; {@code prod} omits a default so an unconfigured deploy
 * fails closed at startup rather than serving requests from any
 * origin. {@code allowedOrigins} accepts comma-separated env vars via
 * Spring's relaxed binding (e.g.
 * {@code APP_CORS_ALLOWED_ORIGINS=https://a.example,https://b.example}).
 */
@ConfigurationProperties("app.cors")
@Validated
public record CorsProperties(
        @NotNull List<String> allowedOrigins,
        @NotNull List<String> allowedMethods,
        @NotNull List<String> allowedHeaders,
        boolean allowCredentials,
        @NotNull Duration maxAge) {}
