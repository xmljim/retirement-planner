/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import io.github.xmljim.retirement.retirementplanner.shared.AuthenticatedUser;
import io.github.xmljim.retirement.retirementplanner.shared.TenantContext;

class SecurityContextTenantContextTest {

    private final TenantContext context = new SecurityContextTenantContext();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("returns tenant from AuthenticatedUser principal on the SecurityContext")
    void returnsTenantFromAuthenticatedUser() {
        var user = new AuthenticatedUser("owner", 42L);
        var auth = new UsernamePasswordAuthenticationToken(user, "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(context.currentTenantId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("throws when no authentication is present")
    void throwsWhenNoAuthentication() {
        assertThatThrownBy(context::currentTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No AuthenticatedUser");
    }

    @Test
    @DisplayName("throws when principal is not an AuthenticatedUser")
    void throwsWhenPrincipalWrongType() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("anonymous", ""));

        assertThatThrownBy(context::currentTenantId).isInstanceOf(IllegalStateException.class);
    }
}
