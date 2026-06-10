/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

/**
 * Resolves the active tenant for the current request scope.
 *
 * <p>Per ADR-002, every aggregate root carries a tenant identifier;
 * repositories filter by it automatically. In solo mode the
 * {@code SoloTenantContext} bean returns the seeded {@code "solo"}
 * tenant; in SaaS mode (later) a request-scoped implementation will
 * read the authenticated principal.
 */
public interface TenantContext {

    /** Database identifier of the {@code solo} tenant seeded in V1. */
    long SOLO_TENANT_ID = 1L;

    /** @return the tenant id active on the current call */
    long currentTenantId();
}
