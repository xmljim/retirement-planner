/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.xmljim.retirement.retirementplanner.shared.TenantContext;

class SoloTenantContextTest {

    @Test
    @DisplayName("solo context resolves to the seeded SOLO tenant id")
    void resolvesSoloTenantId() {
        TenantContext context = new SoloTenantContext();
        assertThat(context.currentTenantId()).isEqualTo(TenantContext.SOLO_TENANT_ID);
    }
}
