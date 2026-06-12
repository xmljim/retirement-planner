/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared;

import java.security.Principal;

/**
 * Authenticated principal carrying both username and resolved tenant.
 *
 * <p>Stored as the {@code principal} of the Spring Security
 * {@code Authentication}; consumed by {@link TenantContext} to scope
 * repository queries. In stub mode the auth filter installs a fixed
 * instance per request; in passkey mode (EPIC-8) the WebAuthn filter
 * builds one from the authenticated credential.
 */
public record AuthenticatedUser(String username, long tenantId) implements Principal {

    public AuthenticatedUser {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }

    @Override
    public String getName() {
        return username;
    }
}
