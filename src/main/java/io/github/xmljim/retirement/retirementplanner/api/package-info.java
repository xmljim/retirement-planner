/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */

/**
 * REST API surface: controllers and request/response DTOs.
 *
 * <p>Per ADR-001 / CLAUDE.md, controllers delegate to services and
 * never compute. ArchUnit enforces this and forbids controllers from
 * importing internals, repositories, or entities.
 *
 * <p>The Money type serializes as
 * {@code {"amount":"...","currency":"USD"}} — amount is a string to
 * preserve precision in JS clients (per ADR-007).
 *
 * <p>Public API: REST endpoints documented via OpenAPI; DTOs in
 * {@code api/dto/}.
 *
 * <p>See ADR-001 (platform), ADR-007 (Money serialization),
 * ADR-008 (module boundaries).
 */
@ApplicationModule(displayName = "REST API")
package io.github.xmljim.retirement.retirementplanner.api;

import org.springframework.modulith.ApplicationModule;
