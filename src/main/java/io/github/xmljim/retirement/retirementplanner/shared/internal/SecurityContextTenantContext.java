/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared.internal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import io.github.xmljim.retirement.retirementplanner.shared.AuthenticatedUser;
import io.github.xmljim.retirement.retirementplanner.shared.TenantContext;

/**
 * Resolves the tenant from the Spring Security {@code Authentication}
 * stored on the current thread. The auth filter (stub or, later,
 * passkey) is responsible for installing an {@link AuthenticatedUser}
 * principal before any controller runs; this context just reads it.
 *
 * <p>Throws {@link IllegalStateException} when no authentication is
 * present — that means a request reached a tenant-scoped operation
 * without going through the security filter chain, which is a bug.
 */
@Component
final class SecurityContextTenantContext implements TenantContext {

    @Override
    public long currentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalStateException("No AuthenticatedUser on the SecurityContext; tenant cannot be resolved. "
                    + "Ensure the request passes through the SecurityFilterChain.");
        }
        return user.tenantId();
    }
}
