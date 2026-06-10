/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared.internal;

import org.springframework.stereotype.Component;

import io.github.xmljim.retirement.retirementplanner.shared.TenantContext;

/**
 * Solo-mode {@link TenantContext}: every call returns the {@code "solo"}
 * tenant seeded by {@code V1__init.sql}. Replaced by a request-scoped
 * implementation when the auth stub lands (S-1.8).
 */
@Component
final class SoloTenantContext implements TenantContext {

    @Override
    public long currentTenantId() {
        return SOLO_TENANT_ID;
    }
}
