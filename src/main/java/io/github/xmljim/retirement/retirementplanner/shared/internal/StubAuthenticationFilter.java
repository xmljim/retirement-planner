/*
 * Copyright (c) 2026 Jim Earley. All rights reserved.
 * Licensed under PolyForm Noncommercial 1.0.0 plus the project's
 * AI-training restriction. See LICENSE and LICENSE-ADDENDUM.md.
 */
package io.github.xmljim.retirement.retirementplanner.shared.internal;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.xmljim.retirement.retirementplanner.shared.AuthenticatedUser;
import io.github.xmljim.retirement.retirementplanner.shared.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Stub-mode authentication: installs a fixed {@link AuthenticatedUser}
 * for the seeded {@code solo} tenant on every request. The principal's
 * username is configurable via {@code app.auth.solo-principal}.
 *
 * <p>Replaced by a real WebAuthn filter when EPIC-8 wires passkey
 * authentication. Until then, anything past the filter chain sees a
 * fully-formed {@link org.springframework.security.core.Authentication}.
 */
final class StubAuthenticationFilter extends OncePerRequestFilter {

    private static final List<GrantedAuthority> SOLO_AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final AuthenticatedUser soloUser;

    StubAuthenticationFilter(String soloPrincipalUsername) {
        super();
        this.soloUser = new AuthenticatedUser(soloPrincipalUsername, TenantContext.SOLO_TENANT_ID);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var token = new StubAuthenticationToken(soloUser, SOLO_AUTHORITIES);
        token.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(token);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static final class StubAuthenticationToken extends AbstractAuthenticationToken {
        private static final long serialVersionUID = 1L;
        private final transient AuthenticatedUser principal;

        StubAuthenticationToken(AuthenticatedUser principal, List<GrantedAuthority> authorities) {
            super(authorities);
            this.principal = principal;
        }

        @Override
        public Object getCredentials() {
            return "";
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }
    }
}
